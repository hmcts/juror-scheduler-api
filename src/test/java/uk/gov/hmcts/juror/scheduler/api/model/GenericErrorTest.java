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
    @Autowired
    private ObjectMapper objectMapper;

    protected abstract String getErrorCode();

    protected abstract String getDefaultMessage();

    protected abstract T createErrorObject();


    @Test
    void positiveConstructorTest() {
        T errorObject = createErrorObject();
        assertNotNull(errorObject, "Error object must not be null");
        assertEquals(getErrorCode(), errorObject.getCode(), "Error code must match");

        if (getDefaultMessage() != null) {
            assertNotNull(errorObject.getMessages(), "Messages must not be null");
            assertEquals(1, errorObject.getMessages().size(), "Message size must match");
            assertThat("Messages must match", errorObject.getMessages(), hasItem(getDefaultMessage()));
        } else {
            assertNull(errorObject.getMessages(), "Message must be null");
        }
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
    })
    void positiveJsonSerializeTest() {
        T errorObject = createErrorObject();
        validateJson(errorObject, getDefaultMessage());
    }

    protected void validateJson(T errorObject, String defaultMessage) {
        validateJson(errorObject, defaultMessage == null
            ? null
            : Collections.singletonList(defaultMessage));
    }


    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    protected void validateJson(final T error, final List<String> messages) {
        try {
            String generatedJson = objectMapper.writeValueAsString(error);

            final String messageListStr;
            if (messages == null || messages.isEmpty()) {
                messageListStr = "null";
            } else {
                List<String> messageList = messages.stream().map(message -> "\"" + message + "\"").toList();
                messageListStr = "[" + StringUtils.join(messageList, ",") + "]";
            }

            String expectedJsonBuilder =
                "{\"code\":\"" + getErrorCode() + "\"" + ",\"messages\":" + messageListStr + "}";

            JSONAssert.assertEquals(expectedJsonBuilder, generatedJson, true);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
