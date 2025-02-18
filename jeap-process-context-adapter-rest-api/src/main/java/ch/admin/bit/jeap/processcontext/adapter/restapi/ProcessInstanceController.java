package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.adapter.restapi.model.*;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/processes")
@Tag(name = "process-instance", description = "Process Instance Resource")
public class ProcessInstanceController {

    private final ProcessInstanceQueryRepository repository;
    private final ProcessInstanceService processInstanceService;
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

    /**
     * Create new process instance
     * @deprecated
     * this method should no longer be used.
     * <p> Use instantiation with domain event/command or create process instance command instead.
     *
     * @param originProcessId the originProcessId for the process instance
     * @param newProcessInstanceDTO the newProcessInstanceDTO for the creation
     */
    @PutMapping("/{originProcessId}")
    @PreAuthorize("hasRole('processinstance','create')")
    @Operation(summary = "Create Process Instance")
    @ApiResponse(responseCode = "201", description = "Process instance successfully created")
    @ApiResponse(responseCode = "400", description = "No process template found for the given template name")
    @Deprecated(since = "5.0.0")
    public ResponseEntity<Void> putNewProcessInstance(
            @PathVariable("originProcessId") String originProcessId,
            @RequestBody @Valid NewProcessInstanceDTO newProcessInstanceDTO) {

        try {
            processInstanceService.createProcessInstance(
                    originProcessId,
                    newProcessInstanceDTO.getProcessTemplateName(),
                    toProcessData(newProcessInstanceDTO.getExternalReferences()));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .build();

        } catch (TaskPlanningException exc) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TaskPlanningException", exc);
        }
    }

    private Set<ProcessData> toProcessData(Set<ExternalReferenceDTO> externalReferenceDTOS) {
        if (externalReferenceDTOS == null) {
            return emptySet();
        }
        return externalReferenceDTOS.stream().map(this::toProcessData).collect(Collectors.toSet());
    }

    private ProcessData toProcessData(ExternalReferenceDTO externalReferenceDTO) {
        return new ProcessData(externalReferenceDTO.getName(), externalReferenceDTO.getValue());
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
