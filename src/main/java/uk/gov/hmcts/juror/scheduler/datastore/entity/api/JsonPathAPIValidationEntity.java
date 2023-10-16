package uk.gov.hmcts.juror.scheduler.datastore.entity.api;

import io.restassured.response.Response;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import uk.gov.hmcts.juror.scheduler.datastore.model.ValidationType;

@Entity
@DiscriminatorValue("JSON_PATH")
@Getter
@Audited
@AllArgsConstructor
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "validation_id")
public class JsonPathAPIValidationEntity extends APIValidationEntity {

    @NotNull
    @Setter
    private String path;

    @NotNull
    @Setter
    private String expectedResponse;


    @Override
    public Result validate(Response response, APIJobDetailsEntity jobData) {
        String returnedString = response.jsonPath().getString(path);
        boolean passed = returnedString != null && returnedString.equals(expectedResponse);

        String message = passed ? null :
            "Expected response to return '" + expectedResponse + "' for json path '" + path
                + "' but got '" + returnedString + "'";
        return Result.builder()
            .passed(passed)
            .message(message)
            .build();
    }

    @Override
    public ValidationType getType() {
        return ValidationType.JSON_PATH;
    }


}
