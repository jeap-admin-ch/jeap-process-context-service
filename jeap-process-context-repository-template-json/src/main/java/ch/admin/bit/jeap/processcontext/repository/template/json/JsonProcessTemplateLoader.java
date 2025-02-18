package ch.admin.bit.jeap.processcontext.repository.template.json;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.plugin.api.event.AlwaysProcessInstantiationCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.event.NeverProcessInstantiationCondition;
import ch.admin.bit.jeap.processcontext.plugin.api.event.ProcessInstantiationCondition;
import ch.admin.bit.jeap.processcontext.repository.template.json.deserializer.ProcessTemplateDeserializer;
import ch.admin.bit.jeap.processcontext.repository.template.json.model.ProcessTemplateDefinition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
class JsonProcessTemplateLoader {

    private static final String DUPLICATED_PROCESS_INSTANTIATION_ERROR_MSG = """
        There are more than on process templates ({}) configuring the same process instantiation for the event {}.
        This is not supported as it would result in more than one process instances having the same origin process id.""";

    static Map<String, ProcessTemplate> loadTemplateResources(Resource[] resources) {
        Map<String, ProcessTemplate> templatesByName = new HashMap<>();
        for (Resource resource : resources) {
            try {
                loadTemplateResource(templatesByName, resource);
            } catch (IOException ex) {
                throw TemplateLoaderException.failedToLoadResource(resource, ex);
            }
        }
        validateProcessInstantiationConfigurationAcrossTemplates(templatesByName.values());
        return templatesByName;
    }

    private static void validateProcessInstantiationConfigurationAcrossTemplates(Collection<ProcessTemplate> templates) {
        Map<String, Map<Class<?>, Set<String>>> templateNamesByEventAndProcessInstantiationCondition = new HashMap<>();
        templates.forEach(template ->
                template.getMessageReferences().forEach(eventReference -> {
                ProcessInstantiationCondition<Message> condition = eventReference.getProcessInstantiationCondition();
                if (!(condition instanceof NeverProcessInstantiationCondition)) {
                    String eventName = eventReference.getMessageName();
                    var templatesByCondition = templateNamesByEventAndProcessInstantiationCondition
                            .computeIfAbsent(eventName, er -> new HashMap<>());

                    var templateNames = templatesByCondition
                            .computeIfAbsent(eventReference.getProcessInstantiationCondition().getClass(), c -> new HashSet<>());
                    templateNames.add(template.getName());
                    if (templateNames.size() > 1) {
                        log.error(DUPLICATED_PROCESS_INSTANTIATION_ERROR_MSG, String.join(", ", templateNames), eventName);
                        throw TemplateLoaderException.duplicatedProcessInstantiationTrigger(eventName, templateNames);
                    }
                    if ((templatesByCondition.size() > 1) && (templatesByCondition.get(AlwaysProcessInstantiationCondition.class) != null)) {
                        Collection<String> conflictingTemplateNames = templatesByCondition.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
                        log.error(DUPLICATED_PROCESS_INSTANTIATION_ERROR_MSG, String.join(", ", conflictingTemplateNames), eventName);
                        throw TemplateLoaderException.duplicatedProcessInstantiationTrigger(eventName, conflictingTemplateNames);

                    }
                }
            })
        );
    }

    private static void loadTemplateResource(Map<String, ProcessTemplate> templatesByName, Resource resource) throws IOException {
        ProcessTemplate template = loadTemplateResource(resource);
        String templateName = template.getName();

        if (templatesByName.containsKey(templateName)) {
            throw TemplateLoaderException.duplicateProcessTemplateName(templateName);
        }

        templatesByName.put(templateName, template);
    }

    private static ProcessTemplate loadTemplateResource(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            log.info("Loading process template from " + resource.getDescription());

            String templateHash = computeCanonicalTemplateJsonHash(resource);

            ProcessTemplateDefinition templateDefinition = objectMapper.readValue(inputStream, ProcessTemplateDefinition.class);
            ProcessTemplateDeserializer processTemplateDeserializer = new ProcessTemplateDeserializer(templateDefinition, templateHash);
            return processTemplateDeserializer.toProcessTemplate();
        }
    }

    private static String computeCanonicalTemplateJsonHash(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            JsonCanonicalizer jc = new JsonCanonicalizer(inputStream.readAllBytes());
            return DigestUtils.md5DigestAsHex(jc.getEncodedUTF8()).toUpperCase();
        }
    }

    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();

    // To hide implicit public constructor
    private JsonProcessTemplateLoader() {
    }
}
