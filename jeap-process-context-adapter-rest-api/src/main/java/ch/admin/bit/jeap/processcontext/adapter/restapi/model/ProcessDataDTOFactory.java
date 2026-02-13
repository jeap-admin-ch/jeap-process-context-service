package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessDataDTOFactory {

    private static final Comparator<ProcessDataDTO> PROCESS_DATA_COMPARATOR = Comparator
            .comparing(ProcessDataDTO::getKey)
            .thenComparing(ProcessDataDTO::getValue);

    private final ProcessDataRepository processDataRepository;

    public Page<ProcessDataDTO> createProcessDataDTOPage(UUID processInstanceId, Pageable pageable) {
        Sort sort = Sort.by("key").ascending()
                .and(Sort.by("value").ascending());
        Pageable sortedPageable = pageable.isUnpaged() ? pageable :
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return processDataRepository.findByProcessInstanceId(processInstanceId, sortedPageable)
                .map(ProcessDataDTO::create);
    }

    public Page<ProcessDataDTO> createProcessDataDTOPageFromSnapshot(ProcessSnapshot snapshot, Pageable pageable) {
        return PageUtils.toPage(createProcessData(snapshot.getProcessData()), pageable);
    }

    private static List<ProcessDataDTO> createProcessData(List<ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData> processData) {
        if (processData == null) {
            return List.of();
        }
        return processData.stream()
                .map(ProcessDataDTO::create)
                .sorted(PROCESS_DATA_COMPARATOR)
                .toList();
    }
}
