package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
public class ProcessInstanceListDTOFactory {

    private final ProcessInstanceQueryRepository repository;
    private final TranslateService translateService;
    private final MessageReferenceRepository messageReferenceRepository;

    public Page<ProcessInstanceLightDTO> createProcessInstanceLightDTOPage(String processData, Pageable pageable) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        if (processData.isEmpty()) {
            return convertProcessInstancePage(repository.findAll(pageRequest));
        }
        return convertProcessInstancePage(repository.findByProcessData(processData, pageRequest));
    }

    private Page<ProcessInstanceLightDTO> convertProcessInstancePage(Page<ProcessInstance> processInstancePage) {
        if (processInstancePage == null) {
            return Page.empty();
        }
        List<ProcessInstance> processInstanceList = processInstancePage.getContent();

        Set<UUID> processInstanceIds = processInstanceList.stream()
                .map(ProcessInstance::getId)
                .collect(toSet());
        Map<UUID, ZonedDateTime> lastMessageDates = messageReferenceRepository
                .findLastMessageCreatedAtByProcessInstanceIds(processInstanceIds);

        List<ProcessInstanceLightDTO> lightDtoList = processInstanceList.stream()
                .map(p -> ProcessInstanceLightDTO.create(p, translateService, lastMessageDates.get(p.getId())))
                .toList();

        return new PageImpl<>(lightDtoList, processInstancePage.getPageable(), processInstancePage.getTotalElements());
    }
}
