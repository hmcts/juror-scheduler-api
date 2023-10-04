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
@DiscriminatorValue("STATUS_CODE")
@Getter
@Audited
@PrimaryKeyJoinColumn(name = "validation_id")
@AllArgsConstructor
@NoArgsConstructor
public class StatusCodeValidationEntity extends APIValidationEntity {

    @NotNull
    @Min(100)
    @Max(599)
    @Setter
    private int expectedStatusCode;

    @Override
    public Result validate(Response response, APIJobDetailsEntity jobData) {
        boolean passed =  response.statusCode() == expectedStatusCode;
        String message = passed ? null :
            "Expected status code of " + expectedStatusCode + " but got " + response.statusCode();
        return Result.builder()
            .passed(passed)
            .message(message)
            .build();
    }

    @Override
    public ValidationType getType() {
        return ValidationType.STATUS_CODE;
    }
}
