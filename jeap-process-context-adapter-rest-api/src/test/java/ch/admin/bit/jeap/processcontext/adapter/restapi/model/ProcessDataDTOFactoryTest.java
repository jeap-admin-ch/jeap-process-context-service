package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessDataDTOFactoryTest {

    private ProcessDataRepository processDataRepository;
    private ProcessDataDTOFactory factory;

    @BeforeEach
    void setUp() {
        processDataRepository = mock(ProcessDataRepository.class);
        factory = new ProcessDataDTOFactory(processDataRepository);
    }

    @Test
    void createProcessDataDTOPage_returnsPageOfDTOs() {
        UUID processInstanceId = UUID.randomUUID();
        ProcessData data1 = new ProcessData("key1", "value1");
        ProcessData data2 = new ProcessData("key2", "value2", "roleA");

        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessData> dataPage = new PageImpl<>(List.of(data1, data2), pageable, 2);
        when(processDataRepository.findByProcessInstanceId(eq(processInstanceId), any(Pageable.class)))
                .thenReturn(dataPage);

        Page<ProcessDataDTO> result = factory.createProcessDataDTOPage(processInstanceId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getKey()).isEqualTo("key1");
        assertThat(result.getContent().get(0).getValue()).isEqualTo("value1");
        assertThat(result.getContent().get(1).getKey()).isEqualTo("key2");
        assertThat(result.getContent().get(1).getRole()).isEqualTo("roleA");
    }

    @Test
    void createProcessDataDTOPage_emptyPage_returnsEmptyPage() {
        UUID processInstanceId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(processDataRepository.findByProcessInstanceId(eq(processInstanceId), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        Page<ProcessDataDTO> result = factory.createProcessDataDTOPage(processInstanceId, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void createProcessDataDTOPageFromSnapshot_withData_returnsPageSortedByKeyThenValue() {
        var data1 = ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData.newBuilder()
                .setKey("beta").setValue("value-b").setRole("roleB").build();
        var data2 = ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData.newBuilder()
                .setKey("alpha").setValue("value-a").build();

        ProcessSnapshot snapshot = mock(ProcessSnapshot.class);
        when(snapshot.getProcessData()).thenReturn(List.of(data1, data2));

        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessDataDTO> result = factory.createProcessDataDTOPageFromSnapshot(snapshot, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().getFirst().getKey()).isEqualTo("alpha");
        assertThat(result.getContent().getFirst().getValue()).isEqualTo("value-a");
        assertThat(result.getContent().get(1).getKey()).isEqualTo("beta");
        assertThat(result.getContent().get(1).getRole()).isEqualTo("roleB");
    }

    @Test
    void createProcessDataDTOPageFromSnapshot_nullData_returnsEmptyPage() {
        ProcessSnapshot snapshot = mock(ProcessSnapshot.class);
        when(snapshot.getProcessData()).thenReturn(null);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessDataDTO> result = factory.createProcessDataDTOPageFromSnapshot(snapshot, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void createProcessDataDTOPageFromSnapshot_paginatesCorrectly() {
        var data1 = ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData.newBuilder()
                .setKey("key1").setValue("value1").build();
        var data2 = ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData.newBuilder()
                .setKey("key2").setValue("value2").build();

        ProcessSnapshot snapshot = mock(ProcessSnapshot.class);
        when(snapshot.getProcessData()).thenReturn(List.of(data1, data2));

        Page<ProcessDataDTO> result = factory.createProcessDataDTOPageFromSnapshot(snapshot, PageRequest.of(0, 1));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(1);
    }
}
