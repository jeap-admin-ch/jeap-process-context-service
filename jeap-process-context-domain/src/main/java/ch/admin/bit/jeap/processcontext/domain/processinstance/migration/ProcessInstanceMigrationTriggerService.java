package ch.admin.bit.jeap.processcontext.domain.processinstance.migration;

import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessInstanceMigrationTriggerService {

    private final ProcessTemplateRepository processTemplateRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final InternalMessageProducer internalMessageProducer;

    /**
     * Trigger a migration for process instances with outdated template hashes,
     * limited to {@code batchSize} total events across all templates.
     *
     * @return the total number of migration events sent
     */
    @Timed(value = "jeap_pcs_migration_trigger", description = "Time taken to trigger process instance migration for modified templates")
    public int triggerMigrationForModifiedTemplates(ZonedDateTime createdAtAfter, int batchSize) {
        List<String> originProcessIds = findOriginProcessIdsToMigrate(createdAtAfter, batchSize);
        if (originProcessIds.isEmpty()) {
            return 0;
        }
        String idempotenceId = createdAtAfter.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        log.info("Sending {} migration trigger events", originProcessIds.size());
        internalMessageProducer.produceProcessContextOutdatedMigrationTriggerEvents(originProcessIds, idempotenceId);
        return originProcessIds.size();
    }

    private List<String> findOriginProcessIdsToMigrate(ZonedDateTime createdAtAfter, int batchSize) {
        List<String> allIds = new ArrayList<>();
        for (ProcessTemplate template : processTemplateRepository.getAllTemplates()) {
            int remaining = batchSize - allIds.size();
            if (remaining <= 0) {
                break;
            }
            List<String> ids = processInstanceRepository
                    .findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(createdAtAfter, template, remaining);
            allIds.addAll(ids);
        }
        return allIds;
    }
}
