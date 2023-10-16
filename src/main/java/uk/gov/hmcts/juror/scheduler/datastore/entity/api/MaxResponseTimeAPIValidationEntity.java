package uk.gov.hmcts.juror.scheduler.datastore.entity.api;

import io.restassured.response.Response;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import uk.gov.hmcts.juror.scheduler.datastore.model.ValidationType;

@Entity
@DiscriminatorValue("MAX_RESPONSE_TIME")
@Getter
@Audited
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "validation_id")
public class MaxResponseTimeAPIValidationEntity extends APIValidationEntity {

    @NotNull
    @Min(1)
    @Max(30_000)
    @Setter
    private int maxResponseTimeMS;

    @Override
    public Result validate(Response response, APIJobDetailsEntity jobData) {
        boolean passed = response.getTime() <= maxResponseTimeMS;

        String message = passed
            ? null
            :
                "API call took longer then the max response time allowed. Max response time: "
                    + maxResponseTimeMS + " ms but took: " + response.getTime() + " ms";
        return Result.builder()
            .passed(passed)
            .message(message)
            .build();
    }

    @Override
    public ValidationType getType() {
        return ValidationType.MAX_RESPONSE_TIME;
    }
}
