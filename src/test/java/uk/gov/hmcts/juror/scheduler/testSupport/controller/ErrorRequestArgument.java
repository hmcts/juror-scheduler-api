package uk.gov.hmcts.juror.scheduler.testSupport.controller;

import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.juror.scheduler.testSupport.TestUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ErrorRequestArgument extends RequestArgument {
    private final String code;
    private final String[] expectedErrorMessages;
    private final HttpStatus expectedStatus;

    public ErrorRequestArgument(HttpStatus expectedStatus, String requestPayload, String code, String... expectedErrorMessages) {
        super(null, null, requestPayload);
        this.expectedStatus = expectedStatus;
        this.code = code;
        this.expectedErrorMessages = expectedErrorMessages;
    }

    @Override
    public String toString() {
        return Arrays.toString(this.expectedErrorMessages);
    }

    @Override
    public void runPostActions(ResultActions resultActions, ControllerTest controllerTest) throws Exception {
        resultActions.andExpect(status().is(this.expectedStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(
                        TestUtil.jsonMatcher(JSONCompareMode.NON_EXTENSIBLE, createErrorResponseString(this.code, this.expectedErrorMessages)));
        super.runPostActions(resultActions, controllerTest);
    }

    protected static String createErrorResponseString(String errorCode, String... messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"code\":\"").append(errorCode).append("\"");

        if (messages != null && messages.length > 0) {
            builder.append(",\"messages\": [");
            builder.append(Arrays.stream(messages).map(s -> "\"" + s + "\"").collect(Collectors.joining(",")));
            builder.append("]");
        }
        builder.append("}");
        return builder.toString();
    }
}
