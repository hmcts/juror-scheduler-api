package uk.gov.hmcts.juror.scheduler.datastore.entity.api;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        JsonPathAPIValidationEntity.class
    }
)
class JsonPathAPIValidationEntityTest {

    @MockBean
    private Response response;
    @MockBean
    private APIJobDetailsEntity jobData;

    @Autowired
    private JsonPathAPIValidationEntity validationEntity;

    public static Stream<Arguments> jsonPathValidationTestDataProvider() {
        return Stream.of(
            arguments("positive_validate_typical","$.status", "UP", "UP", true),
            arguments("negative_validate_invalid_value","$.status", "UP", "DOWN", false),
            arguments("negative_validate_null_value","$.status", "UP", null, false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("jsonPathValidationTestDataProvider")
    void testRunner(String name, String path, String expectedValue, String returnedValue,
                           boolean expectPass) {
        final JsonPath jsonPath = mock(JsonPath.class);
        validationEntity.setPath(path);
        validationEntity.setExpectedResponse(expectedValue);
        when(response.jsonPath()).thenReturn(jsonPath);
        when(jsonPath.getString(path)).thenReturn(returnedValue);

        APIValidationEntity.Result result = validationEntity.validate(response, jobData);

        if (expectPass) {
            assertNotNull(result,"Result should not be null");
            assertTrue(result.isPassed(),"Result should be passed");
            assertNull(result.getMessage(),"Result must not have message");
        } else {
            assertNotNull(result,"Result must not be null");
            assertFalse(result.isPassed(),"Result must not be passed");
            assertEquals("Expected response to return '" + expectedValue + "' for json path '"
                + path + "' but got '" + returnedValue + "'", result.getMessage(),
                "Message must match");
        }
    }
}
