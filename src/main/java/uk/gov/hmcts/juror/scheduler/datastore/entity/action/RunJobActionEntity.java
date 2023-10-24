package uk.gov.hmcts.juror.scheduler.datastore.entity.action;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import uk.gov.hmcts.juror.scheduler.datastore.model.ActionType;

@Entity
@DiscriminatorValue("STATUS_CODE")
@Getter
@Audited
@PrimaryKeyJoinColumn(name = "action_id")
@AllArgsConstructor
@NoArgsConstructor
public class RunJobActionEntity extends ActionEntity {

    @NotNull
    @NotBlank
    @Setter
    private String jobKey;

    @Override
    public ActionType getType() {
        return ActionType.RUN_JOB;
    }

}
