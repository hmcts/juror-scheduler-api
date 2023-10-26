package uk.gov.hmcts.juror.scheduler.testsupport.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.Information;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.actions.RunJobAction;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetailsResponse;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.JsonPathAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.MaxResponseTimeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.RunJobActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.JsonPathAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.MaxResponseTimeAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.StatusCodeValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;
import uk.gov.hmcts.juror.scheduler.datastore.model.ConditionType;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings({
    "PMD.AvoidThrowingRawExceptionTypes",
    "PMD.TooManyMethods"
})
public final class GenerateUtil {
    private static final Random RANDOM;

    private GenerateUtil() {

    }

    static {
        RANDOM = new Random();
    }

    public static Information generateInformation() {
        Information.InformationBuilder builder = Information.builder();
        builder.name(RandomStringUtils.randomAlphabetic(1, 2500));
        builder.description(RandomStringUtils.randomAlphabetic(1, 2500));
        Set<String> tags = new HashSet<>();
        tags.add(RandomStringUtils.randomAlphabetic(1, 250));
        tags.add(RandomStringUtils.randomAlphabetic(1, 250));
        tags.add(RandomStringUtils.randomAlphabetic(1, 250));
        builder.tags(tags);
        return builder.build();
    }

    public static APIJobDetails generateAPIJobDetails() {
        return APIJobDetails
            .builder()
            .key(RandomStringUtils.randomAlphabetic(3, 25))
            .cronExpression("* 5 * * * ?")
            .information(generateInformation())
            .method(APIMethod.GET)
            .url("www." + RandomStringUtils.randomAlphabetic(10, 25) + ".com")
            .headers(
                Map.of(
                    RandomStringUtils.randomAlphabetic(10, 25), RandomStringUtils.randomAlphabetic(10, 25),
                    RandomStringUtils.randomAlphabetic(10, 25), RandomStringUtils.randomAlphabetic(10, 25)))
            .authenticationDefault(AuthenticationDefaults.NONE)
            .payload(
                "{\"" + RandomStringUtils.randomAlphabetic(10, 25) + "\":\""
                    + RandomStringUtils.randomAlphabetic(10, 25) + "\"}")
            .validations(
                List.of(new StatusCodeAPIValidation(201)))
            .build();
    }

