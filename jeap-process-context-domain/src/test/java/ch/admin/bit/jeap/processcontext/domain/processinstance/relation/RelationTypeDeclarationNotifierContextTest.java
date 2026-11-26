package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.SmartLifecycle;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the Spring lifecycle guarantees of {@link RelationTypeDeclarationNotifier}: the listener is invoked during
 * the context startup, before lifecycle beans (such as Kafka listener containers) are started, also with lazy
 * initialization enabled, and an exception thrown by the listener aborts the startup.
 * <p>
 * All state is held by a {@link StartupRecorder} bean of the respective context, so the tests are independent of each
 * other (no static state).
 */
class RelationTypeDeclarationNotifierContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ProcessTemplateRepository.class, RelationTypeDeclarationNotifierContextTest::emptyTemplateRepository)
            .withBean(StartupRecorder.class)
            .withBean(LifecycleProbe.class)
            .withBean(RelationTypeDeclarationNotifier.class);

    @Test
    void listenerIsInvokedBeforeLifecycleBeansAreStarted() {
        contextRunner.withBean(RecordingRelationListener.class).run(context -> {
            assertThat(context).hasNotFailed();
            StartupRecorder recorder = context.getBean(StartupRecorder.class);
            assertThat(recorder.listenerInvocations).isEqualTo(1);
            assertThat(recorder.lifecycleStartedWhenListenerInvoked).isFalse();
            assertThat(recorder.lifecycleStarted).isTrue();
        });
    }

    /**
     * Spring Boot's LazyInitializationBeanFactoryPostProcessor (spring.main.lazy-initialization=true) excludes
     * SmartInitializingSingletons from lazy initialization. Guards against losing the startup callback if this changes.
     */
    @Test
    void lazyInitializationEnabled_listenerIsInvokedNevertheless() {
        contextRunner.withBean(RecordingRelationListener.class)
                .withInitializer(context -> context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor()))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(StartupRecorder.class).listenerInvocations).isEqualTo(1);
                });
    }

    @Test
    void listenerThrows_contextFailsToStart_andLifecycleBeansAreNotStarted() {
        contextRunner.withBean(ThrowingRelationListener.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .isInstanceOf(RecorderCarryingException.class)
                    .hasMessage("relation types rejected");
            // The failed context is closed, its recorder is handed over by the exception
            StartupRecorder recorder = ((RecorderCarryingException) context.getStartupFailure()).recorder;
            assertThat(recorder.lifecycleStarted).isFalse();
        });
    }

    private static ProcessTemplateRepository emptyTemplateRepository() {
        ProcessTemplateRepository repository = mock(ProcessTemplateRepository.class);
        when(repository.getAllTemplates()).thenReturn(List.of());
        return repository;
    }

    /**
     * State of one application context
     */
    static class StartupRecorder {
        volatile int listenerInvocations;
        volatile boolean lifecycleStarted;
        volatile boolean lifecycleStartedWhenListenerInvoked;
    }

    /**
     * Stands in for lifecycle beans like the web server, which must not be started before the listener ran. The Kafka
     * consumers are started on ApplicationStartedEvent, see RelationTypeDeclarationNotifierStartupTest
     */
    static class LifecycleProbe implements SmartLifecycle {
        private final StartupRecorder recorder;

        LifecycleProbe(StartupRecorder recorder) {
            this.recorder = recorder;
        }

        @Override
        public void start() {
            recorder.lifecycleStarted = true;
        }

        @Override
        public void stop() {
            recorder.lifecycleStarted = false;
        }

        @Override
        public boolean isRunning() {
            return recorder.lifecycleStarted;
        }
    }

    static class RecordingRelationListener implements RelationListener {
        private final StartupRecorder recorder;

        RecordingRelationListener(StartupRecorder recorder) {
            this.recorder = recorder;
        }

        @Override
        public void relationsAdded(Collection<Relation> relations) {
            // not relevant for these tests
        }

        @Override
        public void relationTypesDeclared(Set<RelationType> relationTypes) {
            recorder.listenerInvocations++;
            recorder.lifecycleStartedWhenListenerInvoked = recorder.lifecycleStarted;
        }
    }

    static class ThrowingRelationListener implements RelationListener {
        private final StartupRecorder recorder;

        ThrowingRelationListener(StartupRecorder recorder) {
            this.recorder = recorder;
        }

        @Override
        public void relationsAdded(Collection<Relation> relations) {
            // not relevant for these tests
        }

        @Override
        public void relationTypesDeclared(Set<RelationType> relationTypes) {
            throw new RecorderCarryingException(recorder);
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
