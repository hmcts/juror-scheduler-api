package uk.gov.hmcts.juror.scheduler.testSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.juror.scheduler.testSupport.controller.ControllerTest;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Preferred method is to use {@link ControllerTest}
 */
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;


    //TODO replace with manual converter
    protected <T> String createResponseStringFromObject(T apiJobDetailsResponses) throws JsonProcessingException {
        return objectMapper.writeValueAsString(apiJobDetailsResponses);
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
