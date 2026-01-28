package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.util.NamedFeature;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelationServiceTest {

    private static final String ORIGIN_PROCESS_ID = "test-process-id";

    @Mock
    private RelationRepository relationRepository;

    @Mock
    private RelationFactory relationFactory;

    @Mock
    private RelationListener relationListener;

    @Mock
    private FeatureManager featureManager;

    @Mock
    private ProcessInstance processInstance;

    @Captor
    private ArgumentCaptor<Collection<Relation>> savedRelationsCaptor;

    @Captor
    private ArgumentCaptor<Collection<ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation>> notifiedRelationsCaptor;

    private RelationService relationService;

    @BeforeEach
    void setUp() {
        relationService = new RelationService(relationRepository, relationFactory, relationListener, featureManager);
        lenient().when(processInstance.getOriginProcessId()).thenReturn(ORIGIN_PROCESS_ID);
    }

    @Test
    void onNewProcessData_emptyRelations_savesNothingAndNotifiesWithEmptyList() {
        List<ProcessData> newProcessData = List.of(new ProcessData("key", "value"));
        when(relationFactory.createNewRelations(processInstance, newProcessData)).thenReturn(Set.of());

        relationService.onNewProcessData(processInstance, newProcessData);

        verify(relationRepository).saveAll(savedRelationsCaptor.capture());
        assertThat(savedRelationsCaptor.getValue()).isEmpty();

        verify(relationListener).relationsAdded(notifiedRelationsCaptor.capture());
        assertThat(notifiedRelationsCaptor.getValue()).isEmpty();
    }

    @Test
    void onNewProcessData_withRelations_callsOnPrePersistAndSavesAndNotifies() {
        List<ProcessData> newProcessData = List.of(new ProcessData("key", "value"));
        Relation relation = Relation.builder()
                .systemId("system")
                .subjectType("subjectType")
                .subjectId("subjectId")
                .objectType("objectType")
                .objectId("objectId")
                .predicateType("predicateType")
                .build();

        when(relationFactory.createNewRelations(processInstance, newProcessData)).thenReturn(Set.of(relation));

        relationService.onNewProcessData(processInstance, newProcessData);

        // Verify onPrePersist was called (relation should now have idempotenceId and createdAt)
        assertThat(relation.getIdempotenceId()).isNotNull();
        assertThat(relation.getCreatedAt()).isNotNull();

        verify(relationRepository).saveAll(savedRelationsCaptor.capture());
        assertThat(savedRelationsCaptor.getValue()).containsExactly(relation);

        verify(relationListener).relationsAdded(notifiedRelationsCaptor.capture());
        assertThat(notifiedRelationsCaptor.getValue()).hasSize(1);

        var apiRelation = notifiedRelationsCaptor.getValue().iterator().next();
        assertThat(apiRelation.getOriginProcessId()).isEqualTo(ORIGIN_PROCESS_ID);
        assertThat(apiRelation.getSystemId()).isEqualTo("system");
        assertThat(apiRelation.getSubjectType()).isEqualTo("subjectType");
        assertThat(apiRelation.getSubjectId()).isEqualTo("subjectId");
        assertThat(apiRelation.getObjectType()).isEqualTo("objectType");
        assertThat(apiRelation.getObjectId()).isEqualTo("objectId");
        assertThat(apiRelation.getPredicateType()).isEqualTo("predicateType");
    }

    @Test
    void onNewProcessData_relationWithoutFeatureFlag_isNotified() {
        List<ProcessData> newProcessData = List.of(new ProcessData("key", "value"));
        Relation relation = Relation.builder()
                .systemId("system")
                .subjectType("subjectType")
                .subjectId("subjectId")
                .objectType("objectType")
                .objectId("objectId")
                .predicateType("predicateType")
                .featureFlag(null)
                .build();

        when(relationFactory.createNewRelations(processInstance, newProcessData)).thenReturn(Set.of(relation));

        relationService.onNewProcessData(processInstance, newProcessData);

        verify(relationListener).relationsAdded(notifiedRelationsCaptor.capture());
        assertThat(notifiedRelationsCaptor.getValue()).hasSize(1);
        // No feature flag check should occur
        verifyNoInteractions(featureManager);
    }

    @Test
    void onNewProcessData_relationWithActiveFeatureFlag_isNotified() {
        List<ProcessData> newProcessData = List.of(new ProcessData("key", "value"));
        Relation relation = Relation.builder()
                .systemId("system")
                .subjectType("subjectType")
                .subjectId("subjectId")
                .objectType("objectType")
                .objectId("objectId")
                .predicateType("predicateType")
                .featureFlag("MY_FEATURE")
                .build();

        when(relationFactory.createNewRelations(processInstance, newProcessData)).thenReturn(Set.of(relation));
        when(featureManager.isActive(any(NamedFeature.class))).thenReturn(true);

        relationService.onNewProcessData(processInstance, newProcessData);

        verify(featureManager).isActive(argThat(feature -> "MY_FEATURE".equals(feature.name())));
        verify(relationListener).relationsAdded(notifiedRelationsCaptor.capture());
        assertThat(notifiedRelationsCaptor.getValue()).hasSize(1);
    }

    @Test
    void onNewProcessData_relationWithInactiveFeatureFlag_isNotNotified() {
        List<ProcessData> newProcessData = List.of(new ProcessData("key", "value"));
        Relation relation = Relation.builder()
                .systemId("system")
                .subjectType("subjectType")
                .subjectId("subjectId")
                .objectType("objectType")
                .objectId("objectId")
                .predicateType("predicateType")
                .featureFlag("MY_FEATURE")
                .build();

        when(relationFactory.createNewRelations(processInstance, newProcessData)).thenReturn(Set.of(relation));
        when(featureManager.isActive(any(NamedFeature.class))).thenReturn(false);

        relationService.onNewProcessData(processInstance, newProcessData);

        verify(featureManager).isActive(argThat(feature -> "MY_FEATURE".equals(feature.name())));
        verify(relationListener).relationsAdded(notifiedRelationsCaptor.capture());
        assertThat(notifiedRelationsCaptor.getValue()).isEmpty();
    }

    @Test
    void onNewProcessData_mixedFeatureFlags_onlyNotifiesActiveOrNoFlag() {
        List<ProcessData> newProcessData = List.of(new ProcessData("key", "value"));

        Relation relationNoFlag = Relation.builder()
                .systemId("system")
                .subjectType("subjectType1")
                .subjectId("subjectId1")
                .objectType("objectType1")
                .objectId("objectId1")
                .predicateType("predicateType")
                .featureFlag(null)
                .build();

        Relation relationActiveFlag = Relation.builder()
                .systemId("system")
                .subjectType("subjectType2")
                .subjectId("subjectId2")
                .objectType("objectType2")
                .objectId("objectId2")
                .predicateType("predicateType")
                .featureFlag("ACTIVE_FEATURE")
                .build();

        Relation relationInactiveFlag = Relation.builder()
                .systemId("system")
                .subjectType("subjectType3")
                .subjectId("subjectId3")
                .objectType("objectType3")
                .objectId("objectId3")
                .predicateType("predicateType")
                .featureFlag("INACTIVE_FEATURE")
                .build();

        when(relationFactory.createNewRelations(processInstance, newProcessData))
                .thenReturn(Set.of(relationNoFlag, relationActiveFlag, relationInactiveFlag));
        when(featureManager.isActive(any(NamedFeature.class))).thenAnswer(invocation -> {
            NamedFeature feature = invocation.getArgument(0);
            return "ACTIVE_FEATURE".equals(feature.name());
        });

        relationService.onNewProcessData(processInstance, newProcessData);

        // All relations should be saved
        verify(relationRepository).saveAll(savedRelationsCaptor.capture());
        assertThat(savedRelationsCaptor.getValue()).hasSize(3);

        // Only relations without flag or with active flag should be notified
        verify(relationListener).relationsAdded(notifiedRelationsCaptor.capture());
        assertThat(notifiedRelationsCaptor.getValue()).hasSize(2);
        assertThat(notifiedRelationsCaptor.getValue())
                .extracting(ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation::getSubjectId)
                .containsExactlyInAnyOrder("subjectId1", "subjectId2");
    }

    @Test
    void onNewProcessData_multipleRelations_allAreSavedAndNotified() {
        List<ProcessData> newProcessData = List.of(new ProcessData("key", "value"));

        Relation relation1 = Relation.builder()
                .systemId("system")
                .subjectType("subjectType1")
                .subjectId("subjectId1")
                .objectType("objectType1")
                .objectId("objectId1")
                .predicateType("predicateType1")
                .build();

        Relation relation2 = Relation.builder()
                .systemId("system")
                .subjectType("subjectType2")
                .subjectId("subjectId2")
                .objectType("objectType2")
                .objectId("objectId2")
                .predicateType("predicateType2")
                .build();

        when(relationFactory.createNewRelations(processInstance, newProcessData))
                .thenReturn(Set.of(relation1, relation2));

        relationService.onNewProcessData(processInstance, newProcessData);

        verify(relationRepository).saveAll(savedRelationsCaptor.capture());
        assertThat(savedRelationsCaptor.getValue()).hasSize(2);

        verify(relationListener).relationsAdded(notifiedRelationsCaptor.capture());
        assertThat(notifiedRelationsCaptor.getValue()).hasSize(2);
    }
}
