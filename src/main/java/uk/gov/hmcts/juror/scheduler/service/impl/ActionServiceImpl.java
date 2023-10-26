package uk.gov.hmcts.juror.scheduler.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.juror.scheduler.actions.ActionRunner;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.ActionType;
import uk.gov.hmcts.juror.scheduler.datastore.model.ConditionType;
import uk.gov.hmcts.juror.scheduler.service.contracts.ActionService;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class ActionServiceImpl implements ActionService {
    private final Map<ActionType, List<ActionRunner>> actionRunners;

    public ActionServiceImpl(List<ActionRunner> actionRunners) {
        this.actionRunners = new EnumMap<>(ActionType.class);
        actionRunners.forEach(actionRunner ->
            actionRunner.supports().forEach(
                actionType -> this.actionRunners.computeIfAbsent(actionType,
                    key -> new ArrayList<>()).add(actionRunner)
            ));
    }

    @Override
    public TaskEntity taskUpdated(TaskEntity taskEntity) {
        if (!hasActions(taskEntity.getJob())) {
            return taskEntity;
        }
        AtomicReference<String> postActionsMessage = new AtomicReference<>("");

        taskEntity.getJob().getPostExecutionActions()
            .stream()
            .filter(action -> action.getCondition()
                .isMet(ConditionType.TaskEntityChangedListener.class, taskEntity))
            .forEach(action -> {
                try {
                    this.actionRunners.get(action.getType())
                        .forEach(actionRunner -> actionRunner.trigger(action, taskEntity));
                } catch (Exception e) {
                    log.error("Unexpected exception when running RunJobAction", e);
                    postActionsMessage.set("Failed to run post actions. Unexpected exception");
                }
            });
        if (!postActionsMessage.get().isBlank()) {
            taskEntity.appendPostActionsMessage(postActionsMessage.get());
        }
        return taskEntity;
    }

    static boolean hasActions(APIJobDetailsEntity job) {
        return !CollectionUtils.isEmpty(job.getPostExecutionActions());
    }
}
