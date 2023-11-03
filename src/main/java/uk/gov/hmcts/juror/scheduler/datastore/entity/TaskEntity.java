package uk.gov.hmcts.juror.scheduler.datastore.entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.Length;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@Setter
@Audited
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEntity {
    @Id
    @SequenceGenerator(name = "task_entity_task_id_seq_gen",
        sequenceName = "task_entity_task_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "task_entity_task_id_seq_gen",
        strategy = GenerationType.SEQUENCE)
    private long taskId;

    @ManyToOne(fetch = FetchType.EAGER)
    @NotNull
    @Setter
    protected APIJobDetailsEntity job;

    @Length(min = 1, max = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    private String message;

    @Length(min = 1, max = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    private String postActionsMessage;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<@Length(min = 1, max = APIConstants.DEFAULT_MAX_LENGTH_LONG) String,
        @Length(min = 1, max = APIConstants.DEFAULT_MAX_LENGTH_LONG) String> metaData;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Status status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime lastUpdatedAt;

    public void appendPostActionsMessage(String message) {
        if (Strings.isBlank(this.postActionsMessage)) {
            this.postActionsMessage = message;
        } else {
            this.postActionsMessage += ", " + message;
        }
    }

    private Map<String, String> getMetaDataInternal() {
        if (this.metaData == null) {
            this.metaData = new HashMap<>();
        }
        return this.metaData;
    }

    public Map<String, String> getMetaData() {
        return Collections.unmodifiableMap(getMetaDataInternal());
    }

    public void addMetaData(Map<String, String> metaDataValues) {
        if (CollectionUtils.isEmpty(metaDataValues)) {
            return;
        }
        this.getMetaDataInternal().putAll(metaDataValues);
    }
}
