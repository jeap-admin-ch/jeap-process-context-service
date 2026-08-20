package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobExceptionReason;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ReevaluationJobController.class)
class MaintenanceJobExceptionHandler {

    @ExceptionHandler(MaintenanceJobException.class)
    ResponseEntity<String> handleMaintenanceJobException(MaintenanceJobException exception) {
        HttpStatus status = exception.getReason() == MaintenanceJobExceptionReason.CONFLICT
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<String> handleValidationException() {
        return ResponseEntity.badRequest().body("Invalid request");
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<Void> handleAuthorizationDeniedException() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
