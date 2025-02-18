package ch.admin.bit.jeap.processcontext.domain.processinstance;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessDataWrapperTest {

    @Test
    void findByKeyAndOptionalRole() {
        ProcessData pd1 = new ProcessData("targetKeyName", "someValue");
        ProcessData pd2 = new ProcessData("targetKeyName", "someValue", "someRole");
        ProcessData pd3 = new ProcessData("otherTargetKeyName", "value123", "otherRole");
        Set<ProcessData> processDataSet = Set.of(pd1, pd2, pd3);

        ProcessDataWrapper wrapper = ProcessDataWrapper.of(processDataSet);

        assertTrue(wrapper.findByKeyAndOptionalRole("inexistant", null).isEmpty());
        assertEquals(Set.of(pd1, pd2), wrapper.findByKeyAndOptionalRole("targetKeyName", null));
        assertEquals(Set.of(pd3), wrapper.findByKeyAndOptionalRole("otherTargetKeyName", "otherRole"));
    }
}
