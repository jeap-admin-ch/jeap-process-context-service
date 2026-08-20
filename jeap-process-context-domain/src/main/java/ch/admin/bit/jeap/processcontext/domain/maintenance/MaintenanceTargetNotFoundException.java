package ch.admin.bit.jeap.processcontext.domain.maintenance;

public class MaintenanceTargetNotFoundException extends RuntimeException {
    public MaintenanceTargetNotFoundException() {
        super("Maintenance target not found");
    }
}
