package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContextProvider;
import ch.admin.bit.jeap.processcontext.adapter.objectstorage.S3ObjectStorageRepository;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReceiver;
import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import ch.admin.bit.jeap.processcontext.domain.port.MessageConsumerFactory;
import ch.admin.bit.jeap.processcontext.domain.port.ProcessInstanceEventProducer;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import ch.admin.bit.jeap.processcontext.event.test1.SubjectReference;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test1.Test1EventReferences;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.test.processcontext.persistence.DomainWithPersistenceConfig;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.togglz.core.manager.FeatureManager;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;


@SuppressWarnings("unused")
@Slf4j
@DataJpaTest
@ContextConfiguration(classes = DomainWithPersistenceConfig.class)
@TestPropertySource(properties = {
        "logging.level.root=INFO"
        , "logging.level.org.springframework=INFO"
        , "logging.level.ch.admin.bit.jeap.processcontext=INFO"
//        , "logging.level.org.hibernate.SQL=DEBUG"
//        , "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
//        , "spring.jpa.properties.hibernate.format_sql=true"
        , "spring.jpa.properties.hibernate.session_factory.statement_inspector=ch.admin.bit.jeap.processcontext.CountSqlStatementsInterceptor"
        , "spring.jpa.properties.hibernate.generate_statistics=true"
        , "spring.jpa.properties.hibernate.jdbc.batch_size=20"
        , "spring.jpa.properties.hibernate.order_inserts=true"
        , "spring.jpa.properties.hibernate.order_updates=true"
        , "jeap.processcontext.objectstorage.snapshot-bucket=test-bucket"
        , "jeap.processcontext.objectstorage.snapshot-retention-days=1"
})
 // start from scratch to get comparable results
class DomainWithPersistenceIT {

    private static final String PROCESS_TEMPLATE_NAME = "domain-with-persistence-test";
    private static final String PROCESS_DATA_DOSSIER_KEY = "dossierKey";

    @MockitoBean
    private ProcessInstanceEventProducer processInstanceEventProducer;
    @MockitoBean
    private InternalMessageProducer internalMessageProducer;
    @MockitoBean
    private MessageConsumerFactory messageConsumerFactory;
    @MockitoBean
    private RelationListener relationListener;
    @MockitoBean
    private S3ObjectStorageRepository s3ObjectStorageRepository;
    @Autowired
    private ProcessInstanceService processInstanceService;
    @MockitoBean
    private ContractsValidator contractsValidator;
    @MockitoBean
    private FeatureManager featureManager;

    @Autowired
    private MessageReceiver messageReceiver;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    ProcessEventService processEventService;

    @MockitoBean
    TraceContextProvider traceContextProvider;

    @MockitoBean
    TranslateService translateService;

    private Statistics hibernateStatistics;

    @BeforeEach
    void setUp() {
        CountSqlStatementsInterceptor.clearCounts();
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        hibernateStatistics = sessionFactory.getStatistics();
        hibernateStatistics.clear();
    }

    @AfterEach()
    void tearDown() {
        CountSqlStatementsInterceptor.logCounts();
        logHibernateStatistics();
    }

