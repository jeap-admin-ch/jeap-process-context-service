package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessDataTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
class ProcessDataService {

    private final ProcessDataRepository processDataRepository;

    List<ProcessData> copyMessageDataToProcessData(ProcessInstance processInstance, Message message) {
        String messageName = message.getMessageName();
        Set<MessageData> messageData = message.getMessageData(processInstance.getProcessTemplateName());
        List<ProcessDataTemplate> processDataTemplates = processInstance.getProcessTemplate()
                .getProcessDataTemplatesBySourceMessageName(messageName);
        List<ProcessData> addedProcessData = new ArrayList<>();
        processDataTemplates.forEach(template ->
                applyProcessDataTemplate(processInstance, addedProcessData, messageData, template));
        return addedProcessData;
    }

    private void applyProcessDataTemplate(ProcessInstance processInstance, List<ProcessData> addedProcessData,
                                          Set<MessageData> messageDataSet, ProcessDataTemplate processDataTemplate) {
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
        ProcessData processDataItem = new ProcessData(targetKey, messageData.getValue(), messageData.getRole());
        processDataItem.setProcessInstance(processInstance);
        boolean saved = processDataRepository.saveIfNew(processDataItem);
        return saved ? processDataItem : null;
    }
}
