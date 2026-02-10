package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTOFactory;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class ProcessInstanceViewService {
    private final ProcessRelationsService processRelationsService;
    private final Optional<ProcessSnapshotRepository> processSnapshotRepository;
    private final ProcessInstanceQueryRepository repository;
    private final TranslateService translateService;
    private final MessageRepository messageRepository;
    private final MessageReferenceRepository messageReferenceRepository;
    private final RelationRepository relationRepository;
    private final ProcessDataRepository processDataRepository;
    private final TaskInstanceRepository taskInstanceRepository;

    public Optional<ProcessInstanceDTO> createDto(String originProcessId) {
        return repository.findByOriginProcessId(originProcessId)
                .map(this::createDtoFromProcessInstance)
                .or(() -> createDtoFromProcessSnapshot(originProcessId));
    }

    private ProcessInstanceDTO createDtoFromProcessInstance(ProcessInstance p) {
        return ProcessInstanceDTOFactory.createFromProcessInstance(p, translateService, processRelationsService, messageRepository, messageReferenceRepository, relationRepository, processDataRepository, taskInstanceRepository);
    }

    private Optional<ProcessInstanceDTO> createDtoFromProcessSnapshot(String originProcessId) {
        return processSnapshotRepository.flatMap(snapshotRepository ->
                snapshotRepository.loadAndDeserializeNewestSnapshot(originProcessId)
                        .map(processSnapshot -> ProcessInstanceDTOFactory.createFromSnapshot(processSnapshot, translateService)));
    }
}
