package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessDataTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessDataService {

    private final ProcessDataRepository processDataRepository;

    List<ProcessData> copyMessageDataToProcessData(ProcessInstance processInstance, Message message) {
        String messageName = message.getMessageName();
        List<MessageData> messageData = message.getMessageData(processInstance.getProcessTemplateName());
        List<ProcessDataTemplate> processDataTemplates = processInstance.getProcessTemplate()
                .getProcessDataTemplatesBySourceMessageName(messageName);
        List<ProcessData> addedProcessData = new ArrayList<>();
        processDataTemplates.forEach(template ->
                applyProcessDataTemplate(processInstance, addedProcessData, messageData, template));
        return addedProcessData;
    }

    public void addProcessData(ProcessInstance processInstance, Collection<ProcessDataValue> values) {
        values.forEach(value -> addProcessDataToProcessInstance(
                processInstance, value.key(), value.value(), value.role()));
    }

    private void applyProcessDataTemplate(ProcessInstance processInstance, List<ProcessData> addedProcessData,
                                          List<MessageData> messageDataSet, ProcessDataTemplate processDataTemplate) {
        String sourceKey = processDataTemplate.getSourceMessageDataKey();
        String targetKey = processDataTemplate.getKey();
        for (MessageData messageData : messageDataSet) {
            addProcessDataIfKeyMatches(processInstance, addedProcessData, messageData, sourceKey, targetKey);
        }
    }

    private void addProcessDataIfKeyMatches(ProcessInstance processInstance, List<ProcessData> addedProcessData, MessageData messageData, String sourceKey, String targetKey) {
        if (sourceKey.equals(messageData.getKey())) {
            ProcessData data = addProcessDataToProcessInstance(processInstance, targetKey, messageData);
            if (data != null) {
                addedProcessData.add(data);
            }
        }
    }

    private ProcessData addProcessDataToProcessInstance(ProcessInstance processInstance, String targetKey, MessageData messageData) {
        return addProcessDataToProcessInstance(processInstance, targetKey, messageData.getValue(), messageData.getRole());
    }

    private ProcessData addProcessDataToProcessInstance(ProcessInstance processInstance, String key, String value,
                                                        String role) {
        ProcessData processDataItem = new ProcessData(key, value, role);
        processDataItem.setProcessInstance(processInstance);
        boolean saved = processDataRepository.saveIfNew(processDataItem);
        return saved ? processDataItem : null;
    }
}
