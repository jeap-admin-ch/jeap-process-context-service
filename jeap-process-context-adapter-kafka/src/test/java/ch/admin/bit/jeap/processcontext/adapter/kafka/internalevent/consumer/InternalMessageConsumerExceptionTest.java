package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation.Temporality;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.DefaultExceptionTemporalityResolver;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class InternalMessageConsumerExceptionTest {

    @Test
    void from_databaseFailover_leavesTemporalityToMessagingResolver() {
        SQLException failover = new SQLException("The active SQL connection has changed", "08S02");
        DataAccessResourceFailureException exception =
                new DataAccessResourceFailureException("could not execute batch", failover);

        InternalMessageConsumerException result = InternalMessageConsumerException.from(exception);

        assertThat(result.getTemporality()).isEqualTo(Temporality.UNKNOWN);
        assertThat(result.getErrorCode()).isEqualTo("COULD_NOT_UPDATE_PROCESS");
        assertThat(result.getCause()).isSameAs(exception);
    }

    @Test
    void from_databaseFailover_isResolvedAsTemporaryByMessagingResolver() {
        SQLException failover = new SQLException("The active SQL connection has changed", "08S02");
        RuntimeException processUpdateFailure = new RuntimeException("Failed updating process",
                new DataAccessResourceFailureException("could not execute batch", failover));

        InternalMessageConsumerException result = InternalMessageConsumerException.from(processUpdateFailure);

        assertThat(new DefaultExceptionTemporalityResolver().resolve(result)).contains(Temporality.TEMPORARY);
    }

    @Test
    void from_otherException_isNotResolvedByMessagingResolver() {
        InternalMessageConsumerException result = InternalMessageConsumerException.from(new IllegalStateException("bug"));

        assertThat(new DefaultExceptionTemporalityResolver().resolve(result)).isEmpty();
    }

    @Test
    void from_otherException_leavesTemporalityToMessagingResolver() {
        InternalMessageConsumerException result = InternalMessageConsumerException.from(new IllegalStateException("bug"));

        assertThat(result.getTemporality()).isEqualTo(Temporality.UNKNOWN);
        assertThat(result.getErrorCode()).isEqualTo("COULD_NOT_UPDATE_PROCESS");
    }

    @Test
    void from_transientDataAccessException_isTemporary() {
        InternalMessageConsumerException result = InternalMessageConsumerException.from(new QueryTimeoutException("timeout"));

        assertThat(result.getTemporality()).isEqualTo(Temporality.TEMPORARY);
        assertThat(result.getErrorCode()).isEqualTo("TRANSIENT_DATA_ACCESS_EXCEPTION");
    }

    @Test
    void from_retriableKafkaException_isTemporary() {
        InternalMessageConsumerException result = InternalMessageConsumerException.from(new TimeoutException("timeout"));

        assertThat(result.getTemporality()).isEqualTo(Temporality.TEMPORARY);
        assertThat(result.getErrorCode()).isEqualTo("TRANSIENT_KAFKA_EXCEPTION");
    }
}
