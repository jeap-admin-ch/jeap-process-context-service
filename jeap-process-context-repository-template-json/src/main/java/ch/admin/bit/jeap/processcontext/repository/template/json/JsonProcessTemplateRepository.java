package ch.admin.bit.jeap.processcontext.repository.template.json;

import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.MessageReference;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.plugin.api.condition.MessageProcessCompletionCondition;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;

@Component
@Slf4j
@RequiredArgsConstructor
class JsonProcessTemplateRepository implements ProcessTemplateRepository {
    private static final String CLASSPATH_LOCATION = "classpath:/process/templates/*.json";
    private final TranslateService translateService;
    private final JsonProcessTemplateConsumerContractValidator jsonProcessTemplateConsumerContractValidator;
    private final Map<String, Map<String, MessageReference>> domainEventReferencesByEventNameThenTemplateName = new HashMap<>();
    private Map<String, ProcessTemplate> templatesByName;

    @Value("${jeap.processcontext.consumer-contract-validator.enabled:true}")
    private boolean consumerContractValidatorEnabled = true;
    private boolean anyTemplateHasEventsCorrelatedByProcessData = false;

    private static Map<String, ProcessTemplate> getTemplatesFromClasspath() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(CLASSPATH_LOCATION);
        return JsonProcessTemplateLoader.loadTemplateResources(resources);
    }

    @Override
    public Optional<ProcessTemplate> findByName(String templateName) {
        return Optional.ofNullable(templatesByName.get(templateName));
    }

    @Override
    public List<ProcessTemplate> getAllTemplates() {
        return List.copyOf(templatesByName.values());
    }

    @Override
    public Map<String, MessageReference> getMessageReferencesByTemplateNameForMessageName(String messageName) {
        return domainEventReferencesByEventNameThenTemplateName.getOrDefault(messageName, emptyMap());
    }

    @Override
    public boolean isAnyTemplateHasEventsCorrelatedByProcessData() {
        return anyTemplateHasEventsCorrelatedByProcessData;
    }

    @PostConstruct
    void loadTemplates() throws IOException {
        this.templatesByName = getTemplatesFromClasspath();
        templatesByName.forEach((key, value) -> value.getMessageReferences().forEach(reference ->
                domainEventReferencesByEventNameThenTemplateName.compute(reference.getMessageName(), (eventName, referencesByTemplateName) -> {
                    if (referencesByTemplateName == null) {
                        referencesByTemplateName = new HashMap<>();
                    }
                    referencesByTemplateName.put(key, reference);
                    return referencesByTemplateName;
                })
        ));

        this.templatesByName.values().forEach(this::validateTemplateTranslations);
        if (consumerContractValidatorEnabled) {
            this.templatesByName.values().forEach(jsonProcessTemplateConsumerContractValidator::validateContract);
        }

        anyTemplateHasEventsCorrelatedByProcessData = templatesByName.values().stream()
                .anyMatch(ProcessTemplate::isAnyEventCorrelatedByProcessData);
    }

    private void validateTemplateTranslations(ProcessTemplate processTemplate) {
        log.info("Validating translations for template: {}", processTemplate.getName());
        translateService.translateProcessTemplateName(processTemplate.getName());

        processTemplate.getTaskTypes().forEach(taskType ->
        {
            log.info("Validating task {}", taskType.getName());
            translateService.translateTaskTypeName(processTemplate.getName(), taskType.getName());
            taskType.getTaskData().forEach(taskData ->
                    taskData.getMessageDataKeys().forEach(taskDataKey ->
                            translateService.translateTaskDataKey(processTemplate.getName(), taskType.getName(), taskDataKey))
            );

        });

        processTemplate.getMilestoneNames().forEach(milestoneName ->
        {
            log.info("Validating milestoneName {}", milestoneName);
            translateService.translateMilestoneName(processTemplate.getName(), milestoneName);
        });

        processTemplate.getProcessCompletionConditions().forEach(completionCondition ->
        {
            if (completionCondition instanceof MessageProcessCompletionCondition messageProcessCompletionCondition) {
                log.info("Validating messageProcessCompletionCondition {}", messageProcessCompletionCondition.getName());
                translateService.translateProcessCompletionName(processTemplate.getName(), messageProcessCompletionCondition.getName());
            }
        });

        processTemplate.getProcessRelationPatterns().forEach(processRelationPattern ->
        {
            log.info("Validating processRelationPattern {}", processRelationPattern.getName());
            translateService.translateProcessRelationTargetRole(processTemplate.getName(), processRelationPattern.getName());
            translateService.translateProcessRelationOriginRole(processTemplate.getName(), processRelationPattern.getName());
        });
    }
}
