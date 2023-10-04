package uk.gov.hmcts.juror.scheduler.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.api.model.task.Task;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class TaskMapper {

    public abstract List<Task> toTaskList(List<TaskEntity> taskEntityList);

    @Mapping(target = "jobKey", source = "job.key")
    public abstract Task toTask(TaskEntity taskEntity);
}
