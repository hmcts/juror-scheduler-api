package uk.gov.hmcts.juror.scheduler.datastore.entity.action;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.envers.Audited;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.ActionType;
import uk.gov.hmcts.juror.scheduler.datastore.model.ConditionType;


@Entity
@Getter
@Audited
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
@Accessors(chain = true)
public abstract class ActionEntity {

    @Id
    @SequenceGenerator(name = "action_entity_id_seq_gen", sequenceName = "action_entity_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "action_entity_id_seq_gen", strategy = GenerationType.SEQUENCE)
    private long id;

    @NotNull
    @Setter
    private ConditionType condition;

    @ManyToOne(fetch = FetchType.EAGER)
    @NotNull
    @Setter
    protected APIJobDetailsEntity job;

    @NotNull
    public abstract ActionType getType();
}
