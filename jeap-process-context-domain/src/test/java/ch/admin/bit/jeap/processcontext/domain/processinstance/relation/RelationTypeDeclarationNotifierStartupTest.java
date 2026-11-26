package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the startup order against the mechanism actually used to start the Kafka message consumers:
 * {@code MessageService} starts them on {@link ApplicationStartedEvent}. The relation listener must have been invoked
 * before, and an exception thrown by it must abort the startup before the event is published.
 */
class RelationTypeDeclarationNotifierStartupTest {

    @Test
    void listenerIsInvokedBeforeApplicationStartedEvent() {
        try (ConfigurableApplicationContext context = run(RecordingListenerConfig.class)) {
            StartupRecorder recorder = context.getBean(StartupRecorder.class);
            assertThat(recorder.listenerInvocations).isEqualTo(1);
            assertThat(recorder.applicationStarted).isTrue();
            assertThat(recorder.applicationStartedWhenListenerInvoked).isFalse();
        }
    }

    @Test
    void listenerThrows_startupFails_beforeApplicationStartedEvent() {
        assertThatThrownBy(() -> run(ThrowingListenerConfig.class).close())
                .isInstanceOf(RecorderCarryingException.class)
                .satisfies(e -> assertThat(((RecorderCarryingException) e).recorder.applicationStarted).isFalse());
    }

    private static ConfigurableApplicationContext run(Class<?> listenerConfig) {
        return new SpringApplicationBuilder(CommonConfig.class, listenerConfig)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run();
    }

    @Configuration(proxyBeanMethods = false)
    @Import(RelationTypeDeclarationNotifier.class)
    static class CommonConfig {
        @Bean
        StartupRecorder startupRecorder() {
            return new StartupRecorder();
        }

        @Bean
        ProcessTemplateRepository processTemplateRepository() {
            ProcessTemplateRepository repository = mock(ProcessTemplateRepository.class);
            when(repository.getAllTemplates()).thenReturn(List.of());
            return repository;
        }

        /**
         * Stands in for MessageService#onAppStarted, which starts the Kafka message consumers
         */
        @Bean
        ConsumerStarter consumerStarter(StartupRecorder recorder) {
            return new ConsumerStarter(recorder);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RecordingListenerConfig {
        @Bean
        RelationListener relationListener(StartupRecorder recorder) {
            return new RelationListener() {
                @Override
                public void relationsAdded(Collection<Relation> relations) {
                    // not relevant for this test
                }

                @Override
                public void relationTypesDeclared(Set<RelationType> relationTypes) {
                    recorder.listenerInvocations++;
                    recorder.applicationStartedWhenListenerInvoked = recorder.applicationStarted;
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ThrowingListenerConfig {
        @Bean
        RelationListener relationListener(StartupRecorder recorder) {
            return new RelationListener() {
                @Override
                public void relationsAdded(Collection<Relation> relations) {
                    // not relevant for this test
                }

                @Override
                public void relationTypesDeclared(Set<RelationType> relationTypes) {
                    throw new RecorderCarryingException(recorder);
                }
            };
        }
    }

    static class StartupRecorder {
        volatile int listenerInvocations;
        volatile boolean applicationStarted;
        volatile boolean applicationStartedWhenListenerInvoked;
    }

    static class ConsumerStarter {
        private final StartupRecorder recorder;

        ConsumerStarter(StartupRecorder recorder) {
            this.recorder = recorder;
        }

        @EventListener
        public void onAppStarted(ApplicationStartedEvent ignored) {
            recorder.applicationStarted = true;
        }
    }

    /**
     * Carries the recorder of the failed (and therefore closed) context to the assertions
     */
    static class RecorderCarryingException extends IllegalStateException {
        private final transient StartupRecorder recorder;

        RecorderCarryingException(StartupRecorder recorder) {
            super("relation types rejected");
            this.recorder = recorder;
        }
    }
}
