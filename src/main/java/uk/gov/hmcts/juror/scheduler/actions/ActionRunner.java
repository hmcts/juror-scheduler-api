package uk.gov.hmcts.juror.scheduler.actions;

import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.ActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.ActionType;

import java.util.Set;


public interface ActionRunner {

    void trigger(ActionEntity action, TaskEntity taskEntity);

    Set<ActionType> supports();
}
