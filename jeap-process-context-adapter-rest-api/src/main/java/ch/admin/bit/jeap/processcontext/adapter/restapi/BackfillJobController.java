package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.maintenance.BackfillJobEntry;
import ch.admin.bit.jeap.processcontext.domain.maintenance.BackfillJobService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.BackfillJobSubmission;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobSubmitter;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@ConditionalOnProperty(value = "jeap.processcontext.maintenance.enabled", havingValue = "true")
@RestController
@RequestMapping("/api/backfill-jobs")
@Tag(name = "Backfill Jobs", description = "Create and inspect process-data backfill jobs.")
@RequiredArgsConstructor
public class BackfillJobController {

    static final String APPLICATION_YAML_VALUE = "application/yaml";
    static final String APPLICATION_X_YAML_VALUE = "application/x-yaml";

    private final BackfillJobService backfillJobService;

    @Operation(
            summary = "Create a process-data backfill job.",
            description = "Requires the semantic role processcontextjob:write.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = APPLICATION_YAML_VALUE,
                    schema = @Schema(implementation = BackfillJobRequest.class),
                    examples = @ExampleObject(value = """
                            process-template-name: assessmentProcess
                            entries:
                              - origin-process-id: assessment-4711
                                process-data:
                                  - key: assessmentArtefactId
                                    value: art-456
                                    role: FinalVersion
                                  - key: assessmentId
                                    value: a-123
                              - origin-process-id: assessment-4712
                                process-data:
                                  - key: assessmentId
                                    value: a-789
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
            @Valid @RequestBody BackfillJobRequest request,
            Authentication authentication) {
        boolean created = backfillJobService.submit(new BackfillJobSubmission(
                jobId,
                request.processTemplateName(),
                request.entries().stream()
                        .map(entry -> new BackfillJobEntry(
                                entry.originProcessId(),
                                entry.processData().stream()
                                        .map(data -> new ProcessDataValue(data.key(), data.value(), data.role()))
                                        .toList()))
                        .toList(),
                submitter(jwt(authentication))));
        return created ? ResponseEntity.status(HttpStatus.CREATED).build() : ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get a process-data backfill job report.",
            description = "Requires the semantic role processcontextjob:read.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Backfill job report", content = @Content(
                            mediaType = APPLICATION_YAML_VALUE,
                            schema = @Schema(implementation = BackfillJobReport.class),
                            examples = @ExampleObject(value = """
                                    job-id: 88dbb65f-9634-4685-bc86-17b72d715d3e
                                    job-type: process-data-backfill
                                    process-template-name: assessmentProcess
                                    job-state: open
                                    started: 2026-08-06T08:03:12Z
                                    started-by-name: John Doe
                                    started-by-ext-id: "287365"
                                    entries:
                                       - task-id: 019c8c72-6fd1-7f25-a9a1-3b3d51fbb321
                                         origin-process-id: assessment-4711
                                         state: command-queued
                                    """))),
                    @ApiResponse(responseCode = "403", description = "Access denied"),
                    @ApiResponse(responseCode = "404", description = "Job not found")
            },
            security = {@SecurityRequirement(name = "OIDC_Enduser"), @SecurityRequirement(name = "OIDC_System")})
    @PreAuthorize("hasRole('processcontextjob', 'read')")
    @GetMapping(value = "/{jobId}", produces = {APPLICATION_YAML_VALUE, APPLICATION_X_YAML_VALUE})
    public ResponseEntity<BackfillJobReport> get(@PathVariable UUID jobId) {
        return backfillJobService.get(jobId)
                .map(BackfillJobReport::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static MaintenanceJobSubmitter submitter(Jwt jwt) {
        return jwt == null
                ? null
                : new MaintenanceJobSubmitter(jwt.getClaimAsString("name"), jwt.getClaimAsString("ext_id"));
    }

    private static Jwt jwt(Authentication authentication) {
        return authentication instanceof JwtAuthenticationToken jwtAuthenticationToken
                ? jwtAuthenticationToken.getToken()
                : null;
    }
}
