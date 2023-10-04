package uk.gov.hmcts.juror.scheduler.testSupport.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ControllerWithPayloadTest extends ControllerTest {

    public ControllerWithPayloadTest(HttpMethod method, String url, HttpStatus successStatus) {
        super(method,url,successStatus);
    }
    protected Stream<InvalidPayloadArgument> getStandardInvalidPayloads() {
        return Stream.of(
                new InvalidPayloadArgument(null, "Unable to read payload content"),
                new InvalidPayloadArgument("Non-Json", "Unable to read payload content")
        );
    }

    @ParameterizedTest(name = "Expect error message: {0}")
    @MethodSource({"getInvalidPayloadArgumentSource", "getStandardInvalidPayloads"})
    @DisplayName("Invalid Payload")
    protected void callAndExpectErrorResponse(ErrorRequestArgument errorRequestArgument) throws Exception {
        callAndValidate(errorRequestArgument);
    }

    @Test
    protected void negative_invalid_content_type() throws Exception {
        callAndValidate(new ErrorRequestArgument(HttpStatus.UNSUPPORTED_MEDIA_TYPE, getTypicalPayload(), "INVALID_CONTENT_TYPE", "Content Type must be application/json").setContentType(MediaType.TEXT_PLAIN));
    }

   @Test
    protected void negative_missing_content_type() throws Exception {
        callAndValidate(new ErrorRequestArgument(HttpStatus.UNSUPPORTED_MEDIA_TYPE, getTypicalPayload(), "INVALID_CONTENT_TYPE", "Content Type must be application/json").setContentType(null));
    }

    protected abstract String getTypicalPayload();

    protected abstract Stream<SuccessRequestArgument> getSuccessRequestArgument();

    @ParameterizedTest(name = "Positive: {0}")
    @MethodSource("getSuccessRequestArgument")
    protected void positive_valid_typical(SuccessRequestArgument requestArgument) throws Exception {
        callAndValidate(requestArgument);
    }


    protected abstract Stream<InvalidPayloadArgument> getInvalidPayloadArgumentSource();

}
