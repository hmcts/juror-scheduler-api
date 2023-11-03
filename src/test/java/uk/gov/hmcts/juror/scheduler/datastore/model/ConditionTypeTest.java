package uk.gov.hmcts.juror.scheduler.datastore.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SuppressWarnings({
    "PMD.TestClassWithoutTestCases" //False positive done via inheritance
})
class ConditionTypeTest extends EnumTest<ConditionType> {
    public static final Map<ConditionType, Status> ALLOWED_STATUES = Map.of(
        ConditionType.ON_PENDING, Status.PENDING,
        ConditionType.ON_VALIDATION_PASSED, Status.VALIDATION_PASSED,
        ConditionType.ON_VALIDATION_FAILED, Status.VALIDATION_FAILED,
        ConditionType.ON_PROGRESSING, Status.PROGRESSING,
        ConditionType.ON_FAILED_UNEXPECTED_EXCEPTION, Status.FAILED_UNEXPECTED_EXCEPTION,
        ConditionType.ON_SUCCESS, Status.SUCCESS,
        ConditionType.ON_PARTIAL_SUCCESS, Status.PARTIAL_SUCCESS,
        ConditionType.ON_FAILED, Status.FAILED,
        ConditionType.ON_INDETERMINATE, Status.INDETERMINATE
    );

    @Override
    protected Class<ConditionType> getEnumClass() {
        return ConditionType.class;
    }

    @Override
    protected String getErrorPrefix() {
        return "condition type";
    }


    public static Stream<Arguments> statusConditionArguments() {
        Stream.Builder<Arguments> builder = Stream.builder();
        ALLOWED_STATUES.forEach((key, value) -> builder.add(arguments(key, value)));
        return builder.build();
    }

    public static Stream<Arguments> invalidStatusConditionArguments() {
        Stream.Builder<Arguments> builder = Stream.builder();

        ALLOWED_STATUES.forEach((key, value) -> {
            for (Status status : Status.values()) {
                if (status != value) {
                    builder.add(arguments(key, status));
                }
            }
        });
        return builder.build();
    }

    @ParameterizedTest(name = "Positive: isMet condition type: {0} should pass when given status {1}")
    @MethodSource("statusConditionArguments")
    void positiveIsMet(ConditionType conditionType, Status allowedStatus) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setStatus(allowedStatus);
        assertTrue(conditionType
            .isMet(ConditionType.TaskEntityChangedListener.class,
                taskEntity), "Condition should be met");
    }

    @ParameterizedTest(name = "Negative: isMet condition type: {0} should fail when given status {1}")
    @MethodSource("invalidStatusConditionArguments")
    void negativeIsNotMet(ConditionType conditionType, Status disallowedStatus) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setStatus(disallowedStatus);
        assertFalse(conditionType
            .isMet(ConditionType.TaskEntityChangedListener.class,
                taskEntity), "Condition should not be met");
    }

    @ParameterizedTest(name = "Negative: isMet condition type: {0} should fail if given the wrong type of lisener")
    @MethodSource("statusConditionArguments")
    void negativeOnSuccessWrongListenerType(ConditionType conditionType, Status allowedStatus) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setStatus(allowedStatus);

        assertFalse(conditionType
                .isMet(TestChangedListener.class, taskEntity),
            "Condition should not be met");
    }


    @FunctionalInterface
    public interface TestChangedListener extends ConditionType.Listener<TaskEntity> {

    }
}
