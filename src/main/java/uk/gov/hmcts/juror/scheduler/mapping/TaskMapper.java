package uk.gov.hmcts.juror.scheduler.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;

import java.util.List;

@Mapper(componentModel = "spring")
//TODO test
public abstract class TaskMapper {

    public abstract List<TaskDetail> toTaskList(List<TaskEntity> taskEntityList);

    @Mapping(target = "jobKey", source = "job.key")
    public abstract TaskDetail toTask(TaskEntity taskEntity);
}
