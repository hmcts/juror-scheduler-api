package uk.gov.hmcts.juror.scheduler.datastore.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.standard.service.exceptions.InvalidEnumValueException;

import java.util.Arrays;
import java.util.Set;

public enum ConditionType {

    ON_PENDING(Set.of(statusIs(Status.PENDING))),
    ON_VALIDATION_PASSED(Set.of(statusIs(Status.VALIDATION_PASSED))),
    ON_VALIDATION_FAILED(Set.of(statusIs(Status.VALIDATION_FAILED))),
    ON_PROGRESSING(Set.of(statusIs(Status.PROGRESSING))),
    ON_FAILED_UNEXPECTED_EXCEPTION(Set.of(statusIs(Status.FAILED_UNEXPECTED_EXCEPTION))),
    ON_SUCCESS(Set.of(statusIs(Status.SUCCESS))),
    ON_FAILED(Set.of(statusIs(Status.FAILED))),
    ON_INDETERMINATE(Set.of(statusIs(Status.INDETERMINATE)));

    private final Set<Listener<?>> listeners;

    ConditionType(Set<Listener<?>> listeners) {
        this.listeners = listeners;
    }

    @JsonCreator
    @SuppressWarnings("PMD.PreserveStackTrace")
    public static ConditionType forValues(String value) {
        try {
            return valueOf(value);
        } catch (Exception e) {
            throw new InvalidEnumValueException(
                "Invalid condition type entered. Allowed values are: "
                    + Arrays.toString(ConditionType.values()));
        }
    }

    public <T> boolean isMet(Class<? extends Listener<T>> listenerClass, T value) {
        return this.listeners.stream()
            .anyMatch(listener -> {
                if (!listenerClass.isInstance(listener)) {
                    return false;
                }
                return listenerClass.cast(listener).isMet(value);
            });
    }

    private static TaskEntityChangedListener statusIs(final Status status) {
        return taskEntity -> taskEntity.getStatus() == status;
    }

    @FunctionalInterface
    public interface TaskEntityChangedListener extends Listener<TaskEntity> {

    }

    @FunctionalInterface
    public interface Listener<T> {
        boolean isMet(T value);
    }
}
