package ch.admin.bit.jeap.processcontext.domain.maintenance;

import lombok.Getter;

@Getter
public class MaintenanceJobException extends RuntimeException {
    private final MaintenanceJobExceptionReason reason;

    private MaintenanceJobException(MaintenanceJobExceptionReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    static MaintenanceJobException invalidRequest(String message) {
        return new MaintenanceJobException(MaintenanceJobExceptionReason.INVALID_REQUEST, message);
    }

    static MaintenanceJobException conflict(String message) {
        return new MaintenanceJobException(MaintenanceJobExceptionReason.CONFLICT, message);
    }
}
