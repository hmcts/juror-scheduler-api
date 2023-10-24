package uk.gov.hmcts.juror.scheduler.api.model.job.details.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.scheduler.datastore.model.ActionType;
import uk.gov.hmcts.juror.scheduler.datastore.model.ConditionType;

@Getter
@Setter
@NoArgsConstructor
@Schema(oneOf = {
    RunJobAction.class
})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(RunJobAction.class)
})
@Slf4j
public abstract class Action {

    @JsonProperty("condition")
    private ConditionType condition;

    @JsonIgnore
    public abstract ActionType getType();
}