    public static APIJobDetailsResponse generateAPIJobDetailsResponse() {
        return APIJobDetailsResponse
            .builder()
            .createdAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now().plusHours(1))
            .information(generateInformation())
            .method(APIMethod.GET)
            .url("www." + RandomStringUtils.randomAlphabetic(10, 25) + ".com")
            .headers(
                Map.of(RandomStringUtils.randomAlphabetic(10, 25), RandomStringUtils.randomAlphabetic(10, 25),
                    RandomStringUtils.randomAlphabetic(10, 25), RandomStringUtils.randomAlphabetic(10, 25)))
            .authenticationDefault(getRandomEnumValue(AuthenticationDefaults.class))
            .payload(
                "{\"" + RandomStringUtils.randomAlphabetic(10, 25) + "\":\"" + RandomStringUtils.randomAlphabetic(10,
                    25) + "\"}")
            .validations(List.of(new StatusCodeAPIValidation(201)))
            .build();
    }

    public static APIJobDetailsEntity generateAPIJobDetailsEntry() {
        Information information = generateInformation();
        return APIJobDetailsEntity
            .builder()
            .createdAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now().plusHours(1))
            .name(information.getName())
            .description(information.getDescription())
            .tags(information.getTags())
            .method(APIMethod.GET)
            .url("www." + RandomStringUtils.randomAlphabetic(10, 25) + ".com")
            .headers(
                Map.of(RandomStringUtils.randomAlphabetic(10, 25), RandomStringUtils.randomAlphabetic(10, 25),
                    RandomStringUtils.randomAlphabetic(10, 25), RandomStringUtils.randomAlphabetic(10, 25)))
            .authenticationDefault(AuthenticationDefaults.JUROR_API_SERVICE)
            .payload(
                "{\"" + RandomStringUtils.randomAlphabetic(10, 25) + "\":\""
                    + RandomStringUtils.randomAlphabetic(10, 25) + "\"}")
            .validations(List.of(new StatusCodeValidationEntity(201)))
            .postExecutionActions(List.of(new RunJobActionEntity(RandomStringUtils.randomAlphabetic(3, 10))))
            .build();
    }


    public static TaskDetail generateTask() {
        return TaskDetail.builder()
            .jobKey(RandomStringUtils.randomAlphabetic(3, 25))
            .taskId(ThreadLocalRandom.current().nextInt(1, 30_000))
            .createdAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now().plusHours(1))
            .status(getRandomEnumValue(Status.class))
            .message(RandomStringUtils.randomAlphabetic(1, 2500))
            .build();
    }

    private static <T extends Enum<?>> T getRandomEnumValue(Class<T> enumClass) {
        T[] values = enumClass.getEnumConstants();
        return values[RANDOM.nextInt(values.length)];
    }

    public static TaskEntity generateTaskEntity() {
        return TaskEntity.builder()
            .taskId(ThreadLocalRandom.current().nextInt(1, 30_000))
            .job(generateAPIJobDetailsEntrySimple())
            .message(RandomStringUtils.randomAlphabetic(1, 2500))
            .postActionsMessage(RandomStringUtils.randomAlphabetic(1, 2500))
            .metaData(Map.of(
                RandomStringUtils.randomAlphabetic(1, 250), RandomStringUtils.randomAlphabetic(1, 250),
                RandomStringUtils.randomAlphabetic(1, 250), RandomStringUtils.randomAlphabetic(1, 250),
                RandomStringUtils.randomAlphabetic(1, 250), RandomStringUtils.randomAlphabetic(1, 250)
            ))
            .status(getRandomEnumValue(Status.class))
            .createdAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now().plusHours(1))
            .build();
    }

    private static APIJobDetailsEntity generateAPIJobDetailsEntrySimple() {
        return APIJobDetailsEntity.builder()
            .key(RandomStringUtils.randomAlphabetic(3, 25))
            .build();
    }

    public static StatusCodeAPIValidation generateStatusCodeAPIValidation() {
        return new StatusCodeAPIValidation(RandomUtils.nextInt(100, 600));
    }

    public static StatusCodeValidationEntity generateStatusCodeValidationEntity() {
        return (StatusCodeValidationEntity) new StatusCodeValidationEntity(RandomUtils.nextInt(100, 600))
            .setJob(generateAPIJobDetailsEntrySimple());
    }


    public static MaxResponseTimeAPIValidation generateMaxResponseTimeAPIValidation() {
        return new MaxResponseTimeAPIValidation(RandomUtils.nextInt(1, 30_001));
    }

    public static MaxResponseTimeAPIValidationEntity generateMaxResponseTimeAPIValidationEntity() {
        return (MaxResponseTimeAPIValidationEntity) new MaxResponseTimeAPIValidationEntity(
            RandomUtils.nextInt(1, 30_001))
            .setJob(generateAPIJobDetailsEntrySimple());
    }

    public static JsonPathAPIValidation generateJsonPathAPIValidation() {
        return new JsonPathAPIValidation(
            RandomStringUtils.randomAlphabetic(250),
            RandomStringUtils.randomAlphabetic(250));
    }

    public static JsonPathAPIValidationEntity generateJsonPathAPIValidationEntity() {
        return (JsonPathAPIValidationEntity) new JsonPathAPIValidationEntity(
            RandomStringUtils.randomAlphabetic(250),
            RandomStringUtils.randomAlphabetic(250))
            .setJob(generateAPIJobDetailsEntrySimple());
    }

    public static RunJobAction generateRunJobAction() {
        return (RunJobAction) new RunJobAction(RandomStringUtils.randomAlphabetic(3, 25))
            .setJobKey(RandomStringUtils.randomAlphabetic(3, 25))
            .setCondition(getRandomEnumValue(ConditionType.class));
    }


    public static RunJobActionEntity generateRunJobActionEntity() {
        return (RunJobActionEntity) new RunJobActionEntity(RandomStringUtils.randomAlphabetic(3, 25))
            .setJobKey(RandomStringUtils.randomAlphabetic(3, 25))
            .setJob(generateAPIJobDetailsEntrySimple())
            .setCondition(getRandomEnumValue(ConditionType.class));
    }
}
