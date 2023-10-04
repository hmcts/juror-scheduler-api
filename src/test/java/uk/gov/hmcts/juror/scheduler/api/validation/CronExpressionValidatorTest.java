package uk.gov.hmcts.juror.scheduler.api.validation;


import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        CronExpressionValidator.class
    }
)
class CronExpressionValidatorTest {


    @MockBean
    private ConstraintValidatorContext context;
    @MockBean
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    @Autowired
    private CronExpressionValidator validator;

    @BeforeEach
    public void before(){
        when(context.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilder);
    }

    //https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html
    @ParameterizedTest
    @ValueSource(strings = {
        "0 0 12 * * ?",//	Fire at 12pm (noon) every day
        "0 15 10 ? * *",//	Fire at 10:15am every day
        "0 15 10 * * ?",//	Fire at 10:15am every day
        "0 15 10 * * ? *",//	Fire at 10:15am every day
        "0 15 10 * * ? 2005",//	Fire at 10:15am every day during the year 2005
        "0 * 14 * * ?",//	Fire every minute starting at 2pm and ending at 2:59pm, every day
        "0 0/5 14 * * ?",//	Fire every 5 minutes starting at 2pm and ending at 2:55pm, every day
        "0 0/5 14,18 * * ?",//	Fire every 5 minutes starting at 2pm and ending at 2:55pm, AND fire every 5 minutes starting at 6pm and ending at 6:55pm, every day
        "0 0-5 14 * * ?",//	Fire every minute starting at 2pm and ending at 2:05pm, every day
        "0 10,44 14 ? 3 WED",//	Fire at 2:10pm and at 2:44pm every Wednesday in the month of March.
        "0 15 10 ? * MON-FRI",//	Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday
        "0 15 10 15 * ?",//	Fire at 10:15am on the 15th day of every month
        "0 15 10 L * ?",//	Fire at 10:15am on the last day of every month
        "0 15 10 L-2 * ?",//	Fire at 10:15am on the 2nd-to-last last day of every month
        "0 15 10 ? * 6L",//	Fire at 10:15am on the last Friday of every month
        "0 15 10 ? * 6L",//	Fire at 10:15am on the last Friday of every month
        "0 15 10 ? * 6L",// 2002-2005	Fire at 10:15am on every last friday of every month during the years 2002, 2003, 2004 and 2005
        "0 15 10 ? * 6#3",//	Fire at 10:15am on the third Friday of every month
        "0 0 12 1/5 * ?",//	Fire at 12pm (noon) every 5 days every month, starting on the first day of the month.
        "0 11 11 11 11 ?",// Fire every November 11th at 11:11am.
        "0/5 14,18,3-39,52 * ? JAN,MAR,SEP MON-FRI 2002-2010",
        "* * * * * ?",
        "* * * ? * *"
    })
    @NullSource
    void positive_valid_cron_expressions(String cronExpression) {
        assertTrue(this.validator.isValid(cronExpression, context));
        verify(context, never()).disableDefaultConstraintViolation();
        verify(context, never()).buildConstraintViolationWithTemplate(any());
        verify(constraintViolationBuilder, never()).addConstraintViolation();
    }

    private static Stream<Arguments> provideInvalidCronExpressions() {
        return Stream.of(
            arguments("? * * * * *", "'?' can only be specified for Day-of-Month or Day-of-Week."),
            arguments("* ? * * * *", "'?' can only be specified for Day-of-Month or Day-of-Week."),
            arguments("* * ? * * *", "'?' can only be specified for Day-of-Month or Day-of-Week."),
            arguments("* * * * ? *", "'?' can only be specified for Day-of-Month or Day-of-Week."),
            arguments("INVALID", "Illegal characters for this position: 'INV'"),
            arguments("* * * * *", "Unexpected end of expression."),
            arguments("* * * * * *", "Support for specifying both a day-of-week AND a day-of-month parameter is not " +
                "implemented."),
            arguments("0 10,44 14 ? 3 WEN","Invalid Day-of-Week value: 'WEN'")

        );
    }
    @ParameterizedTest
    @MethodSource("provideInvalidCronExpressions")
    void negative_invalid_cron_expressions(String cronExpression, String expectedErrorMessage) {
        assertFalse(this.validator.isValid(cronExpression, context));
        verify(context, times(1)).disableDefaultConstraintViolation();
        verify(context, times(1)).buildConstraintViolationWithTemplate(eq("Invalid Cron Expression: " + expectedErrorMessage));
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }
}
