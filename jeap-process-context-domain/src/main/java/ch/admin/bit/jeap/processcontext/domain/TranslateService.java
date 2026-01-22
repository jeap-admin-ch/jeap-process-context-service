package ch.admin.bit.jeap.processcontext.domain;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
@Slf4j
public class TranslateService {

    private static final String DEFAULT_BUNDLE_LOCATION = "default-messages/messages";

    private static final String BUNDLE_LOCATION = "process/messages/messages";
    public static final String TASK = ".task.";
    public static final String FALLBACK_DEFAULT = "-";

    private final ResourceBundle defaultGerman;
    private final ResourceBundle defaultFrench;
    private final ResourceBundle defaultItalian;

    private final ResourceBundle german;
    private final ResourceBundle french;
    private final ResourceBundle italian;

    public TranslateService() {
        log.info("Loading default translations ResourceBundle from classpath {}", DEFAULT_BUNDLE_LOCATION);
        this.defaultGerman = ResourceBundle.getBundle(DEFAULT_BUNDLE_LOCATION);
        this.defaultFrench = ResourceBundle.getBundle(DEFAULT_BUNDLE_LOCATION, Locale.FRENCH);
        this.defaultItalian = ResourceBundle.getBundle(DEFAULT_BUNDLE_LOCATION, Locale.ITALIAN);
        log.info("Default ResourceBundles loaded");

        log.info("Loading translations ResourceBundle from classpath {}", BUNDLE_LOCATION);
        this.german = ResourceBundle.getBundle(BUNDLE_LOCATION);
        this.french = ResourceBundle.getBundle(BUNDLE_LOCATION, Locale.FRENCH);
        this.italian = ResourceBundle.getBundle(BUNDLE_LOCATION, Locale.ITALIAN);
        log.info("ResourceBundles loaded");
    }

    public Map<String, String> translateProcessTemplateName(@NonNull String processTemplateName) {
        return retrieveLabels(processTemplateName + ".label");
    }

    public Map<String, String> translateProcessTemplateName(@NonNull String processTemplateName, @NonNull String processTemplateLabel) {
        return retrieveLabelsWithDefault(processTemplateName + ".label", processTemplateLabel);
    }

    public Map<String, String> translateTaskTypeName(@NonNull String processTemplate, @NonNull String taskTypeName) {
        return retrieveLabels(processTemplate + TASK + taskTypeName);
    }

    public Map<String, String> translateTaskDataKey(@NonNull String processTemplate, @NonNull String taskTypeName, @NonNull String taskDataKey) {
        return retrieveLabels(processTemplate + TASK + taskTypeName + ".data." + taskDataKey);
    }

    public Map<String, String> translateProcessRelationOriginRole(@NonNull String processTemplate, @NonNull String relationName) {
        return retrieveLabels(processTemplate + "." + relationName + ".originRole");
    }

    public Map<String, String> translateProcessRelationTargetRole(@NonNull String processTemplate, @NonNull String relationName) {
        return retrieveLabels(processTemplate + "." + relationName + ".targetRole");
    }

    public Map<String, String> translateProcessCompletionName(@NonNull String processTemplate, String processCompletionName) {
        if (processCompletionName == null) {
            return noTranslation(FALLBACK_DEFAULT);
        }
        if (processCompletionName.equals("legacyProcessCompletionCondition") || processCompletionName.equals("allTasksInFinalStateProcessCompletionCondition")) {
            return retrieveLabels("completion." + processCompletionName);
        }
        String key = processTemplate + ".completion." + processCompletionName;
        try {
            return retrieveLabels(key);
        } catch (IllegalArgumentException e) {
            log.warn("No translation found for key '{}'. Using the name as label...", key);
            return noTranslation(processCompletionName);
        }
    }

    public Map<String, String> translateProcessCompletionNameFromSnapshot(@NonNull String processTemplate, String processCompletionName, String completionLegacyLabel) {
        if (processCompletionName != null) {
            if (processCompletionName.equals("legacyProcessCompletionCondition") || processCompletionName.equals("allTasksInFinalStateProcessCompletionCondition")) {
                return retrieveLabels("completion." + processCompletionName);
            }
            String key = processTemplate + ".completion." + processCompletionName;
            try {
                return retrieveLabels(key);
            } catch (IllegalArgumentException e) {
                log.warn("No translation found for key '{}'", key);
            }
        }
        if (completionLegacyLabel != null) {
            log.warn("Using the legacy label as label...");
            return noTranslation(completionLegacyLabel);
        } else if (processCompletionName != null) {
            log.warn("Using the processCompletionName as label...");
            return noTranslation(processCompletionName);
        }
        return noTranslation(FALLBACK_DEFAULT);
    }

    public Map<String, String> translateUserDataKey(@NonNull String userDataKey) {
        return retrieveLabelsWithDefault("userData." + userDataKey, userDataKey);
    }

    public Map<String, String> translateUserDataKey(@NonNull String userDataKey, String userDataLabel) {
        return retrieveLabelsWithDefault("userData." + userDataKey, StringUtils.hasText(userDataLabel) ? userDataLabel: userDataKey);
    }

    public Map<String, String> translateTaskDataKey(@NonNull String processTemplate, @NonNull String taskTypeName, @NonNull String taskDataKey, String taskDataLabel) {
        return retrieveLabelsWithDefault(processTemplate + TASK + taskTypeName + ".data." + taskDataKey,  StringUtils.hasText(taskDataLabel) ? taskDataLabel: taskDataKey);
    }

    public Map<String, String> translateTaskTypeName(@NonNull String processTemplate, @NonNull String taskTypeName, String taskTypeLabel) {
        return retrieveLabelsWithDefault(processTemplate + TASK + taskTypeName,  StringUtils.hasText(taskTypeLabel) ? taskTypeLabel: taskTypeName);
    }

    public static Map<String, String> noTranslation(@NonNull String value) {
        Map<String, String> translations = new HashMap<>();
        Arrays.asList(Language.values()).forEach(
                language -> translations.put(language.languageId(), value));
        return translations;
    }

    private Map<String, String> retrieveLabelsWithDefault(@NonNull String key, @NonNull String defaultValue) {
        try {
            return retrieveLabels(key);
        } catch (IllegalArgumentException e) {
            log.warn("No translation found for key '{}'. Using the defaultValue as translation...", key);
            return noTranslation(defaultValue);
        }
    }

    private Map<String, String> retrieveLabels(@NonNull String key) {
        Map<String, String> labels = new HashMap<>();
        labels.put(Language.DE.languageId(), getString(defaultGerman, german, key));
        labels.put(Language.FR.languageId(), getString(defaultFrench, french, key));
        labels.put(Language.IT.languageId(), getString(defaultItalian, italian, key));
        return labels;
    }

    private String getString(@NonNull ResourceBundle defaultResourceBundle, @NonNull ResourceBundle resourceBundle, @NonNull String key) {
        if (resourceBundle.containsKey(key)) {
            return resourceBundle.getString(key);
        }

        if (defaultResourceBundle.containsKey(key)) {
            return defaultResourceBundle.getString(key);
        }
        throw new IllegalArgumentException("No translation found in ResourceBundles for key: '" + key + "'");
    }
}
