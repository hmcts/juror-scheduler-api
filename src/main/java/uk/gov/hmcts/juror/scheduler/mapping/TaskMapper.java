package uk.gov.hmcts.juror.scheduler.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class TaskMapper {

    public abstract List<TaskDetail> toTaskList(List<TaskEntity> taskEntityList);

    @Mapping(target = "jobKey", source = "job.key")
    public abstract TaskDetail toTask(TaskEntity taskEntity);

    public String toTaskJson(ObjectMapper objectMapper, TaskEntity latestTask) {
        try {
            return objectMapper.writeValueAsString(toTask(latestTask));
        } catch (Exception e) {
            throw new InternalServerException("Error converting Task to JSON", e);
        }
    }
}
