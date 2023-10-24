package uk.gov.hmcts.juror.scheduler.testsupport.util;

import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.Information;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetailsResponse;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.RunJobActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.StatusCodeValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings({
    "PMD.AvoidThrowingRawExceptionTypes"
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
}
