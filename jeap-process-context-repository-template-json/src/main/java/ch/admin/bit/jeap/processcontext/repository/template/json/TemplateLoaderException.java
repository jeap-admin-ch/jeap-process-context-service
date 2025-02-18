package ch.admin.bit.jeap.processcontext.repository.template.json;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Collection;

class TemplateLoaderException extends RuntimeException {

    private TemplateLoaderException(String message) {
        super(message);
    }

    private TemplateLoaderException(String message, Throwable t) {
        super(message, t);
    }

    static TemplateLoaderException duplicateProcessTemplateName(String templateName) {
        return new TemplateLoaderException("Duplicate process template name " + templateName);
    }

    static TemplateLoaderException duplicatedProcessInstantiationTrigger(String eventName, Collection<String> templateNames) {
        return new TemplateLoaderException("Duplicated process instantiation trigger/condition for the event '" + eventName +
                "' found configured by the following process templates: " + String.join(", ", templateNames));
    }

    static TemplateLoaderException failedToLoadResource(Resource resource, IOException ex) {
        return new TemplateLoaderException("Failed to load template from resource " + resource, ex);
    }
}