package ch.admin.bit.jeap.processcontext.domain.processinstance.relation;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationNodeSelector;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.RelationPattern;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationListener;
import ch.admin.bit.jeap.processcontext.plugin.api.relation.RelationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelationTypeDeclarationNotifierTest {

    private static final String SYSTEM_A = "ch.test.SystemA";
    private static final String SYSTEM_B = "ch.test.SystemB";

    @Mock
    private ProcessTemplateRepository processTemplateRepository;
    @Mock
    private RelationListener relationListener;
    @InjectMocks
    private RelationTypeDeclarationNotifier notifier;
    @Captor
    private ArgumentCaptor<Set<RelationType>> relationTypesCaptor;

    @Test
    void afterSingletonsInstantiated_reportsDistinctRelationTypesOfAllTemplatesIncludingFeatureFlags() {
        ProcessTemplate template1 = templateWithPatterns("a-template", SYSTEM_A,
                pattern("ch.test.predicate.Knows", "ch.test.Subject", "ch.test.Object", null),
                pattern("ch.test.predicate.Owns", "ch.test.Subject", "ch.test.Other", "MY_FLAG"));
        ProcessTemplate template2 = templateWithPatterns("b-template", SYSTEM_A,
                pattern("ch.test.predicate.Knows", "ch.test.Subject", "ch.test.Object", null));
        // Same pattern in a template of another system: a distinct relation type
        ProcessTemplate template3 = templateWithPatterns("c-template", SYSTEM_B,
                pattern("ch.test.predicate.Knows", "ch.test.Subject", "ch.test.Object", null));
        // Returned in no particular order, reported ordered by template name
        when(processTemplateRepository.getAllTemplates()).thenReturn(List.of(template3, template1, template2));

        notifier.afterSingletonsInstantiated();

        verify(relationListener).relationTypesDeclared(relationTypesCaptor.capture());
        assertThat(relationTypesCaptor.getValue()).containsExactly(
                RelationType.builder().systemId(SYSTEM_A).predicateType("ch.test.predicate.Knows")
                        .subjectType("ch.test.Subject").objectType("ch.test.Object").build(),
                RelationType.builder().systemId(SYSTEM_A).predicateType("ch.test.predicate.Owns")
                        .subjectType("ch.test.Subject").objectType("ch.test.Other").featureFlag("MY_FLAG").build(),
                RelationType.builder().systemId(SYSTEM_B).predicateType("ch.test.predicate.Knows")
                        .subjectType("ch.test.Subject").objectType("ch.test.Object").build());
    }

    @Test
    void afterSingletonsInstantiated_reportedSetIsUnmodifiable() {
        ProcessTemplate template = templateWithPatterns("template", SYSTEM_A,
                pattern("ch.test.predicate.Knows", "ch.test.Subject", "ch.test.Object", null));
        when(processTemplateRepository.getAllTemplates()).thenReturn(List.of(template));

        notifier.afterSingletonsInstantiated();

        verify(relationListener).relationTypesDeclared(relationTypesCaptor.capture());
        Set<RelationType> reported = relationTypesCaptor.getValue();
        assertThatThrownBy(reported::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void afterSingletonsInstantiated_relationPatternsWithoutRelationSystemId_failsNamingTheTemplate() {
        ProcessTemplate template = templateWithPatterns("broken-template", null,
                pattern("ch.test.predicate.Knows", "ch.test.Subject", "ch.test.Object", null));
        when(processTemplateRepository.getAllTemplates()).thenReturn(List.of(template));

        assertThatThrownBy(notifier::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Process template 'broken-template' declares relation patterns but no relationSystemId");
        verifyNoInteractions(relationListener);
    }

    @Test
    void afterSingletonsInstantiated_templateWithoutRelationPatternsAndWithoutRelationSystemId_isAccepted() {
        // Most templates declare no relation patterns and thus no relationSystemId
        ProcessTemplate template = templateWithPatterns("template-without-relations", null);
        when(processTemplateRepository.getAllTemplates()).thenReturn(List.of(template));

        notifier.afterSingletonsInstantiated();

        verify(relationListener).relationTypesDeclared(Set.of());
    }

    @Test
    void afterSingletonsInstantiated_noRelationPatterns_reportsEmptySet() {
        ProcessTemplate template = templateWithPatterns("template", SYSTEM_A);
        when(processTemplateRepository.getAllTemplates()).thenReturn(List.of(template));

        notifier.afterSingletonsInstantiated();

        verify(relationListener).relationTypesDeclared(Set.of());
    }

    @Test
    void afterSingletonsInstantiated_listenerFails_exceptionIsPropagated() {
        when(processTemplateRepository.getAllTemplates()).thenReturn(List.of());
        doThrow(new IllegalStateException("rejected")).when(relationListener).relationTypesDeclared(anySet());

        assertThatThrownBy(notifier::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rejected");
    }

    private static ProcessTemplate templateWithPatterns(String name, String relationSystemId, RelationPattern... patterns) {
        ProcessTemplate template = mock(ProcessTemplate.class);
        lenient().when(template.getName()).thenReturn(name);
        when(template.getRelationPatterns()).thenReturn(List.of(patterns));
        lenient().when(template.getRelationSystemId()).thenReturn(relationSystemId);
        return template;
    }

    private static RelationPattern pattern(String predicate, String subjectType, String objectType, String featureFlag) {
        return RelationPattern.builder()
                .predicateType(predicate)
                .subjectSelector(RelationNodeSelector.builder().type(subjectType).processDataKey("subjectKey").build())
                .objectSelector(RelationNodeSelector.builder().type(objectType).processDataKey("objectKey").build())
                .featureFlag(featureFlag)
                .build();
    }
}
