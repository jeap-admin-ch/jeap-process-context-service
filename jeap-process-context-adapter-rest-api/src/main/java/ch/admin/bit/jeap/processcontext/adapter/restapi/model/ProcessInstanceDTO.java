package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Value
@Builder(access = AccessLevel.PACKAGE)
@Slf4j
public class ProcessInstanceDTO {

    String originProcessId;
    Map<String, String> name;
    String state;
    ZonedDateTime createdAt;
    ZonedDateTime modifiedAt;
    List<TaskInstanceDTO> tasks;
    List<MessageDTO> messages;
    List<ProcessDataDTO> processData;
    List<RelationDTO> relations;
    List<ProcessRelationDTO> processRelations;
    ProcessCompletionDTO processCompletion;
    boolean snapshot;
    ZonedDateTime snapshotCreatedAt;

}
