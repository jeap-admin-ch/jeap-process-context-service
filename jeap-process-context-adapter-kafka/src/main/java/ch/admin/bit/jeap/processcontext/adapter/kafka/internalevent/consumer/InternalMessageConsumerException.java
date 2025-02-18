package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.processcontext.domain.processinstance.NotFoundException;
import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskPlanningException;
import lombok.Getter;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.dao.TransientDataAccessException;

import java.io.PrintWriter;
import java.io.StringWriter;

@Getter
public class InternalMessageConsumerException extends RuntimeException implements MessageHandlerExceptionInformation {
    private final String errorCode;
    private final String description;
    private final Temporality temporality;

    private InternalMessageConsumerException(String errorCode, String description, Temporality temporality, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
        this.description = description;
        this.temporality = temporality;
    }

    static InternalMessageConsumerException from(Exception e) {
        if (e instanceof NotFoundException nfe) {
            Temporality temporality = nfe.isRetryable() ? Temporality.TEMPORARY : Temporality.PERMANENT;
            return new InternalMessageConsumerException("PROCESS_OR_TASK_NOT_FOUND",
                    "The given process or task could not be found, maybe it has not yet been created",
                    temporality,
                    e
            );
        }
        if (e instanceof TaskPlanningException) {
            return new InternalMessageConsumerException("TASK_CANNOT_BE_PLANNED",
                    "The task cannot be planned",
                    Temporality.PERMANENT,
                    e
            );
        }
        if (e instanceof TransientDataAccessException) {
            return new InternalMessageConsumerException("TRANSIENT_DATA_ACCESS_EXCEPTION",
                    "Transient data access exception occurred",
                    Temporality.TEMPORARY,
                    e
            );
        }
        if (e instanceof RetriableException) {
            return new InternalMessageConsumerException("TRANSIENT_KAFKA_EXCEPTION",
                    "Transient kafka exception occurred",
                    Temporality.TEMPORARY,
                    e
            );
        }
        return new InternalMessageConsumerException("COULD_NOT_UPDATE_PROCESS",
                "The given process could not be updated",
                Temporality.PERMANENT,
                e
        );
    }

    @Override
    public String getStackTraceAsString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        printStackTrace(pw);
        return sw.toString();
    }
}
