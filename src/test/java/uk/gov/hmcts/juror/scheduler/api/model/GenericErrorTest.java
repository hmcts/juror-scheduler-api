package uk.gov.hmcts.juror.scheduler.api.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.juror.standard.api.model.error.GenericError;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        ObjectMapper.class
    }
)
public abstract class GenericErrorTest<T extends GenericError> {

    protected abstract String getErrorCode();

    protected abstract String getDefaultMessage();

    protected abstract T createErrorObject();

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void positive_constructor_test() {
        T errorObject = createErrorObject();
        assertNotNull(errorObject);
        assertEquals(getErrorCode(), errorObject.getCode());

        if (getDefaultMessage() != null) {
            assertNotNull(errorObject.getMessages());
            assertEquals(1, errorObject.getMessages().size());
            assertThat(errorObject.getMessages(), hasItem(getDefaultMessage()));
        } else {
            assertNull(errorObject.getMessages());
        }
    }

    @Test
    void positive_json_serialize_test() {
        T errorObject = createErrorObject();
        validateJson(errorObject, getDefaultMessage());
    }

    protected void validateJson(T errorObject, String defaultMessage) {
        validateJson(errorObject, defaultMessage == null ? null : Collections.singletonList(defaultMessage));
    }


    protected void validateJson(T error, List<String> messages) {
        try {
            String generatedJson = objectMapper.writeValueAsString(error);

            final String messageList;
            if (messages == null || messages.isEmpty()) {
                messageList = "null";
            } else {
                messages = messages.stream().map(message -> "\"" + message + "\"").collect(Collectors.toList());
                messageList = "[" + StringUtils.join(messages, ",") + "]";
            }

            String expectedJsonBuilder = "{\"code\":\"" +
                getErrorCode() +
                "\"" +
                ",\"messages\":" +
                messageList +
                "}";

            JSONAssert.assertEquals(expectedJsonBuilder, generatedJson, true);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
