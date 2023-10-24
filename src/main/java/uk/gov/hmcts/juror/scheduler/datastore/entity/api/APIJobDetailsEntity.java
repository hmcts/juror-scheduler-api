package uk.gov.hmcts.juror.scheduler.datastore.entity.api;


import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.Length;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.ActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Entity
@Audited
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class APIJobDetailsEntity {

    @Id
    @NotNull
    private String key;

    private String cronExpression;
    @NotNull
    private APIMethod method;

    @NotNull
    @Column(length = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    private String name;

    @Column(length = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<@Length(max = APIConstants.DEFAULT_MAX_LENGTH_SHORT) @NotBlank String> tags;

    @NotNull
    private String url;

    @Nullable
    @Length(max = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    private String payload;

    @Nullable
    private AuthenticationDefaults authenticationDefault;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime lastUpdatedAt;

    @Nullable
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "apijob_headers",
                     joinColumns = {@JoinColumn(name = "job_key", referencedColumnName = "key")})
    @MapKeyColumn(name = "key", length = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    @Column(name = "value", length = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    private Map<@NotNull String, String> headers;


    @OneToMany(fetch = FetchType.EAGER,
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<APIValidationEntity> validations;


    @OneToMany(fetch = FetchType.EAGER,
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<ActionEntity> postExecutionActions;


    private List<ActionEntity> getPostExecutionActionsInternal() {
        if (this.postExecutionActions == null) {
            this.postExecutionActions = new ArrayList<>();
        }
        return this.postExecutionActions;
    }

    public List<ActionEntity> getPostExecutionActions() {
        return Collections.unmodifiableList(this.getPostExecutionActionsInternal());
    }

    public void setPostExecutionActions(Collection<ActionEntity> actions) {
        this.getPostExecutionActionsInternal().clear();
        addExecutionAction(actions);
    }

    public void addExecutionAction(Collection<ActionEntity> actions) {
        if (CollectionUtils.isEmpty(actions)) {
            return;
        }
        actions.forEach(this::addExecutionAction);
    }

    public void addExecutionAction(ActionEntity action) {
        getPostExecutionActionsInternal().add(action);
        action.setJob(this);
    }

    private List<APIValidationEntity> getValidationsInternal() {
        if (this.validations == null) {
            this.validations = new ArrayList<>();
        }
        return this.validations;
    }

    public List<APIValidationEntity> getValidations() {
        return Collections.unmodifiableList(this.getValidationsInternal());
    }

    public void setValidations(Collection<APIValidationEntity> validations) {
        this.getValidationsInternal().clear();
        addValidations(validations);
    }

    public void addValidations(Collection<APIValidationEntity> validations) {
        if (CollectionUtils.isEmpty(validations)) {
            return;
        }
        validations.forEach(this::addValidation);
    }

    public void addValidation(APIValidationEntity validation) {
        getValidationsInternal().add(validation);
        validation.setJob(this);
    }
}
