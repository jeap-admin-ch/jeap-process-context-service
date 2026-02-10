package ch.admin.bit.jeap.processcontext.domain.processinstance.migration;

import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessInstanceMigrationTriggerService {

    private final ProcessTemplateRepository processTemplateRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final InternalMessageProducer internalMessageProducer;

    /**
     * Trigger a migration for all not-completed process instances with <pre>modifiedAt &gt; createdAtAfter</pre>
     * for which the template has been modified.
     */
    @Async
    public void triggerMigrationForModifiedTemplates(ZonedDateTime createdAtAfter) {
        processTemplateRepository.getAllTemplates()
                .forEach(template -> triggerMigrationForTemplateIfModified(template, createdAtAfter));
    }

    private void triggerMigrationForTemplateIfModified(ProcessTemplate template, ZonedDateTime createdAtAfter) {
        Pageable pageable = Pageable.ofSize(10);
        while (pageable.isPaged()) {
            Slice<String> changedTemplateOriginProcessIds = processInstanceRepository
                    .findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(createdAtAfter, template, pageable);

            changedTemplateOriginProcessIds.forEach(this::sendProcessOutdatedEvent);

            pageable = changedTemplateOriginProcessIds.nextPageable();
        }
    }

    private void sendProcessOutdatedEvent(String originProcessId) {
        log.info("Triggering process outdated event due to changed template for process {}", keyValue("originProcessId", originProcessId));
        internalMessageProducer.produceProcessContextOutdatedMigrationTriggerEventSynchronously(originProcessId);
    }
}
