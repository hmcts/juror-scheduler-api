package uk.gov.hmcts.juror.scheduler.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class APIConstants {

    public static final int DEFAULT_MAX_LENGTH_LONG = 2500;
    public static final int DEFAULT_MAX_LENGTH_SHORT = 250;
    public static final long TASK_ID_MAX = Long.MAX_VALUE;
    public static final long TASK_ID_MIN = 1;
    public static final String JOB_KEY_REGEX = "[A-Z_]{3,50}";
}
