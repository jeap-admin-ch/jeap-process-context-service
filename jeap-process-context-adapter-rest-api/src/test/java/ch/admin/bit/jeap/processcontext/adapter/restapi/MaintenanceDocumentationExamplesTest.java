package ch.admin.bit.jeap.processcontext.adapter.restapi;

import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceDocumentationExamplesTest {

    private static final List<ExampleSet> EXAMPLES = List.of(
            new ExampleSet(ReevaluationJobController.class, "reevaluation-request.yaml", "reevaluation-report.yaml",
                    "reevaluation-processes.csv", "originProcessId", "event-queued"),
            new ExampleSet(BackfillJobController.class, "backfill-request.yaml", "backfill-report.yaml",
                    "backfill-process-data.csv", "originProcessId,key,value,role", "command-queued"),
            new ExampleSet(RelationPublicationJobController.class, "relation-publication-request.yaml",
                    "relation-publication-report.yaml", "relation-publication-relations.csv", "relationId",
                    "event-queued"));

    @Test
    void openApiAndDocumentationUseGoldenMaintenanceExamples() throws IOException {
        Path repositoryRoot = repositoryRoot();
        Path examplesDirectory = repositoryRoot.resolve("docs/examples/maintenance");
        String documentation = Files.readString(repositoryRoot.resolve("docs/maintenance-jobs.md"));

        for (ExampleSet examples : EXAMPLES) {
            String request = Files.readString(examplesDirectory.resolve(examples.requestFile())).strip();
            String report = Files.readString(examplesDirectory.resolve(examples.reportFile())).strip();
            String csv = Files.readString(examplesDirectory.resolve(examples.csvFile())).strip();

            assertThat(openApiRequestExample(examples.controller())).isEqualTo(request);
            assertThat(openApiReportExample(examples.controller())).isEqualTo(report);
            assertThat(report)
                    .contains("started:")
                    .contains("state: " + examples.initialTaskState())
                    .doesNotContain("state: created");
            assertThat(documentation)
                    .contains(request)
                    .contains("(examples/maintenance/" + examples.requestFile() + ")")
                    .contains("(examples/maintenance/" + examples.reportFile() + ")")
                    .contains("(examples/maintenance/" + examples.csvFile() + ")");
            assertThat(csv).startsWith(examples.csvHeader());
        }
    }

    private String openApiRequestExample(Class<?> controller) {
        return operation(controller, "create").requestBody().content()[0].examples()[0].value().strip();
    }

    private String openApiReportExample(Class<?> controller) {
        return operation(controller, "get").responses()[0].content()[0].examples()[0].value().strip();
    }

    private Operation operation(Class<?> controller, String methodName) {
        return List.of(controller.getDeclaredMethods()).stream()
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .map(method -> method.getAnnotation(Operation.class))
                .orElseThrow();
    }

    private Path repositoryRoot() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null && !Files.isDirectory(directory.resolve("docs/examples/maintenance"))) {
            directory = directory.getParent();
        }
        if (directory == null) {
            throw new IllegalStateException("Could not locate repository root");
        }
        return directory;
    }

    private record ExampleSet(Class<?> controller, String requestFile, String reportFile, String csvFile,
                              String csvHeader, String initialTaskState) {
    }
}
