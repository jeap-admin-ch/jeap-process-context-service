package ch.admin.bit.jeap.processcontext.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TranslateServiceTest {

    private final String processName = "myProcess";

    @Test
    void translateProcessTemplateName() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessTemplateName(processName);
        assertThat(labels)
                .containsEntry("de", "[de]My Process")
                .containsEntry("fr", "[fr]My Process")
                .containsEntry("it", "[it]My Process");
    }

    @Test
    void translateUserDataKey_fromDefaultBundle() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateUserDataKey("familyName");
        assertThat(labels)
                .containsEntry("de", "Nachname")
                .containsEntry("fr", "Nom")
                .containsEntry("it", "Cognome");
    }

    @Test
    void translateUserDataKey_keyNotFound() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateUserDataKey("keyNotFound");
        assertThat(labels)
                .containsEntry("de", "keyNotFound")
                .containsEntry("fr", "keyNotFound")
                .containsEntry("it", "keyNotFound");
    }

    @Test
    void translateUserDataKey_fromLocalBundle() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateUserDataKey("customValue");
        assertThat(labels)
                .containsEntry("de", "[de]Custom Value")
                .containsEntry("fr", "[fr]Custom Value")
                .containsEntry("it", "[it]Custom Value");
    }

    @Test
    void translateUserDataKey_fromLocalBundleOverDefaultBundle() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateUserDataKey("givenName");
        assertThat(labels)
                .containsEntry("de", "[de]Override givenName")
                .containsEntry("fr", "[fr]Override givenName")
                .containsEntry("it", "[it]Override givenName");
    }

        @Test
    void translateProcessTemplateName_nullKey_exceptionThrown() {
        TranslateService translateService = new TranslateService();
        Throwable t = assertThrows(NullPointerException.class, () -> translateService.translateProcessTemplateName(null));
        assertThat(t.getMessage()).isEqualTo("processTemplateName is marked non-null but is null");
    }

    @Test
    void translateProcessTemplateName_keyNotFound_exceptionThrown() {
        TranslateService translateService = new TranslateService();
        Throwable t = assertThrows(IllegalArgumentException.class, () -> translateService.translateProcessTemplateName("dummy"));
        assertThat(t.getMessage()).isEqualTo("No translation found in ResourceBundles for key: 'dummy.label'");
    }

    @Test
    void noTranslation() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.noTranslation("default");
        assertThat(labels)
                .containsEntry("de", "default")
                .containsEntry("fr", "default")
                .containsEntry("it", "default");
    }

    @Test
    void translateTaskTypeName() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateTaskTypeName(processName, "myTaskName");
        assertThat(labels)
                .containsEntry("de", "[de]My Task")
                .containsEntry("fr", "[fr]My Task")
                .containsEntry("it", "[it]My Task");
    }

    @Test
    void translateTaskDataKey() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateTaskDataKey(processName, "myTaskName", "myDataKey");
        assertThat(labels)
                .containsEntry("de", "[de]My Data Key")
                .containsEntry("fr", "[fr]My Data Key")
                .containsEntry("it", "[it]My Data Key");
    }

    @Test
    void translateProcessRelationOriginRole() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessRelationOriginRole(processName, "myRelation");
        assertThat(labels)
                .containsEntry("de", "[de]My Relation Origin")
                .containsEntry("fr", "[fr]My Relation Origin")
                .containsEntry("it", "[it]My Relation Origin");
    }

    @Test
    void translateProcessRelationTargetRole() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessRelationTargetRole(processName, "myRelation");
        assertThat(labels)
                .containsEntry("de", "[de]My Relation Target")
                .containsEntry("fr", "[fr]My Relation Target")
                .containsEntry("it", "[it]My Relation Target");
    }

    @Test
    void translateProcessCompletionName() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessCompletionName(processName, "processCompletionName");
        assertThat(labels)
                .containsEntry("de", "[de]My Process Completion")
                .containsEntry("fr", "[fr]My Process Completion")
                .containsEntry("it", "[it]My Process Completion");
    }

    @Test
    void translateProcessCompletionName_nameNotFound() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessCompletionName(processName, "nameNotFound");
        assertThat(labels)
                .containsEntry("de", "nameNotFound")
                .containsEntry("fr", "nameNotFound")
                .containsEntry("it", "nameNotFound");
    }

    @Test
    void translateProcessCompletionName_noName() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessCompletionName(processName, null);
        assertThat(labels)
                .containsEntry("de", "-")
                .containsEntry("fr", "-")
                .containsEntry("it", "-");
    }

    @Test
    void translateProcessCompletionName_legacyProcessCompletionCondition() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessCompletionName(processName, "legacyProcessCompletionCondition");
        assertThat(labels)
                .containsEntry("de", "Alle Aufgaben wurden geplant und zu einem endgültigen Stand gebracht")
                .containsEntry("fr", "Toutes les tâches ont été planifiées et ont atteint un état final")
                .containsEntry("it", "Tutte le attività sono stati pianificati e hanno raggiunto uno stato finale");
    }

    @Test
    void translateProcessCompletionName_allTasksInFinalStateProcessCompletionCondition() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessCompletionName(processName, "allTasksInFinalStateProcessCompletionCondition");
        assertThat(labels)
                .containsEntry("de", "Alle Prozessaufgaben haben einen Endzustand erreicht")
                .containsEntry("fr", "Toutes les tâches du processus ont atteint un état final")
                .containsEntry("it", "Tutte le attività del processo hanno raggiunto uno stato finale");
    }

    @Test
    void translateProcessCompletionNameFromSnapshot_nullName() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessCompletionNameFromSnapshot(processName, null, "old label");
        assertThat(labels)
                .containsEntry("de", "old label")
                .containsEntry("fr", "old label")
                .containsEntry("it", "old label");
    }

    @Test
    void translateProcessCompletionNameFromSnapshot_nameNotFound() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessCompletionNameFromSnapshot(processName, "nameNotFound", "old label");
        assertThat(labels)
                .containsEntry("de", "old label")
                .containsEntry("fr", "old label")
                .containsEntry("it", "old label");
    }

    @Test
    void translateProcessCompletionNameFromSnapshot_nameNotFound_noLabel() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessCompletionNameFromSnapshot(processName, "nameNotFound", null);
        assertThat(labels)
                .containsEntry("de", "nameNotFound")
                .containsEntry("fr", "nameNotFound")
                .containsEntry("it", "nameNotFound");
    }

    @Test
    void translateProcessCompletionNameFromSnapshot_noName_noLabel() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessCompletionNameFromSnapshot(processName, null, null);
        assertThat(labels)
                .containsEntry("de", "-")
                .containsEntry("fr", "-")
                .containsEntry("it", "-");
    }

    @Test
    void translateProcessCompletionNameFromSnapshot_legacyProcessCompletionCondition() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessCompletionNameFromSnapshot(processName, "legacyProcessCompletionCondition", null);
        assertThat(labels)
                .containsEntry("de", "Alle Aufgaben wurden geplant und zu einem endgültigen Stand gebracht")
                .containsEntry("fr", "Toutes les tâches ont été planifiées et ont atteint un état final")
                .containsEntry("it", "Tutte le attività sono stati pianificati e hanno raggiunto uno stato finale");
    }

    @Test
    void translateProcessCompletionNameFromSnapshot_allTasksInFinalStateProcessCompletionCondition() {
        TranslateService translateService = new TranslateService();
        Map<String, String> labels = translateService.translateProcessCompletionNameFromSnapshot(processName, "allTasksInFinalStateProcessCompletionCondition", null);
        assertThat(labels)
                .containsEntry("de", "Alle Prozessaufgaben haben einen Endzustand erreicht")
                .containsEntry("fr", "Toutes les tâches du processus ont atteint un état final")
                .containsEntry("it", "Tutte le attività del processo hanno raggiunto uno stato finale");
    }

}
