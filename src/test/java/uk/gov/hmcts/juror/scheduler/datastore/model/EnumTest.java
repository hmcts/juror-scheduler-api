package uk.gov.hmcts.juror.scheduler.datastore.model;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.juror.standard.service.exceptions.InvalidEnumValueException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class EnumTest<T extends Enum<T>> {


    protected abstract Class<T> getEnumClass();

    protected abstract String getErrorPrefix();

    public Stream<Arguments> validEnumOptionsProvider() {
        Stream.Builder<Arguments> builder = Stream.builder();
        for (T enumConstant : getEnumClass().getEnumConstants()) {
            builder.add(arguments(enumConstant));
        }
        return builder.build();
    }

    @ParameterizedTest(name = "positive_convert_valid_String: {0}")
    @MethodSource("validEnumOptionsProvider")
    void positiveConvertValidString(T value) throws Throwable {
        assertEquals(value, invokeForValuesMethod(value.name()), "Value must match");
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID_", "09123_d%"})
    @NullAndEmptySource
    void negativeInvalidEnumValue(String value) {
        InvalidEnumValueException exception = assertThrows(InvalidEnumValueException.class,
            () -> invokeForValuesMethod(value));

        assertEquals("Invalid " + getErrorPrefix() + " entered. Allowed values are: " + Arrays.toString(
                getEnumClass().getEnumConstants()),
            exception.getMessage(),
            "Value must match");

    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    protected Object invokeForValuesMethod(String value) throws Throwable {
        try {
            return getForValuesMethod().invoke(null, value);
        } catch (IllegalAccessException e) {
            fail("Failed to invoke forValues method");
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    protected Method getForValuesMethod() {
        try {
            return getEnumClass().getMethod("forValues", String.class);
        } catch (NoSuchMethodException noSuchMethodException) {
            fail("Could not find method: forValues(String.class)");
            throw new RuntimeException(noSuchMethodException);
        }
    }
}
