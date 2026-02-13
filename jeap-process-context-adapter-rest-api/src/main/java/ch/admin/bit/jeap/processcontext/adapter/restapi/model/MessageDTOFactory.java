package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageDTOFactory {

    private final MessageReferenceRepository messageReferenceRepository;

    public Page<MessageDTO> createMessageDTOPage(UUID processInstanceId, Pageable pageable) {
        return messageReferenceRepository.findByProcessInstanceId(processInstanceId, pageable)
                .map(MessageDTO::create);
    }
}
