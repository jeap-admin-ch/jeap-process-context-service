package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.adapter.restapi.model.MessageDTO;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotArchiveData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessSnapshotRepository;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;

@SuppressWarnings("SameParameterValue")
@Slf4j
@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/snapshots.json")
class ProcessSnapshotIT extends ProcessInstanceMockS3ITBase {

    @MockitoBean
    ProcessSnapshotRepository processSnapshotRepository;

    @Captor
    ArgumentCaptor<ProcessSnapshotArchiveData> processSnapshotArchiveDataCaptor;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testCreateSnapshots() {
        // send Test1Event that creates the process
        sendTest1Event("trigger");
        assertProcessInstanceCreated(originProcessId, "snapshots");
        assertMessageCount(originProcessId, "Test1Event", 1);

        // send Test2Event that complete the only task in the process and thus the process and triggers a snapshot on completion
        sendTest2Event("complete");
        assertMessageCount(originProcessId, "Test2Event", 1);

        // Check that process completed
        assertProcessInstanceCompleted(originProcessId);

        // The completion snapshot condition should have been triggered once
        Mockito.verify(processSnapshotRepository, times(1)).
                storeSnapshot(processSnapshotArchiveDataCaptor.capture());
        ProcessSnapshotArchiveData snapshot = processSnapshotArchiveDataCaptor.getAllValues().getFirst();

        // The snapshot should have been created with the correct origin process id and version 1
        assertThat(snapshot.getProcessSnapshot().getOriginProcessId()).isEqualTo(originProcessId);
        assertThat(snapshot.getMetadata().getSnapshotVersion()).isEqualTo(1);

        // Snapshot created events should have been published for the snapshot
        assertSnapshotCreatedEvent(1);
    }

    private void assertMessageCount(String originProcessId, String messageType, long count) {
        Awaitility.await()
                .pollInSameThread()
                .atMost(TIMEOUT)
                .until(() -> countProcessEventsOfType(originProcessId, messageType), is(equalTo(count)));
    }

    private long countProcessEventsOfType(String originProcessId, String type) {
        ProcessInstanceDTO processInstanceDTO = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId);
        return processInstanceDTO.getMessages().stream().
                map(MessageDTO::getName).
                filter(type::equals).
                count();
    }

    private void sendTest1Event(String s) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId)
                .taskIds(s)
                .build();
       sendSync("topic.test1", event1);
    }

    private void sendTest2Event(String s) {
        Test2Event event2 = Test2EventBuilder.createForProcessId(originProcessId)
                .objectId(s)
                .build();
        sendSync("topic.test2", event2);
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
