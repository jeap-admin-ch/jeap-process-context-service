package ch.admin.bit.jeap.processcontext.repository.template.json;


import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class JsonProcessTemplateLoaderTest {

    @ParameterizedTest
    @CsvSource({"trigger-process-instantiation.json,trigger-process-instantiation-2.json",
                "trigger-conditional-process-instantiation.json,trigger-conditional-process-instantiation-2.json",
                "trigger-process-instantiation.json,trigger-conditional-process-instantiation.json",
                "trigger-conditional-process-instantiation.json,trigger-process-instantiation.json"
    })
    void test_WhenProcessInstantiationDuplicated_ThenExceptionThrown(String resource1Name, String resource2Name) {
        final Resource template1 = new ClassPathResource(resource1Name);
        final Resource template2 = new ClassPathResource(resource2Name);

        assertThatThrownBy(() -> JsonProcessTemplateLoader.loadTemplateResources(new Resource[]{template1, template2}))
                .isInstanceOf(TemplateLoaderException.class);
    }

    @ParameterizedTest
    @CsvSource({"external-process-instantiation.json,external-process-instantiation-2.json",
                "external-process-instantiation.json,trigger-process-instantiation.json",
                "external-process-instantiation.json,trigger-conditional-process-instantiation.json",
                "trigger-conditional-process-instantiation.json,trigger-conditional-process-instantiation-other.json"
    })
    void test_WhenProcessInstantiationNotDuplicated_ThenSuccess(String resource1Name, String resource2Name) throws IOException {
        final Resource template1 = new ClassPathResource(resource1Name);
        final Resource template2 = new ClassPathResource(resource2Name);

        Map<String, ProcessTemplate> result = JsonProcessTemplateLoader.loadTemplateResources(new Resource[]{template1, template2});
        assertThat(result).hasSize(2);
    }

}
