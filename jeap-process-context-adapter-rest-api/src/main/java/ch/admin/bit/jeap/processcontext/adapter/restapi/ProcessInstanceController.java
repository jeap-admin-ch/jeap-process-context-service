package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceLightDto;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceListDto;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceQueryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static java.util.Collections.emptyList;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/processes")
@Tag(name = "process-instance", description = "Process Instance Resource")
public class ProcessInstanceController {

    private final ProcessInstanceQueryRepository repository;
    private final TranslateService translateService;
    private final ProcessInstanceViewService processInstanceViewService;

    @GetMapping("/")
    @PreAuthorize("hasRole('processinstance','view')")
    @Operation(summary = "Find Process Instances")
    @Transactional(readOnly = true)
    public ProcessInstanceListDto findProcessInstances(
            @RequestParam(name = "processData", required = false, defaultValue = "") String processData,
            @RequestParam(name = "pageIndex", required = false, defaultValue = "0") int pageIndex,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize) {

        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (processData.isEmpty()) {
            return convertProcessInstanceList(repository.findAll(pageRequest));
        }
        return convertProcessInstanceList(repository.findByProcessData(processData, pageRequest));
    }

    @GetMapping("/{originProcessId}")
    @PreAuthorize("hasRole('processinstance','view')")
    @Operation(summary = "Read Process Instance")
    @ApiResponse(responseCode = "200", description = "Process instance found")
    @ApiResponse(responseCode = "404", description = "Process instance not found for the given origin process ID")
    @Transactional(readOnly = true)
    public ProcessInstanceDTO getProcessInstanceByOriginProcessId(@PathVariable("originProcessId") String originProcessId) {
        return processInstanceViewService.createDto(originProcessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ProcessInstanceListDto convertProcessInstanceList(Page<ProcessInstance> processInstancePage) {
        if (processInstancePage == null) {
            return emptyDto();
        }
        List<ProcessInstance> processInstanceList = processInstancePage.getContent();
        List<ProcessInstanceLightDto> lightDtoList = processInstanceList.stream()
                .map(p -> ProcessInstanceLightDto.create(p, translateService))
                .toList();

        return ProcessInstanceListDto.builder()
                .processInstanceLightDtoList(lightDtoList)
                .totalCount(processInstancePage.getTotalElements())
                .build();
    }

    private ProcessInstanceListDto emptyDto() {
        return ProcessInstanceListDto.builder()
                .processInstanceLightDtoList(emptyList())
                .totalCount(0)
                .build();
    }

}
