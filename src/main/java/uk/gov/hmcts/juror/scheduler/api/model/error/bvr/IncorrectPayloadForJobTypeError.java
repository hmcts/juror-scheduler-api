package uk.gov.hmcts.juror.scheduler.api.model.error.bvr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.juror.standard.api.model.error.bvr.BusinessRuleError;

import java.util.List;

@Getter
@Setter
@SuppressWarnings("All")
public class IncorrectPayloadForJobTypeError extends BusinessRuleError {

    @JsonIgnore
    private static final String ERROR_CODE = "INCORRECT_PAYLOAD_FOR_JOB_TYPE";
    @JsonIgnore
    private static final String ERROR_MESSAGE = "The payload you have entered is not valid for this type of Job.";

    public IncorrectPayloadForJobTypeError() {
        super(ERROR_CODE);
        addMessage(ERROR_MESSAGE);
    }

    @Schema(allowableValues = ERROR_CODE)
    @Override
    public String getCode() {
        return super.getCode();
    }

    @Schema(allowableValues = ERROR_MESSAGE)
    @Override
    public List<String> getMessages() {
        return super.getMessages();
    }
}
