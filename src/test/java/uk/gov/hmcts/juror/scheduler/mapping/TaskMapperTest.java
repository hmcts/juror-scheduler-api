package uk.gov.hmcts.juror.scheduler.mapping;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.testsupport.util.GenerateUtil;
import uk.gov.hmcts.juror.scheduler.testsupport.util.ValidateUtil;

import java.util.List;

@SuppressWarnings({
    "PMD.JUnitTestsShouldIncludeAssert"//False positive done via support libraries
})
class TaskMapperTest {

    private final TaskMapper taskMapper;

    public TaskMapperTest() {
        this.taskMapper = new TaskMapperImpl();
    }

    @Test
    void verifyTaskDetailList() {
        List<TaskEntity> taskEntityList = List.of(
            GenerateUtil.generateTaskEntity(),
            GenerateUtil.generateTaskEntity(),
            GenerateUtil.generateTaskEntity()
        );
        ValidateUtil.validateTaskEntityList(taskEntityList, taskMapper.toTaskList(taskEntityList));
    }

    @Test
    void verifyTaskDetail() {
        TaskEntity taskEntity = GenerateUtil.generateTaskEntity();
        ValidateUtil.validate(taskEntity, taskMapper.toTask(taskEntity));
    }
}
