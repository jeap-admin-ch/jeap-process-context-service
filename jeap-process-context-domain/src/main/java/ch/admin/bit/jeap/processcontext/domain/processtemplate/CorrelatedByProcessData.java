package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class CorrelatedByProcessData {

    @NonNull
    private String processDataKey;

    @NonNull
    private String messageDataKey;

}
