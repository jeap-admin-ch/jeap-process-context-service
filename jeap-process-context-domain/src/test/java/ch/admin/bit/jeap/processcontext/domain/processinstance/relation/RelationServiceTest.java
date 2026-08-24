package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.StubMetricsListener;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceProperties;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.Relation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationNodeSelector;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationPattern;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private RelationCandidateRepository relationCandidateRepository;

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private ProcessTemplate processTemplate;

    @Captor
    private ArgumentCaptor<Collection<Relation>> savedRelationsCaptor;

    @Captor
    private ArgumentCaptor<Collection<ch.admin.bit.jeap.processcontext.plugin.api.relation.Relation>> notifiedRelationsCaptor;

    private RelationService relationService;
    private MaintenanceProperties maintenanceProperties;

    @BeforeEach
    void setUp() {
        maintenanceProperties = new MaintenanceProperties();
        relationService = new RelationService(relationRepository, relationFactory, relationListener, featureManager,
                new StubMetricsListener(), relationCandidateRepository, maintenanceProperties);
        lenient().when(processInstance.getOriginProcessId()).thenReturn(ORIGIN_PROCESS_ID);
    }

    @Test
    void reevaluateRelations_pagesCandidatesAndNotifiesOnlyNewRelations() {
        RelationPattern pattern = relationPattern();
        RelationCandidate candidateData = candidate("object-1", "subject-1");
        Relation candidate = Relation.builder()
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("subjectType")
                .subjectId("subject-1")
                .objectType("objectType")
                .objectId("object-1")
                .predicateType("predicate")
                .build();
        when(processInstance.getId()).thenReturn(java.util.UUID.randomUUID());
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processTemplate.getRelationPatterns()).thenReturn(List.of(pattern));
        when(relationCandidateRepository.findCandidates(processInstance.getId(), pattern, null, 500))
                .thenReturn(List.of(candidateData), List.of(candidateData));
        when(relationFactory.createRelation(processInstance, pattern, candidateData)).thenReturn(candidate);
        when(relationRepository.saveAllNewRelations(Set.of(candidate))).thenReturn(Set.of(candidate));

        relationService.reevaluateRelations(processInstance);

        verify(relationListener).relationsAdded(notifiedRelationsCaptor.capture());
        assertThat(notifiedRelationsCaptor.getValue()).hasSize(1);
    }

    @Test
    void reevaluateRelations_existingRelationsAreNotNotifiedAgain() {
        RelationPattern pattern = relationPattern();
        RelationCandidate candidateData = candidate("object-1", "subject-1");
        Relation candidate = Relation.builder()
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("subjectType")
                .subjectId("subject-1")
                .objectType("objectType")
                .objectId("object-1")
                .predicateType("predicate")
                .build();
        when(processInstance.getId()).thenReturn(java.util.UUID.randomUUID());
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processTemplate.getRelationPatterns()).thenReturn(List.of(pattern));
        when(relationCandidateRepository.findCandidates(processInstance.getId(), pattern, null, 500))
                .thenReturn(List.of(candidateData), List.of(candidateData));
        when(relationFactory.createRelation(processInstance, pattern, candidateData)).thenReturn(candidate);
        when(relationRepository.saveAllNewRelations(Set.of(candidate))).thenReturn(Set.of());

        relationService.reevaluateRelations(processInstance);

        verifyNoInteractions(relationListener);
    }

    @Test
    void reevaluateRelations_candidateLimitExceeded_failsBeforeCreatingRelations() {
        RelationPattern pattern = relationPattern();
        maintenanceProperties.getLimits().setMaxRelationCandidatesPerTask(1);
        when(processInstance.getId()).thenReturn(java.util.UUID.randomUUID());
        when(processInstance.getProcessTemplate()).thenReturn(processTemplate);
        when(processTemplate.getRelationPatterns()).thenReturn(List.of(pattern));
        when(relationCandidateRepository.findCandidates(processInstance.getId(), pattern, null, 2))
                .thenReturn(List.of(candidate("object-1", "subject-1"), candidate("object-2", "subject-2")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> relationService.reevaluateRelations(processInstance))
                .isInstanceOf(RelationCandidateLimitExceededException.class);

        verifyNoInteractions(relationFactory, relationRepository, relationListener);
    }

    @Test
    void onNewProcessData_emptyRelations_savesNothingAndNotifiesWithEmptyList() {
        List<ProcessData> newProcessData = List.of(new ProcessData("key", "value"));
        when(relationFactory.createNewRelations(processInstance, newProcessData)).thenReturn(Set.of());
        when(relationRepository.saveAllNewRelations(any())).thenReturn(Set.of());

        relationService.onNewProcessData(processInstance, newProcessData);

        verify(relationRepository).saveAllNewRelations(savedRelationsCaptor.capture());
        assertThat(savedRelationsCaptor.getValue()).isEmpty();

        verify(relationListener, never()).relationsAdded(any());
    }

    @Test
    void onNewProcessData_withRelations_callsOnPrePersistAndSavesAndNotifies() {
        List<ProcessData> newProcessData = List.of(new ProcessData("key", "value"));
        Relation relation = Relation.builder()
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("subjectType")
                .subjectId("subjectId")
                .objectType("objectType")
                .objectId("objectId")
                .predicateType("predicateType")
                .build();

        when(relationFactory.createNewRelations(processInstance, newProcessData)).thenReturn(Set.of(relation));
        when(relationRepository.saveAllNewRelations(any())).thenAnswer(invocation -> invocation.getArgument(0));

        relationService.onNewProcessData(processInstance, newProcessData);

        // Verify onPrePersist was called (relation should now have idempotenceId and createdAt)
        assertThat(relation.getIdempotenceId()).isNotNull();
        assertThat(relation.getCreatedAt()).isNotNull();

        verify(relationRepository).saveAllNewRelations(savedRelationsCaptor.capture());
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
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("subjectType")
                .subjectId("subjectId")
                .objectType("objectType")
                .objectId("objectId")
                .predicateType("predicateType")
                .featureFlag(null)
                .build();

        when(relationFactory.createNewRelations(processInstance, newProcessData)).thenReturn(Set.of(relation));
        when(relationRepository.saveAllNewRelations(any())).thenAnswer(invocation -> invocation.getArgument(0));

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
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("subjectType")
                .subjectId("subjectId")
                .objectType("objectType")
                .objectId("objectId")
                .predicateType("predicateType")
                .featureFlag("MY_FEATURE")
                .build();

        when(relationFactory.createNewRelations(processInstance, newProcessData)).thenReturn(Set.of(relation));
        when(relationRepository.saveAllNewRelations(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("subjectType")
                .subjectId("subjectId")
                .objectType("objectType")
                .objectId("objectId")
                .predicateType("predicateType")
                .featureFlag("MY_FEATURE")
                .build();

        when(relationFactory.createNewRelations(processInstance, newProcessData)).thenReturn(Set.of(relation));
        when(relationRepository.saveAllNewRelations(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("subjectType1")
                .subjectId("subjectId1")
                .objectType("objectType1")
                .objectId("objectId1")
                .predicateType("predicateType")
                .featureFlag(null)
                .build();

        Relation relationActiveFlag = Relation.builder()
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("subjectType2")
                .subjectId("subjectId2")
                .objectType("objectType2")
                .objectId("objectId2")
                .predicateType("predicateType")
                .featureFlag("ACTIVE_FEATURE")
                .build();

        Relation relationInactiveFlag = Relation.builder()
                .processInstance(processInstance)
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
        when(relationRepository.saveAllNewRelations(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(featureManager.isActive(any(NamedFeature.class))).thenAnswer(invocation -> {
            NamedFeature feature = invocation.getArgument(0);
            return "ACTIVE_FEATURE".equals(feature.name());
        });

        relationService.onNewProcessData(processInstance, newProcessData);

        // All relations should be saved
        verify(relationRepository).saveAllNewRelations(savedRelationsCaptor.capture());
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
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("subjectType1")
                .subjectId("subjectId1")
                .objectType("objectType1")
                .objectId("objectId1")
                .predicateType("predicateType1")
                .build();

        Relation relation2 = Relation.builder()
                .processInstance(processInstance)
                .systemId("system")
                .subjectType("subjectType2")
                .subjectId("subjectId2")
                .objectType("objectType2")
                .objectId("objectId2")
                .predicateType("predicateType2")
                .build();

        when(relationFactory.createNewRelations(processInstance, newProcessData))
                .thenReturn(Set.of(relation1, relation2));
        when(relationRepository.saveAllNewRelations(any())).thenAnswer(invocation -> invocation.getArgument(0));

        relationService.onNewProcessData(processInstance, newProcessData);

        verify(relationRepository).saveAllNewRelations(savedRelationsCaptor.capture());
        assertThat(savedRelationsCaptor.getValue()).hasSize(2);

        verify(relationListener).relationsAdded(notifiedRelationsCaptor.capture());
        assertThat(notifiedRelationsCaptor.getValue()).hasSize(2);
    }

    @Test
    void republishRelation_notifiesExactlyOnceWithoutMutatingOrSaving() {
        Relation relation = createPersistedRelation(null);
        var idempotenceId = relation.getIdempotenceId();
        var createdAt = relation.getCreatedAt();

        relationService.republishRelation(relation);

        verify(relationListener).relationsAdded(notifiedRelationsCaptor.capture());
        assertThat(notifiedRelationsCaptor.getValue()).singleElement().satisfies(apiRelation -> {
            assertThat(apiRelation.getOriginProcessId()).isEqualTo(ORIGIN_PROCESS_ID);
            assertThat(apiRelation.getIdempotenceId()).isEqualTo(idempotenceId);
            assertThat(apiRelation.getCreatedAt()).isEqualTo(createdAt);
        });
        assertThat(relation.getIdempotenceId()).isEqualTo(idempotenceId);
        assertThat(relation.getCreatedAt()).isEqualTo(createdAt);
        verifyNoInteractions(relationRepository);
    }

    @Test
    void republishRelation_inactiveFeatureFlagThrowsClearException() {
        Relation relation = createPersistedRelation("DISABLED");
        when(featureManager.isActive(any(NamedFeature.class))).thenReturn(false);

        assertThatThrownBy(() -> relationService.republishRelation(relation))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DISABLED").hasMessageContaining("inactive");
        verifyNoInteractions(relationListener, relationRepository);
    }

    @Test
    void republishRelation_listenerExceptionPropagates() {
        Relation relation = createPersistedRelation(null);
        doThrow(new IllegalStateException("listener failed")).when(relationListener).relationsAdded(any());

        assertThatThrownBy(() -> relationService.republishRelation(relation))
                .isInstanceOf(IllegalStateException.class).hasMessage("listener failed");
    }

    private Relation createPersistedRelation(String featureFlag) {
        Relation relation = Relation.builder().processInstance(processInstance).systemId("system")
                .subjectType("subject").subjectId("subject-1").objectType("object").objectId("object-1")
                .predicateType("predicate").featureFlag(featureFlag).build();
        relation.onPrePersist();
        return relation;
    }

    private static RelationCandidate candidate(String objectValue, String subjectValue) {
        return new RelationCandidate(java.util.UUID.randomUUID(), objectValue,
                java.util.UUID.randomUUID(), subjectValue);
    }

    private static RelationPattern relationPattern() {
        return RelationPattern.builder()
                .predicateType("predicate")
                .objectSelector(RelationNodeSelector.builder()
                        .type("objectType")
                        .processDataKey("object")
                        .build())
                .subjectSelector(RelationNodeSelector.builder()
                        .type("subjectType")
                        .processDataKey("subject")
                        .build())
                .build();
    }
}
