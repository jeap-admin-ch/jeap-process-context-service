package ch.admin.bit.jeap.processcontext.domain.processrelation;

import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleVisibility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessRelationsService {

    private final ProcessRelationRepository processRelationRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TranslateService translateService;


    /**
     * When creating the List of ProcessRelationViews, there are two things, which have to be done:
     * 1. Create ProcessRelationViews from the ProcessRelation-Objects, which are directly attached to the processInstance
     * 2. Create ProcessRelationViews from other ProcessInstances where the processInstanceId is stored.
     */
    public List<ProcessRelationView> createProcessRelations(ProcessInstance processInstance) {
        Set<ProcessRelation> processRelations = processInstance.getProcessRelations();
        List<ProcessRelationView> processRelationViewList =
                new ArrayList<>(processRelations.stream()
                        .filter(processRelation -> processRelation.getVisibilityType() != ProcessRelationRoleVisibility.TARGET)
                        .map(this::evaluateDirectProcessRelation)
                        .toList());

        List<ProcessRelationView> externalList = this.findExternalProcessRelations(processInstance.getOriginProcessId());
        processRelationViewList.addAll(externalList);
        return processRelationViewList;
    }

    /**
     * Find process relations, which have the given id processId in relatedProcessId.
     * Filter all process relations out, which have visibility ORIGIN.
     */
    private List<ProcessRelationView> findExternalProcessRelations(String originProcessId) {

        List<ProcessRelation> processRelationList = this.processRelationRepository.findByRelatedProcessId(originProcessId);
        return processRelationList.stream()
                // Take only these processRelation, which have the visibility BOTH or TARGET
                .filter(processRelation -> processRelation.getVisibilityType() != ProcessRelationRoleVisibility.ORIGIN)
                .map(this::createExternalProcessRelationView)
                .toList();
    }

    /**
     * Try to get the Data from an existing process instance. If the process instance does not exist, fill the
     * data which we know.
     * Because it is a direct process reference, the reference text has this rules:
     * - If ORIGIN --> reference text comes from targetRole
     * - if TARGET --> reference text comes from originalRole
     */
    private ProcessRelationView evaluateDirectProcessRelation(ProcessRelation originProcessRelation) {
        String processTemplateName = originProcessRelation.getProcessInstance().getProcessTemplateName();

        Optional<ProcessInstanceSummary> processInstanceSummaryOptional = processInstanceRepository.findProcessInstanceSummaryByOriginProcessId(originProcessRelation.getRelatedProcessId());

        Map<String, String> relationTextI18N;
        if (originProcessRelation.getRoleType() == ProcessRelationRoleType.ORIGIN) {
            relationTextI18N = translateService.translateProcessRelationTargetRole(processTemplateName, originProcessRelation.getName());
        } else {
            relationTextI18N = translateService.translateProcessRelationOriginRole(processTemplateName, originProcessRelation.getName());
        }

        if (processInstanceSummaryOptional.isPresent()) {
            ProcessInstanceSummary instanceSummary = processInstanceSummaryOptional.get();
            return ProcessRelationView.builder()
                    .relationName(originProcessRelation.getName())
                    .originRole(originProcessRelation.getOriginRole())
                    .targetRole(originProcessRelation.getTargetRole())
                    .processTemplateName(instanceSummary.getProcessTemplateName())
                    .relationRole(originProcessRelation.getRoleType())
                    .processId(instanceSummary.getOriginProcessId())
                    .processName(translateService.translateProcessTemplateName(instanceSummary.getProcessTemplateName()))
                    .processState(instanceSummary.getState().toString())
                    .relation(relationTextI18N)
                    .build();

        } else {
            return ProcessRelationView.builder()
                    .relationName(originProcessRelation.getName())
                    .originRole(originProcessRelation.getOriginRole())
                    .targetRole(originProcessRelation.getTargetRole())
                    .processTemplateName("unknown")
                    .relationRole(originProcessRelation.getRoleType())
                    .processId(originProcessRelation.getRelatedProcessId())
                    .processName(TranslateService.noTranslation("-"))
                    .relation(relationTextI18N)
                    .build();
        }
    }

    /**
     * Creates a process relation from am external process instance. ProcessRelation can not be null.
     * Because it is external process reference, the reference text has this rules:
     * - If ORIGIN --> reference text comes from originalRole
     * - if TARGET --> reference text comes from targetRole
     */
    private ProcessRelationView createExternalProcessRelationView(ProcessRelation processRelation) {

        ProcessInstance processInstance = processRelation.getProcessInstance();
        String processTemplateName = processInstance.getProcessTemplateName();

        Map<String, String> relationTextI18N;
        if (processRelation.getRoleType() == ProcessRelationRoleType.ORIGIN) {
            relationTextI18N = translateService.translateProcessRelationOriginRole(processTemplateName, processRelation.getName());
        } else {
            relationTextI18N = translateService.translateProcessRelationTargetRole(processTemplateName, processRelation.getName());
        }

        return ProcessRelationView.builder()
                .relationName(processRelation.getName())
                .originRole(processRelation.getOriginRole())
                .targetRole(processRelation.getTargetRole())
                .processTemplateName(processTemplateName)
                .relationRole(processRelation.getRoleType())
                .processId(processInstance.getOriginProcessId())
                .processName(translateService.translateProcessTemplateName(processTemplateName))
                .processState(processInstance.getState().name())
                .relation(relationTextI18N)
                .build();
    }
    }
