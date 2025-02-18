package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface ProcessTemplateRepository {

    List<ProcessTemplate> getAllTemplates();

    Optional<ProcessTemplate> findByName(String templateName);

    Map<String, MessageReference> getMessageReferencesByTemplateNameForMessageName(String messageName);

    /**
     * Is there any template in the repository that configures the creation of process snapshots?
     * @return <code>true</code> if a template configures the creation of process snapshots, <code>false</code> otherwise.
     */
    default boolean hasProcessSnapshotsConfigured() {
        return Stream.ofNullable(getAllTemplates()).flatMap(List::stream).
                flatMap(template -> template.getProcessSnapshotConditions().stream()).
                findAny().isPresent();
    }

}
