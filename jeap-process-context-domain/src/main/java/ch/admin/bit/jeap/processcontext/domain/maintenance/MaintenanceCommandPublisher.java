package ch.admin.bit.jeap.processcontext.domain.maintenance;

public interface MaintenanceCommandPublisher {
    void publish(MaintenanceJob job, MaintenanceTask task);
}
