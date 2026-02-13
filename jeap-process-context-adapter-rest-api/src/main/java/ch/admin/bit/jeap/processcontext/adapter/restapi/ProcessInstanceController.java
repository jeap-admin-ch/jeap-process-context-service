package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/processes")
@Tag(name = "process-instance", description = "Process Instance Resource")
public class ProcessInstanceController {

    private final ProcessInstanceViewService processInstanceViewService;
    private final ProcessInstanceListDTOFactory listDTOFactory;

    @GetMapping("/")
    @PreAuthorize("hasRole('processinstance','view')")
    @Operation(summary = "Find Process Instances")
    @TransactionalReadReplica
    public Page<ProcessInstanceLightDTO> findProcessInstances(
            @RequestParam(name = "processData", required = false, defaultValue = "") String processData,
            @PageableDefault(size = 10) Pageable pageable) {

        return listDTOFactory.createProcessInstanceLightDTOPage(processData, pageable);
    }

    @GetMapping("/{originProcessId}")
    @PreAuthorize("hasRole('processinstance','view')")
    @Operation(summary = "Read Process Instance")
    @ApiResponse(responseCode = "200", description = "Process instance found")
    @ApiResponse(responseCode = "404", description = "Process instance not found for the given origin process ID")
    @TransactionalReadReplica
    public ProcessInstanceDTO getProcessInstanceByOriginProcessId(@PathVariable("originProcessId") String originProcessId) {
        return processInstanceViewService.createProcessInstanceDTO(originProcessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{originProcessId}/relations")
    @PreAuthorize("hasRole('processinstance','view')")
    @Operation(summary = "Read Relations for a Process Instance")
    @ApiResponse(responseCode = "200")
    @TransactionalReadReplica
    public Page<RelationDTO> getRelationsByOriginProcessId(@PathVariable("originProcessId") String originProcessId, Pageable pageable) {
        return processInstanceViewService.createRelationDTOPage(originProcessId, pageable);
    }

    @GetMapping("/{originProcessId}/process-relations")
    @PreAuthorize("hasRole('processinstance','view')")
    @Operation(summary = "Read Process Relations for a Process Instance")
    @ApiResponse(responseCode = "200")
    @TransactionalReadReplica
    public Page<ProcessRelationDTO> getProcessRelationsByOriginProcessId(@PathVariable("originProcessId") String originProcessId, Pageable pageable) {
        return processInstanceViewService.createProcessRelationDTOPage(originProcessId, pageable);
    }

    @GetMapping("/{originProcessId}/process-data")
    @PreAuthorize("hasRole('processinstance','view')")
    @Operation(summary = "Read Process Data for a Process Instance")
    @ApiResponse(responseCode = "200")
    @TransactionalReadReplica
    public Page<ProcessDataDTO> getProcessDataByOriginProcessId(@PathVariable("originProcessId") String originProcessId, Pageable pageable) {
        return processInstanceViewService.createProcessDataDTOPage(originProcessId, pageable);
    }

    @GetMapping("/{originProcessId}/messages")
    @PreAuthorize("hasRole('processinstance','view')")
    @Operation(summary = "Read Messages for a Process Instance. Returns max. 10 message data items per message.")
    @ApiResponse(responseCode = "200")
    @TransactionalReadReplica
    public Page<MessageDTO> getMessagesByOriginProcessId(@PathVariable("originProcessId") String originProcessId, Pageable pageable) {
        return processInstanceViewService.createMessageDTOPage(originProcessId, pageable);
    }
}
