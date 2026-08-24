package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.maintenance.RelationPublicationJobService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.RelationPublicationJobSubmission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@ConditionalOnProperty(value = "jeap.processcontext.maintenance.enabled", havingValue = "true")
@RestController
@RequestMapping("/api/relation-publication-jobs")
@Tag(name = "Relation Publication Jobs", description = "Create and inspect relation-republication jobs.")
@RequiredArgsConstructor
public class RelationPublicationJobController {

    static final String APPLICATION_YAML_VALUE = "application/yaml";
    static final String APPLICATION_X_YAML_VALUE = "application/x-yaml";

    private final RelationPublicationJobService relationPublicationJobService;

    @Operation(
            summary = "Create a relation-republication job.",
            description = "Requires the semantic role processcontextjob:write.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = APPLICATION_YAML_VALUE,
                    schema = @Schema(implementation = RelationPublicationJobRequest.class),
                    examples = @ExampleObject(value = """
                            relationIds:
                              - 019c8c72-6fd1-7f25-a9a1-3b3d51fbb321
                              - 019c8c72-7b42-7a04-9443-bf8ec98ce871
                            """))),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Job created"),
                    @ApiResponse(responseCode = "200", description = "Job already exists with the same content"),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "403", description = "Access denied"),
                    @ApiResponse(responseCode = "409", description = "Job already exists with different content")
            },
            security = {@SecurityRequirement(name = "OIDC_Enduser"), @SecurityRequirement(name = "OIDC_System")})
    @PreAuthorize("hasRole('processcontextjob', 'write')")
    @PutMapping(value = "/{jobId}", consumes = {APPLICATION_YAML_VALUE, APPLICATION_X_YAML_VALUE})
    public ResponseEntity<Void> create(
            @PathVariable UUID jobId,
            @Valid @RequestBody RelationPublicationJobRequest request,
            Authentication authentication) {
        boolean created = relationPublicationJobService.submit(new RelationPublicationJobSubmission(
                jobId, request.relationIds(), MaintenanceJobSubmitterFactory.from(authentication)));
        return created ? ResponseEntity.status(HttpStatus.CREATED).build() : ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get a relation-republication job report.",
            description = "Requires the semantic role processcontextjob:read.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Relation publication job report",
                            content = @Content(
                                    mediaType = APPLICATION_YAML_VALUE,
                                    schema = @Schema(implementation = RelationPublicationJobReport.class),
                                    examples = @ExampleObject(value = """
                                            job-id: 88dbb65f-9634-4685-bc86-17b72d715d3e
                                            job-type: relation-republication
                                            job-state: open
                                            started: 2026-08-06T08:03:12Z
                                            started-by-name: John Doe
                                            started-by-ext-id: "287365"
                                            relations:
                                              - task-id: 019c8c72-6fd1-7f25-a9a1-3b3d51fbb321
                                                relation-id: 019c8c72-7b42-7a04-9443-bf8ec98ce871
                                                state: event-queued
                                            """))),
                    @ApiResponse(responseCode = "403", description = "Access denied"),
                    @ApiResponse(responseCode = "404", description = "Job not found")
            },
            security = {@SecurityRequirement(name = "OIDC_Enduser"), @SecurityRequirement(name = "OIDC_System")})
    @PreAuthorize("hasRole('processcontextjob', 'read')")
    @GetMapping(value = "/{jobId}", produces = {APPLICATION_YAML_VALUE, APPLICATION_X_YAML_VALUE})
    public ResponseEntity<RelationPublicationJobReport> get(@PathVariable UUID jobId) {
        return relationPublicationJobService.get(jobId)
                .map(RelationPublicationJobReport::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
