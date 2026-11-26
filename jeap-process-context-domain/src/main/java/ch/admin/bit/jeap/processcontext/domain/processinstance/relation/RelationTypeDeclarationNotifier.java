package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationPattern;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reports the relation types declared in all process templates to the {@link RelationListener} at startup.
 * <p>
 * Runs in {@link SmartInitializingSingleton#afterSingletonsInstantiated()}, i.e. after all singletons (including the
 * template repository) have been instantiated, but before the context is refreshed completely: before lifecycle beans
 * (e.g. the web server) are started, and before the {@code ApplicationStartedEvent} is published, on which
 * {@code MessageService} starts the Kafka message consumers. An exception thrown by the listener thus aborts the
 * startup before any message is consumed. Spring Boot excludes {@link SmartInitializingSingleton}s from lazy
 * initialization, so this also holds with {@code spring.main.lazy-initialization=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelationTypeDeclarationNotifier implements SmartInitializingSingleton {

    private final ProcessTemplateRepository processTemplateRepository;
    private final RelationListener relationListener;

    @Override
    public void afterSingletonsInstantiated() {
        Set<RelationType> relationTypes = collectRelationTypes();
        log.info("Reporting {} relation types declared in process templates to the relation listener", relationTypes.size());
        relationListener.relationTypesDeclared(relationTypes);
    }

    /**
     * @return Distinct relation types, ordered by template name and then by the order of the patterns in the template,
     * as an unmodifiable set handed over to the plugin
     */
    Set<RelationType> collectRelationTypes() {
        // The repository returns the templates in no defined order, sort them for a deterministic result
        return processTemplateRepository.getAllTemplates().stream()
                .sorted(Comparator.comparing(ProcessTemplate::getName))
                .flatMap(template -> template.getRelationPatterns().stream()
                        .map(pattern -> toRelationType(template, pattern)))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
    }

    /**
     * Mandatory in templates declaring relation patterns. Enforced when loading JSON templates (see
     * ProcessTemplateDeserializer), but not by ProcessTemplate itself, e.g. for templates created programmatically.
     */
    private static String relationSystemId(ProcessTemplate template) {
        String relationSystemId = template.getRelationSystemId();
        if (relationSystemId == null || relationSystemId.isBlank()) {
            throw new IllegalStateException(
                    "Process template '%s' declares relation patterns but no relationSystemId".formatted(template.getName()));
        }
        return relationSystemId;
    }

    private static RelationType toRelationType(ProcessTemplate template, RelationPattern pattern) {
        return RelationType.builder()
                .systemId(relationSystemId(template))
                .predicateType(pattern.getPredicateType())
                .subjectType(pattern.getSubjectSelector().getType())
                .objectType(pattern.getObjectSelector().getType())
                .featureFlag(pattern.getFeatureFlag())
                .build();
    }
}
