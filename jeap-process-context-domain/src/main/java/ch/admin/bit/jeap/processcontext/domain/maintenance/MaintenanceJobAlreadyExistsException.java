package ch.admin.bit.jeap.processcontext.domain.maintenance;

public class MaintenanceJobAlreadyExistsException extends RuntimeException {
    public MaintenanceJobAlreadyExistsException() {
        super("Maintenance job already exists");
    }

    public MaintenanceJobAlreadyExistsException(Throwable cause) {
        super("Maintenance job already exists", cause);
    }
}
