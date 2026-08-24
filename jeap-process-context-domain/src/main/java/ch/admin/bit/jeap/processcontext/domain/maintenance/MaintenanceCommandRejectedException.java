package ch.admin.bit.jeap.processcontext.domain.maintenance;

public class MaintenanceCommandRejectedException extends RuntimeException {
    public MaintenanceCommandRejectedException(String message) {
        super(message);
    }
}