    /*
     * Simulates the situation when the process update can keep up with the event ingestion, i.e. each
     * process outdated event leads to the execution of exactly the one corresponding process update.
     *
     * Measurements:
     *
     * Initial (1.2.2023):
     *
     *   #statements: 717, #selects: 480, #inserts: 194, #updates: 28, #deletes: 15.
     *   Entities: @inserts: 37, #updates: 27, #fetches: 0, #loads: 328, #deletes: 0.
     *   Collections: #updates: 24, #fetches: 342, #loads: 358, #removes: 0, #recreates: 21.
     *   SQL: #queries: 101, #transactions: 79, #preparedStatements: 717.
     *
     *
     * After the following changes to the ProcessInstanceService in order to minimize the number of
     * process instance reads/writes from/to the database (6.2.2023):
     *
     *   1) process the process updates available for a process instance together.
     *   2) replace element collections for relations and process data with one-to-many relationships to entities
     *   3) enabling hibernate batching
     *
     *   #statements: 540, #selects: 458, #inserts: 61, #updates: 21, #deletes: 0.
     *   Entities: @inserts: 58, #updates: 20, #fetches: 0, #loads: 604, #deletes: 0.
     *   Collections: #updates: 24, #fetches: 324, #loads: 340, #removes: 0, #recreates: 21.
     *   SQL: #queries: 92, #transactions: 70, #preparedStatements: 540.
     *
     * After fixing the n+1 problems with event data and related origin task ids on event references (by fetching the
     * event information associated with process instance event references as a whole in three queries) (07.03.2023):
     *
     *   #statements: 367, #selects: 285, #inserts: 61, #updates: 21, #deletes: 0.
     *   Entities: @inserts: 58, #updates: 20, #fetches: 0, #loads: 404, #deletes: 0.
     *   Collections: #updates: 24, #fetches: 97, #loads: 113, #removes: 0, #recreates: 21.
     *   SQL: #queries: 146, #transactions: 70, #preparedStatements: 367.
     */
    @Test
    void simulateLinearTestProcess() {
        String originProcessId = "123";
        assertThat(processInstanceRepository.existsByOriginProcessId(originProcessId)).isFalse();

        // let the PCS start the transactions as needed
        TestTransaction.end();

        createProcessInstance(originProcessId);
        updateAndReact(originProcessId);

        Stream.of("sub1", "sub2", "sub3", "sub4").forEach(subject -> {
            receiveTest1Event(originProcessId, subject);
            updateAndReact(originProcessId);
        });

        Stream.of("obj1", "obj2", "obj3", "obj4").forEach(object -> {
            receiveTest2Event(originProcessId, object);
            updateAndReact(originProcessId);
        });

        // If a change results in additional database statements, the resulting statements
        // should be examined and if necessary the expected count should be adjusted accordingly.
        assertThat(CountSqlStatementsInterceptor.getCountTotal()).isLessThanOrEqualTo(368);
    }

    @Test
        /*
         * Simulates the situation when the process update can't keep up with the event ingestion, i.e. one
         * process outdated event leads to the processing of a lot of pending process updates at once.
         *
         * Measurements:
         *
         * Initial (1.2.2023):
         *
         *   #statements: 461, #selects: 232, #inserts: 194, #updates: 20, #deletes: 15.
         *   Entities: @inserts: 37, #updates: 19, #fetches: 0, #loads: 129, #deletes: 0.
         *   Collections: #updates: 24, #fetches: 142, #loads: 158, #removes: 0, #recreates: 21.
         *   SQL: #queries: 53, #transactions: 47, #preparedStatements: 461.
         *
         * After the following changes to the ProcessInstanceService in order to minimize the number of
         * process instance reads/writes from/to the database (6.2.2023):
         *
         *   1) process the process updates available for a process instance together.
         *   2) replace element collections for relations and process data with one-to-many relationships to entities
         *   3) enabling hibernate batching
         *
         *   #statements: 177, #selects: 132, #inserts: 32, #updates: 13, #deletes: 0.
         *   Entities: @inserts: 58, #updates: 12, #fetches: 0, #loads: 94, #deletes: 0.
         *   Collections: #updates: 3, #fetches: 46, #loads: 62, #removes: 0, #recreates: 21.
         *   SQL: #queries: 44, #transactions: 38, #preparedStatements: 177.
         *
         * After fixing the n+1 problems with event data and related origin task ids on event references (by fetching the
         * event information associated with process instance event references as a whole in three queries) (07.03.2023):
         *
         *   #statements: 148, #selects: 103, #inserts: 32, #updates: 13, #deletes: 0.
         *   Entities: @inserts: 58, #updates: 12, #fetches: 0, #loads: 62, #deletes: 0.
         *   Collections: #updates: 3, #fetches: 11, #loads: 27, #removes: 0, #recreates: 21.
         *   SQL: #queries: 50, #transactions: 38, #preparedStatements: 148.
         */
    void simulateBatchedTestProcess() {
        String originProcessId = "456";
        assertThat(processInstanceRepository.existsByOriginProcessId(originProcessId)).isFalse();

        // let the PCS start the transactions as needed
        TestTransaction.end();

        createProcessInstance(originProcessId);

        Stream.of("sub1", "sub2", "sub3", "sub4").forEach(
                subject -> receiveTest1Event(originProcessId, subject));

        Stream.of("obj1", "obj2", "obj3", "obj4").forEach(object ->
                receiveTest2Event(originProcessId, object));

        processInstanceService.updateProcessState(originProcessId);
        long numPcsStateChangesEvents = mockingDetails(internalMessageProducer).getInvocations().stream()
                .map(InvocationOnMock::getMethod)
                .map(Method::getName)
                .filter(methodName -> methodName.equals("produceProcessContextStateChangedEventSynchronously"))
                .count();
        for (int i = 0; i < numPcsStateChangesEvents; i++) {
            processEventService.reactToProcessStateChange(originProcessId);
        }

        // If a change results in additional database statements, the resulting statements
        // should be examined and if necessary the expected count should be adjusted accordingly.
        assertThat(CountSqlStatementsInterceptor.getCountTotal()).isLessThanOrEqualTo(148);
    }

