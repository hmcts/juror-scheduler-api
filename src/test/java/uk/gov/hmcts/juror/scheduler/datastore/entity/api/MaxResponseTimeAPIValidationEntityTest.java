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
        MaxResponseTimeAPIValidationEntity.class
    }
)
class MaxResponseTimeAPIValidationEntityTest {
    @MockBean
    private Response response;
    @MockBean
    private APIJobDetailsEntity jobData;

    @Autowired
    private MaxResponseTimeAPIValidationEntity validationEntity;

    public static Stream<Arguments> maxResponseTimeValidationTestDataProvider() {
        return Stream.of(
            arguments("positive_validate_on_max", 300, 300L, true),
            arguments("positive_validate_just_below_max", 300, 299L, true),
            arguments("negative_validate_just_over_max", 300, 301L, false),
            arguments("negative_validate_well_over_max", 100, 3000L, false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("maxResponseTimeValidationTestDataProvider")
    void testRunner(String name, int maxResponseTime, long actualResponseTime,
                           boolean expectPass) {
        validationEntity.setMaxResponseTimeMS(maxResponseTime);
        when(response.getTime()).thenReturn(actualResponseTime);

        APIValidationEntity.Result result = validationEntity.validate(response, jobData);
        assertEquals(ValidationType.MAX_RESPONSE_TIME, validationEntity.getType(), "Type must be MAX_RESPONSE_TIME");

        if (expectPass) {
            assertNotNull(result,"Result should not be null");
            assertTrue(result.isPassed(),"Result should be passed");
            assertNull(result.getMessage(),"Result must not have message");
        } else {
            assertNotNull(result,"Result must not be null");
            assertFalse(result.isPassed(),"Result must not be passed");
            assertEquals(
                "API call took longer then the max response time allowed. Max response time: "
                    + maxResponseTime + " ms but took: " + actualResponseTime + " ms",
                result.getMessage(),
                "Message must match");
        }
    }
}
