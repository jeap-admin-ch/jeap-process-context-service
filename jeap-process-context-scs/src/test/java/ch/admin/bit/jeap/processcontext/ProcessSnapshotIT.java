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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;

@SuppressWarnings("SameParameterValue")
@Slf4j
class ProcessSnapshotIT extends ProcessInstanceMockS3ITBase {

    @MockitoBean
    ProcessSnapshotRepository processSnapshotRepository;

    @Captor
    ArgumentCaptor<ProcessSnapshotArchiveData> processSnapshotArchiveDataCaptor;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testCreateSnapshots() {
        // Start a new process
        String processTemplateName = "snapshots";
        createProcessInstanceFromTemplate(processTemplateName);

        // send Test1Event that fulfills the programmatic snapshot condition
        sendTest1Event("trigger");
        assertMessageCount(originProcessId, "Test1Event", 1);

        // send Test1Event again that fulfills the programmatic snapshot condition again
        sendTest1Event("no-trigger");
        assertMessageCount(originProcessId, "Test1Event", 2);

        // send Test2Event that complete the only task in the process and thus the process and triggers a snapshot on completion
        sendTest2Event("complete");
        assertMessageCount(originProcessId, "Test2Event", 1);

        // Check that process completed
        assertProcessInstanceCompleted(originProcessId);

        // The programmatic snapshot condition would trigger at least twice but should only register once.
        // The completion snapshot condition would trigger once and should only register once.
        // -> We are expecting only 2 snapshots created and stored in the snapshot repository.
        //    Each of the snapshot conditions should have created one snapshot.
        Mockito.verify(processSnapshotRepository, times(2)).
                storeSnapshot(processSnapshotArchiveDataCaptor.capture());
        ProcessSnapshotArchiveData snapshot1 = processSnapshotArchiveDataCaptor.getAllValues().get(0);
        ProcessSnapshotArchiveData snapshot2 = processSnapshotArchiveDataCaptor.getAllValues().get(1);

        // The snapshots should have been created with the correct origin process id
        assertThat(snapshot1.getProcessSnapshot().getOriginProcessId()).isEqualTo(originProcessId);
        assertThat(snapshot2.getProcessSnapshot().getOriginProcessId()).isEqualTo(originProcessId);

        // The snapshots should have been created in a sequence starting with snapshot version number 1
        assertThat(snapshot1.getMetadata().getSnapshotVersion()).isEqualTo(1);
        assertThat(snapshot2.getMetadata().getSnapshotVersion()).isEqualTo(2);

        // Snapshot created events should have been published for the two snapshot versions
        assertSnapshotCreatedEvents(1, 2);
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
