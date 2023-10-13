package uk.gov.hmcts.juror.scheduler.api.model.error.bvr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.juror.standard.api.model.error.bvr.BusinessRuleError;

import java.util.List;

@Getter
@Setter
public class JobAlreadyDisabledError extends BusinessRuleError {

    @JsonIgnore
    private static final String ERROR_CODE = "JOB_ALREADY_DISABLED";
    @JsonIgnore
    private static final String ERROR_MESSAGE = "This Job is already disabled";

    public JobAlreadyDisabledError() {
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
