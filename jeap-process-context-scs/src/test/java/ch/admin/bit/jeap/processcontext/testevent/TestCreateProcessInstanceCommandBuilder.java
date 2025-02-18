package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.command.test.createprocessinstance.TestCreateProcessInstanceCommand;
import ch.admin.bit.jeap.processcontext.command.test.createprocessinstance.TestCreateProcessInstanceCommandReferences;

public class TestCreateProcessInstanceCommandBuilder extends AbstractTestCommandBuilder<TestCreateProcessInstanceCommandBuilder, TestCreateProcessInstanceCommand> {

    private TestCreateProcessInstanceCommandBuilder(String originProcessId) {
        super(TestCreateProcessInstanceCommand::new, () -> TestCreateProcessInstanceCommandReferences.newBuilder().build(), "TestCreateProcessInstanceCommand", originProcessId);
    }

    public static TestCreateProcessInstanceCommandBuilder createForProcessId(String originProcessId) {
        return new TestCreateProcessInstanceCommandBuilder(originProcessId);
    }
}
