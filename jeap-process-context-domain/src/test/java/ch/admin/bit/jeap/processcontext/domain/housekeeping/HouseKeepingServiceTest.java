package ch.admin.bit.jeap.processcontext.domain.housekeeping;

import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceQueryResult;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateRepository;
import com.fasterxml.uuid.Generators;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HouseKeepingServiceTest {

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Mock
    private ProcessUpdateRepository processUpdateRepository;

    @Mock
    private ProcessEventRepository processEventRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private HouseKeepingConfigProperties houseKeepingConfigProperties;

    @Mock
    private PlatformTransactionManager transactionManager;

    private HouseKeepingService houseKeepingService;

    @Captor
    private ArgumentCaptor<Set<UUID>> uuidCaptor;

    @Captor
    private ArgumentCaptor<Set<String>> processUpdateDeletionCaptor;

    @Captor
    private ArgumentCaptor<Set<String>> processEventDeletionCaptor;

    @BeforeEach
    void beforeEach() {
        houseKeepingConfigProperties = new HouseKeepingConfigProperties();
        houseKeepingConfigProperties.setPageSize(5);
        houseKeepingConfigProperties.setMaxPages(100);

        houseKeepingService = new HouseKeepingService(processInstanceRepository, processUpdateRepository, processEventRepository,
                messageRepository, houseKeepingConfigProperties, transactionManager);
    }

    @Test
    void deleteEventsWithoutProcessCorrelation_eventsFound_eventsDeleted(@Mock Slice<UUID> firstPage,
                                                                         @Mock Slice<UUID> secondPage) {
        Set<UUID> firstSet = Set.of(Generators.timeBasedEpochGenerator().generate());
        when(firstPage.toSet()).thenReturn(firstSet);
        when(firstPage.hasNext()).thenReturn(true);

        Set<UUID> secondSet = Set.of(Generators.timeBasedEpochGenerator().generate());
        when(secondPage.toSet()).thenReturn(secondSet);
        when(secondPage.hasNext()).thenReturn(false);

        when(messageRepository.findMessagesWithoutProcessCorrelation(any(ZonedDateTime.class), any(Pageable.class)))
                .thenReturn(firstPage).thenReturn(secondPage);

        houseKeepingService.deleteMessagesWithoutProcessCorrelation(Duration.of(90, ChronoUnit.DAYS));

        verify(messageRepository, times(2)).findMessagesWithoutProcessCorrelation(any(ZonedDateTime.class), any(Pageable.class));
        verify(messageRepository, times(2)).deleteMessageDataByMessageIds(uuidCaptor.capture());
        verify(messageRepository, times(2)).deleteMessageUserDataByMessageIds(uuidCaptor.capture());
        verify(messageRepository, times(2)).deleteOriginTaskIdByMessageIds(uuidCaptor.capture());
        verify(messageRepository, times(2)).deleteMessageByIds(uuidCaptor.capture());

        final List<Set<UUID>> uuidCaptorAllValues = uuidCaptor.getAllValues();

        assertThat(uuidCaptorAllValues.get(0)).isEqualTo(firstSet);
        assertThat(uuidCaptorAllValues.get(1)).isEqualTo(secondSet);
    }

    @Test
    void deletePaged_stopAtMaxPages(@Mock Slice<UUID> firstPage, @Mock Slice<UUID> secondPage) {
        // Stop after two pages
        int maxPages = 2;
        houseKeepingConfigProperties.setMaxPages(maxPages);

        Set<UUID> firstSet = Set.of(Generators.timeBasedEpochGenerator().generate());
        when(firstPage.toSet()).thenReturn(firstSet);
        when(firstPage.hasNext()).thenReturn(true);

        Set<UUID> secondSet = Set.of(Generators.timeBasedEpochGenerator().generate());
        when(secondPage.toSet()).thenReturn(secondSet);
        when(secondPage.hasNext()).thenReturn(true); // true = more pages present, but stop here due to maxPages reached

        when(messageRepository.findMessagesWithoutProcessCorrelation(any(ZonedDateTime.class), any(Pageable.class)))
                .thenReturn(firstPage).thenReturn(secondPage);

        houseKeepingService.deleteMessagesWithoutProcessCorrelation(Duration.of(90, ChronoUnit.DAYS));

        verify(messageRepository, times(maxPages)).deleteMessageByIds(uuidCaptor.capture());
    }

    @Test
    void deleteProcessInstances_processInstancesFound_processInstancesDeleted() {
        @SuppressWarnings("unchecked")
        Slice<ProcessInstanceQueryResult> firstPage = mock(Slice.class);
        when(firstPage.hasNext()).thenReturn(true);
        Set<ProcessInstanceQueryResult> firstSet = Set.of(ProcessInstanceQueryResultImpl.builder().id(Generators.timeBasedEpochGenerator().generate()).originProcessId("1").build());
        when(firstPage.toSet()).thenReturn(firstSet);

        @SuppressWarnings("unchecked")
        Slice<ProcessInstanceQueryResult> secondPage = mock(Slice.class);
        when(secondPage.hasNext()).thenReturn(false);
        Set<ProcessInstanceQueryResult> secondSet = Set.of(ProcessInstanceQueryResultImpl.builder().id(Generators.timeBasedEpochGenerator().generate()).originProcessId("2").build());
        when(secondPage.toSet()).thenReturn(secondSet);

        when(processInstanceRepository.findProcessInstances(any(ProcessState.class), any(ZonedDateTime.class), any(Pageable.class)))
                .thenReturn(firstPage).thenReturn(secondPage);

        houseKeepingService.deleteProcessInstances(ProcessState.COMPLETED, Duration.of(90, ChronoUnit.DAYS));

        verify(processInstanceRepository, times(2)).findProcessInstances(any(ProcessState.class), any(ZonedDateTime.class), any(Pageable.class));
        verify(processInstanceRepository, times(2)).deleteAllById(uuidCaptor.capture());
        verify(processUpdateRepository, times(2)).deleteAllByOriginProcessIdIn(processUpdateDeletionCaptor.capture());
        verify(processEventRepository, times(2)).deleteAllByOriginProcessIdIn(processEventDeletionCaptor.capture());

        final List<Set<UUID>> uuidCaptorAllValues = uuidCaptor.getAllValues();
        assertThat(uuidCaptorAllValues.get(0)).isEqualTo(firstSet.stream().map(ProcessInstanceQueryResult::getId).collect(Collectors.toSet()));
        assertThat(uuidCaptorAllValues.get(1)).isEqualTo(secondSet.stream().map(ProcessInstanceQueryResult::getId).collect(Collectors.toSet()));

        final List<Set<String>> processUpdateDeletionCaptorAllValues = processUpdateDeletionCaptor.getAllValues();
        assertThat(processUpdateDeletionCaptorAllValues.get(0)).isEqualTo(firstSet.stream().map(ProcessInstanceQueryResult::getOriginProcessId).collect(Collectors.toSet()));
        assertThat(processUpdateDeletionCaptorAllValues.get(1)).isEqualTo(secondSet.stream().map(ProcessInstanceQueryResult::getOriginProcessId).collect(Collectors.toSet()));

        final List<Set<String>> processEventDeletionCaptorAllValues = processEventDeletionCaptor.getAllValues();
        assertThat(processEventDeletionCaptorAllValues.get(0)).isEqualTo(firstSet.stream().map(ProcessInstanceQueryResult::getOriginProcessId).collect(Collectors.toSet()));
        assertThat(processEventDeletionCaptorAllValues.get(1)).isEqualTo(secondSet.stream().map(ProcessInstanceQueryResult::getOriginProcessId).collect(Collectors.toSet()));

    }

    @RequiredArgsConstructor
    @Builder
    private static class ProcessInstanceQueryResultImpl implements ProcessInstanceQueryResult {
        private final UUID id;
        private final String originProcessId;

        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public String getOriginProcessId() {
            return originProcessId;
        }
    }

}
