package uk.gov.hmcts.juror.scheduler.api.model.error;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.juror.standard.api.model.error.GenericError;

import java.util.List;

@Getter
@Setter
public class KeyAlreadyInUseError extends GenericError {

    @JsonIgnore
    private static final String ERROR_CODE = "KEY_ALREADY_IN_USE";
    @JsonIgnore
    private static final String ERROR_MESSAGE = "The key you have provided is already in use. "
        + "Please choice a unique key.";


    public KeyAlreadyInUseError() {
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
