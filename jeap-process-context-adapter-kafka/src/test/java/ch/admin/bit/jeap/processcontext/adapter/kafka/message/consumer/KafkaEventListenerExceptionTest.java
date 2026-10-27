package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation.Temporality;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaEventListenerExceptionTest {

    @Test
    void from_leavesTemporalityToMessagingResolver() {
        SQLException failover = new SQLException("The active SQL connection has changed", "08S02");
        DataAccessResourceFailureException exception =
                new DataAccessResourceFailureException("could not execute statement", failover);

        KafkaEventListenerException result = KafkaEventListenerException.from(exception);

        assertThat(result.getTemporality()).isEqualTo(Temporality.UNKNOWN);
        assertThat(result.getErrorCode()).isEqualTo("ERROR_CONSUMING_EVENT");
        assertThat(result.getCause()).isSameAs(exception);
    }
}
