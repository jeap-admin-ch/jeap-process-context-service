package ch.admin.bit.jeap.processcontext.domain.maintenance;

public interface MaintenanceEventPublisher {
    void publish(MaintenanceJob job, MaintenanceTask task);
}
