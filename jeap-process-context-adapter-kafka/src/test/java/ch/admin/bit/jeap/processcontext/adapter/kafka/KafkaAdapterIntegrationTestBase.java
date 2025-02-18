package ch.admin.bit.jeap.processcontext.adapter.kafka;

import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processcontext.adapter.test.kafka.config.TestApp;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("local")
@SpringBootTest(
        properties = {
                "jeap.processcontext.kafka.topic.process-changed-internal=changed",
                "jeap.processcontext.kafka.topic.process-outdated-internal=outdated",
                "jeap.processcontext.kafka.topic.process-instance-created=process-instance-created",
                "jeap.processcontext.kafka.topic.process-instance-completed=process-instance-completed",
                "jeap.processcontext.kafka.topic.process-milestone-reached=process-milestone-reached",
                "jeap.processcontext.kafka.topic.process-snapshot-created=process-snapshot-created",
                "jeap.processcontext.kafka.topic.create-process-instance=create-process-instance",
                "jeap.messaging.kafka.error-topic-name=error",
                "jeap.messaging.kafka.system-name=test",
                "jeap.messaging.kafka.service-name=test"
        },
        classes = TestApp.class)
@ExtendWith(MockitoExtension.class)
public abstract class KafkaAdapterIntegrationTestBase extends KafkaIntegrationTestBase {
    @MockitoBean
    protected ProcessInstanceService processInstanceService;
    @MockitoBean
    protected ProcessEventService processEventService;
    @MockitoBean
    protected ProcessTemplateRepository processTemplateRepository;
}
