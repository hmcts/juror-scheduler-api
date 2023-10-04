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
import uk.gov.hmcts.juror.scheduler.api.APIConstants;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class APIJobDetailsEntity  {

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
    @Column(name = "value",length = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    private Map<@NotNull String, String> headers;


    @OneToMany(fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private List<APIValidationEntity> validations;


    public void addValidations(List<APIValidationEntity> validations){
        validations.forEach(this::addValidation);
    }
    public void addValidation(APIValidationEntity validation){
        if(validations == null){
            validations = new ArrayList<>();
        }
        validations.add(validation);
        validation.setJob(this);
    }

    public void setValidations(List<APIValidationEntity> validations){
        this.validations.clear();
        addValidations(validations);
    }
}
