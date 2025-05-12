package ch.admin.bit.jeap.processcontext.adapter.kafka.message.filter;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;

public class TestMessageFilterWithoutImplements {

        public boolean filter(AvroMessage message) {
            return false;
        }
    }