package uk.gov.hmcts.juror.scheduler.api.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        JsonPathValidator.class
    }
)
class JsonPathValidatorTest {

    @MockBean
    private ConstraintValidatorContext context;

    @Autowired
    private JsonPathValidator validator;

    @ParameterizedTest
    @ValueSource(strings = {
        "$.someValue",
        "$[0].someValue",
        "someValue[0].newValue[2]",
        "someValue[0].newValue[2].val",
        "someValue[0].newValue[2]..value",
    })
    @NullSource
    public void positive_valid_json_path(String jsonPath) {
        assertTrue(this.validator.isValid(jsonPath, context));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "$ $",
        "$:.value",
        ".value",
        "....$",
        "  ",
        "#",
    })
    public void negative_invalid_json_path(String jsonPath) {
        assertFalse(this.validator.isValid(jsonPath, context));
    }
}
