package ch.admin.bit.jeap.processcontext.domain.processinstance;

public interface ProcessInstanceSummary {

    String getProcessTemplateName();

    String getOriginProcessId();

    ProcessState getState();

}
