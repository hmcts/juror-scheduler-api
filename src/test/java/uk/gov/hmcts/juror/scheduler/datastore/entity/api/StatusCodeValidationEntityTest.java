package uk.gov.hmcts.juror.scheduler.datastore.entity.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.juror.scheduler.datastore.model.ValidationType;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        StatusCodeValidationEntity.class
    }
)
class StatusCodeValidationEntityTest {
    @MockBean
    private Response response;
    @MockBean
    private APIJobDetailsEntity jobData;

    @Autowired
    private StatusCodeValidationEntity validationEntity;

    public static Stream<Arguments> statusCodeValidationTestDataProvider() {
        return Stream.of(
            arguments("positive_validate_correct_code_200", 200, 200, true),
            arguments("positive_validate_correct_code_201", 201, 201, true),
            arguments("negative_validate_incorrect_code_just_above", 200, 201, false),
            arguments("negative_validate_incorrect_code_just_below", 200, 199, false),
            arguments("negative_validate_incorrect_code", 200, 404, false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("statusCodeValidationTestDataProvider")
    void testRunner(String name, int statusCode, int actualStatusCode,
                    boolean expectPass) {
        validationEntity.setExpectedStatusCode(statusCode);
        when(response.statusCode()).thenReturn(actualStatusCode);

        APIValidationEntity.Result result = validationEntity.validate(response, jobData);
        assertEquals(ValidationType.STATUS_CODE, validationEntity.getType(), "Type must be STATUS_CODE");
        if (expectPass) {
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isPassed(), "Result should be passed");
            assertNull(result.getMessage(), "Result must not have message");
        } else {
            assertNotNull(result, "Result must not be null");
            assertFalse(result.isPassed(), "Result must not be passed");
            assertEquals("Expected status code of " + statusCode + " but got "
                    + actualStatusCode, result.getMessage(),
                "Message must match");
        }
    }
}
