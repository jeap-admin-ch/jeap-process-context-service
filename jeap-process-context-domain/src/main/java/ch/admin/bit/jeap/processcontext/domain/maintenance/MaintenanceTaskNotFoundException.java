package ch.admin.bit.jeap.processcontext.domain.maintenance;

public class MaintenanceTaskNotFoundException extends RuntimeException {
    public MaintenanceTaskNotFoundException() {
        super("Maintenance task not found");
    }
}
