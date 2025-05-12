package ch.admin.bit.jeap.processcontext.adapter.kafka.message.filter;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageFilter;

public class TestMessageFilter implements MessageFilter<AvroMessage> {

        @Override
        public boolean filter(AvroMessage message) {
            return false;
        }
    }