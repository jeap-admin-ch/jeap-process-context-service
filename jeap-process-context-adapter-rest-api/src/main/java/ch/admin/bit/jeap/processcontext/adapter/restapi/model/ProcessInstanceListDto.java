package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProcessInstanceListDto {
    private long totalCount;
    private List<ProcessInstanceLightDto> processInstanceLightDtoList;
}
