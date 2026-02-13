package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.adapter.restapi.model.*;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProcessInstanceViewService {
    private final Optional<ProcessSnapshotRepository> processSnapshotRepository;
    private final ProcessInstanceQueryRepository repository;
    private final TranslateService translateService;
    private final ProcessInstanceDTOFactory processInstanceDTOFactory;
    private final RelationDTOFactory relationDTOFactory;
    private final ProcessRelationDTOFactory processRelationDTOFactory;
    private final ProcessDataDTOFactory processDataDTOFactory;
    private final MessageDTOFactory messageDTOFactory;

    public Optional<ProcessInstanceDTO> createProcessInstanceDTO(String originProcessId) {
        return repository.findByOriginProcessId(originProcessId)
                .map(this::createDTOFromProcessInstance)
                .or(() -> createDTOFromProcessSnapshot(originProcessId));
    }

    private ProcessInstanceDTO createDTOFromProcessInstance(ProcessInstance p) {
        return processInstanceDTOFactory.createFromProcessInstance(p);
    }

    private Optional<ProcessInstanceDTO> createDTOFromProcessSnapshot(String originProcessId) {
        Optional<ProcessSnapshot> snapshot = findSnapshot(originProcessId);
        return snapshot.map(processSnapshot ->
                ProcessInstanceDTOFactory.createFromSnapshot(processSnapshot, translateService));
    }

    private Optional<ProcessSnapshot> findSnapshot(String originProcessId) {
        return processSnapshotRepository.flatMap(r -> r.loadAndDeserializeNewestSnapshot(originProcessId));
    }

    public Page<RelationDTO> createRelationDTOPage(String originProcessId, Pageable pageable) {
        UUID instanceId = repository.findIdByOriginProcessId(originProcessId);
        if (instanceId == null) {
            // Relations are not stored in the snapshot
            return Page.empty(pageable);
        }
        return relationDTOFactory.createRelationDTOPage(instanceId, pageable);
    }

    public Page<ProcessRelationDTO> createProcessRelationDTOPage(String originProcessId, Pageable pageable) {
        return repository.findByOriginProcessId(originProcessId)
                .map(processInstance -> processRelationDTOFactory.createProcessRelationDTOPage(processInstance, pageable))
                .orElse(findSnapshot(originProcessId).map(snap -> processRelationDTOFactory.createProcessRelationDTOPageFromSnapshot(snap, pageable))
                        .orElse(Page.empty()));
    }

    public Page<ProcessDataDTO> createProcessDataDTOPage(String originProcessId, Pageable pageable) {
        UUID instanceId = repository.findIdByOriginProcessId(originProcessId);
        if (instanceId == null) {
            return findSnapshot(originProcessId)
                    .map(snap -> processDataDTOFactory.createProcessDataDTOPageFromSnapshot(snap, pageable))
                    .orElse(Page.empty(pageable));
        }
        return processDataDTOFactory.createProcessDataDTOPage(instanceId, pageable);
    }

    public Page<MessageDTO> createMessageDTOPage(String originProcessId, Pageable pageable) {
        UUID instanceId = repository.findIdByOriginProcessId(originProcessId);
        if (instanceId == null) {
            // Messages are not stored in the snapshot
            return Page.empty(pageable);
        }
        return messageDTOFactory.createMessageDTOPage(instanceId, pageable);
    }
}
