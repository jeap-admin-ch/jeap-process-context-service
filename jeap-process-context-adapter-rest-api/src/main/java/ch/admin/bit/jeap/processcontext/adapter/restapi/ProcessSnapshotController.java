package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotMetadata;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.SerializedProcessSnapshotArchiveData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/snapshot")
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class ProcessSnapshotController {

    private static final String ARCHIVE_DATA_SYSTEM_HEADER = "Archive-Data-System";
    private static final String ARCHIVE_DATA_SCHEMA_HEADER = "Archive-Data-Schema";
    private static final String ARCHIVE_DATA_SCHEMA_VERSION_HEADER = "Archive-Data-Schema-Version";
    private static final String ARCHIVE_DATA_RETENTION_PERIOD_MONTHS_HEADER = "Archive-Data-Retention-Period-Months";

    // May be empty if the snapshot creation feature is not used bye a PCS instance.
    private final Optional<ProcessSnapshotRepository> processSnapshotRepositoryProvider;

    /**
     * Endpoint providing process snapshots as archive data to a process archive service.
     */
    @GetMapping(value = "/{id}", produces = "avro/binary")
    @Operation(summary = "Provide process snapshot archive data.",
            responses = {@ApiResponse(responseCode = "200", description = "success"),
                    @ApiResponse(responseCode = "404", description = "not found"),
                    @ApiResponse(responseCode = "403", description = "forbidden")})
    @PreAuthorize("hasRole('processsnapshot','view')")
    public ResponseEntity<byte[]> getSnapshotArchiveData(@PathVariable("id") String processOriginId,
                                                         @RequestParam(name = "version", required = false) Integer version) {
        return processSnapshotRepositoryProvider
                .map(repo -> loadSnapshotVersion(processOriginId, version, repo))
                .orElseGet(this::notFound);
    }

    private ResponseEntity<byte[]> loadSnapshotVersion(String processOriginId, Integer version, ProcessSnapshotRepository processSnapshotRepository) {
        return processSnapshotRepository.loadSnapshot(processOriginId, version)
                .map(this::toResponse)
                .orElseGet(this::notFound);
    }

    private ResponseEntity<byte[]> toResponse(SerializedProcessSnapshotArchiveData serializedProcessSnapshotArchiveData) {
        ProcessSnapshotMetadata metadata = serializedProcessSnapshotArchiveData.getMetadata();
        return ResponseEntity.ok()
                .header(ARCHIVE_DATA_SYSTEM_HEADER, metadata.getSystemName())
                .header(ARCHIVE_DATA_SCHEMA_HEADER, metadata.getSchemaName())
                .header(ARCHIVE_DATA_SCHEMA_VERSION_HEADER, Integer.toString(metadata.getSchemaVersion()))
                .header(ARCHIVE_DATA_RETENTION_PERIOD_MONTHS_HEADER, Integer.toString(metadata.getRetentionPeriodMonths()))
                .body(serializedProcessSnapshotArchiveData.getSerializedProcessSnapshot());
    }

    private ResponseEntity<byte[]> notFound() {
        return ResponseEntity.notFound().build();
    }
}