    private void createProcessInstance(String originProcessId) {
        Set<ProcessData> processData = Set.of(
                new ProcessData("dummy-data-key-1", "dummy-value-1", null),
                new ProcessData("dummy-data-key-2", "dummy-value-2", null),
                new ProcessData("dummy-data-key-3", "dummy-value-3", "dummy-role-3"),
                new ProcessData("dummy-data-key-4", "dummy-value-4", "dummy-role-4"),
                new ProcessData(PROCESS_DATA_DOSSIER_KEY, originProcessId, null));
        processInstanceService.createProcessInstance(originProcessId, PROCESS_TEMPLATE_NAME, processData);
    }

    private void receiveTest1Event(String originProcessId, String subjectId) {
        Test1Event test1Event = Test1EventBuilder.createForProcessId(originProcessId).taskIds().build();
        test1Event.setReferences(Test1EventReferences.newBuilder()
                .setSubjectReference(SubjectReference.newBuilder().setSubjectId(subjectId).build())
                .build());
        messageReceiver.messageReceived(test1Event);
    }

    private void receiveTest2Event(String originProcessId, String objectId) {
        Test2Event test2Event = Test2EventBuilder.createForProcessId(originProcessId)
                .objectId(objectId)
                .build();
        messageReceiver.messageReceived(test2Event);
    }

    private void updateAndReact(String originProcessId) {
        processInstanceService.updateProcessState(originProcessId);
        processEventService.reactToProcessStateChange(originProcessId);
    }

    private void logHibernateStatistics() {
        log.info("Entities: @inserts: {}, #updates: {}, #fetches: {}, #loads: {}, #deletes: {}.",
                hibernateStatistics.getEntityInsertCount(),
                hibernateStatistics.getEntityUpdateCount(),
                hibernateStatistics.getEntityFetchCount(),
                hibernateStatistics.getEntityLoadCount(),
                hibernateStatistics.getEntityDeleteCount());
        log.info("Collections: #updates: {}, #fetches: {}, #loads: {}, #removes: {}, #recreates: {}.",
                hibernateStatistics.getCollectionUpdateCount(),
                hibernateStatistics.getCollectionFetchCount(),
                hibernateStatistics.getCollectionLoadCount(),
                hibernateStatistics.getCollectionRemoveCount(),
                hibernateStatistics.getCollectionRecreateCount());
        log.info("SQL: #queries: {}, #transactions: {}, #preparedStatements: {}.",
                hibernateStatistics.getQueryExecutionCount(),
                hibernateStatistics.getTransactionCount(),
                hibernateStatistics.getPrepareStatementCount());
    }
}
