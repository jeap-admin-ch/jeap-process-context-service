package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationsService;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("unused")
@WebMvcTest(controllers = ProcessSnapshotController.class)
@ContextConfiguration(classes = RestApiTestContext.class)
@AutoConfigureMockMvc
class ProcessSnapshotControllerTest {

    private static final String ARCHIVE_DATA_SYSTEM_HEADER = "Archive-Data-System";
    private static final String ARCHIVE_DATA_SCHEMA_HEADER = "Archive-Data-Schema";
    private static final String ARCHIVE_DATA_SCHEMA_VERSION_HEADER = "Archive-Data-Schema-Version";
    private static final String ARCHIVE_DATA_RETENTION_PERIOD_MONTHS_HEADER = "Archive-Data-Retention-Period-Months";
    private static final String AVRO_BINARY_MEDIA_TYPE_STRING = "avro/binary";

    private static final String SNAPSHOT_ID = "test-snapshot-id";
    private static final int SNAPSHOT_VERSION = 1;
    private static final byte[] SNAPSHOT_BINARY_DATA = "some-data".getBytes();
    private static final String SNAPSHOT_SCHEMA = "test-snapshot-schema";
    private static final String SNAPSHOT_SCHEMA_VERSION = "2";
    private static final String SNAPSHOT_SYSTEM = "test-system";
    private static final int SNAPSHOT_RETENTION_MONTHS = 60;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProcessSnapshotRepository processSnapshotRepository;
    @MockitoBean
    private ProcessInstanceQueryRepository processInstanceQueryRepository;
    @MockitoBean
    private ProcessInstanceService processInstanceService;
    @MockitoBean
    private ProcessRelationsService processRelationsService;
    @MockitoBean
    private MessageRepository messageRepository;

    private static final SemanticApplicationRole SNAPSHOT_VIEW_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("processsnapshot")
            .operation("view")
            .build();

    private static final SemanticApplicationRole SNAPSHOT_OTHER_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("processsnapshot")
            .operation("other")
            .build();

    @Test
    void testGetProcessSnapshot_whenExistingIdAndExistingVersionProvided_thenReturnAvroBinaryAndMetaData() throws Exception {
        final SerializedProcessSnapshotArchiveData snapshot = createSerializedProcessSnapshotArchiveData();
        when(processSnapshotRepository.loadSnapshot(SNAPSHOT_ID, snapshot.getMetadata().getSnapshotVersion()))
                .thenReturn(Optional.of(snapshot));

        mockMvc.perform(get("/api/snapshot/{id}?version={version}", SNAPSHOT_ID, snapshot.getMetadata().getSnapshotVersion())
                        .with(authentication(createAuthenticationForUserRoles(SNAPSHOT_VIEW_ROLE)))
                        .accept(AVRO_BINARY_MEDIA_TYPE_STRING))
                .andExpect(status().isOk())
                .andExpect(content().contentType(AVRO_BINARY_MEDIA_TYPE_STRING))
                .andExpect(header().string(ARCHIVE_DATA_SCHEMA_HEADER, SNAPSHOT_SCHEMA))
                .andExpect(header().string(ARCHIVE_DATA_SCHEMA_VERSION_HEADER, SNAPSHOT_SCHEMA_VERSION))
                .andExpect(header().string(ARCHIVE_DATA_SYSTEM_HEADER, SNAPSHOT_SYSTEM))
                .andExpect(header().string(ARCHIVE_DATA_RETENTION_PERIOD_MONTHS_HEADER, Integer.toString(SNAPSHOT_RETENTION_MONTHS)))
                .andExpect(content().bytes(SNAPSHOT_BINARY_DATA));
    }

    @Test
    void testGetProcessSnapshot_whenExistingIdAndNoVersionProvided_thenReturnAvroBinaryAndMetaDataForLatestSnapshotVersion() throws Exception {
        final SerializedProcessSnapshotArchiveData snapshot = createSerializedProcessSnapshotArchiveData();
        when(processSnapshotRepository.loadSnapshot(SNAPSHOT_ID, null))
                .thenReturn(Optional.of(snapshot));

        mockMvc.perform(get("/api/snapshot/{id}", SNAPSHOT_ID)
                        .with(authentication(createAuthenticationForUserRoles(SNAPSHOT_VIEW_ROLE)))
                        .accept(AVRO_BINARY_MEDIA_TYPE_STRING))
                .andExpect(status().isOk())
                .andExpect(content().contentType(AVRO_BINARY_MEDIA_TYPE_STRING))
                .andExpect(header().string(ARCHIVE_DATA_SCHEMA_HEADER, SNAPSHOT_SCHEMA))
                .andExpect(header().string(ARCHIVE_DATA_SCHEMA_VERSION_HEADER, SNAPSHOT_SCHEMA_VERSION))
                .andExpect(header().string(ARCHIVE_DATA_SYSTEM_HEADER, SNAPSHOT_SYSTEM))
                .andExpect(header().string(ARCHIVE_DATA_RETENTION_PERIOD_MONTHS_HEADER, Integer.toString(SNAPSHOT_RETENTION_MONTHS)))
                .andExpect(content().bytes(SNAPSHOT_BINARY_DATA));
    }

    @Test
    void testGetProcessSnapshot_whenNonExistingIdProvided_thenReturnNotFound() throws Exception {
        final String nonExistingId = "non-existing-test-id";
        when(processSnapshotRepository.loadSnapshot(nonExistingId, 1))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/snapshot/{id}?version={version}", nonExistingId, 1)
                        .with(authentication(createAuthenticationForUserRoles(SNAPSHOT_VIEW_ROLE)))
                        .accept(AVRO_BINARY_MEDIA_TYPE_STRING))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetProcessSnapshot_whenMissingReadRole_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/snapshot/{id}", SNAPSHOT_ID)
                        .with(authentication(createAuthenticationForUserRoles(SNAPSHOT_OTHER_ROLE)))
                        .accept(AVRO_BINARY_MEDIA_TYPE_STRING))
                .andExpect(status().isForbidden());
        Mockito.verifyNoInteractions(processSnapshotRepository);
    }


    private static SerializedProcessSnapshotArchiveData createSerializedProcessSnapshotArchiveData() {
        ProcessSnapshotMetadata metadata = ProcessSnapshotMetadata.builder()
                .snapshotVersion(SNAPSHOT_VERSION)
                .schemaName(SNAPSHOT_SCHEMA)
                .schemaVersion(Integer.parseInt(SNAPSHOT_SCHEMA_VERSION))
                .systemName(SNAPSHOT_SYSTEM)
                .retentionPeriodMonths(SNAPSHOT_RETENTION_MONTHS)
                .build();
        return new SerializedProcessSnapshotArchiveData(SNAPSHOT_BINARY_DATA, metadata);
    }

    private static JeapAuthenticationToken createAuthenticationForUserRoles(SemanticApplicationRole... userroles) {
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(userroles).build();
    }

}
