package uk.gov.hmcts.juror.scheduler.testsupport;


import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.FileCopyUtils;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.Information;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetailsResponse;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.JsonPathAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.MaxResponseTimeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.JsonPathAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.MaxResponseTimeAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.StatusCodeValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({
    "unchecked",
    "PMD.ExcessiveImports",
    "PMD.TooManyMethods",
    "PMD.TestClassWithoutTestCases"//False positive - support class
})
public final class TestUtil {

    private static final Random RANDOM;

    static {
        RANDOM = new Random();
    }

    private TestUtil() {

    }

    @SneakyThrows
    public static String readResource(String url, String... prefixes) {

        try (Reader reader = new InputStreamReader(getUrl(url, prefixes).openStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static URL getUrl(String url, String... prefixes) {
        String prefix = String.join("/", prefixes);
        return Objects.requireNonNull(TestUtil.class.getResource(String.join("/", prefix, url)));
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

    public static <T extends Enum<?>> String[] getEnumNames(Class<T> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }


    public static APIJobDetailsEntity cloneAPIJobDetailsEntity(APIJobDetailsEntity baseApiJobDetailsEntity) {
        return APIJobDetailsEntity.builder()
            .key(baseApiJobDetailsEntity.getKey())
            .cronExpression(baseApiJobDetailsEntity.getCronExpression())
            .method(baseApiJobDetailsEntity.getMethod())
            .name(baseApiJobDetailsEntity.getName())
            .description(baseApiJobDetailsEntity.getDescription())
            .tags(cloneSet(baseApiJobDetailsEntity.getTags()))
            .url(baseApiJobDetailsEntity.getUrl())
            .payload(baseApiJobDetailsEntity.getPayload())
            .authenticationDefault(baseApiJobDetailsEntity.getAuthenticationDefault())
            .createdAt(baseApiJobDetailsEntity.getCreatedAt())
            .lastUpdatedAt(baseApiJobDetailsEntity.getLastUpdatedAt())
            .headers(cloneMap(baseApiJobDetailsEntity.getHeaders()))
            .validations(cloneValidations(baseApiJobDetailsEntity.getValidations()))
            .build();
    }

    private static List<APIValidationEntity> cloneValidations(List<APIValidationEntity> validations) {
        List<APIValidationEntity> list = new ArrayList<>();
        for (APIValidationEntity validation : validations) {
            list.add(cloneValidation(validation));
        }
        return list;
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private static APIValidationEntity cloneValidation(APIValidationEntity validation) {
        if (validation instanceof StatusCodeValidationEntity statusCodeValidation) {
            return new StatusCodeValidationEntity(statusCodeValidation.getExpectedStatusCode());
        }
        if (validation instanceof MaxResponseTimeAPIValidationEntity maxResponseTimeAPIValidation) {
            return new StatusCodeValidationEntity(maxResponseTimeAPIValidation.getMaxResponseTimeMS());
        }
        if (validation instanceof JsonPathAPIValidationEntity jsonPathAPIValidation) {
            return new JsonPathAPIValidationEntity(jsonPathAPIValidation.getPath(),
                jsonPathAPIValidation.getExpectedResponse());
        }
        throw new RuntimeException("Unknown validation type: " + validation.getClass());
    }

    public static List<APIValidationEntity> convertValidations(List<? extends APIValidation> validations) {
        List<APIValidationEntity> list = new ArrayList<>();
        for (APIValidation validation : validations) {
            list.add(convertValidation(validation));
        }
        return list;
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    public static APIValidationEntity convertValidation(APIValidation validation) {
        if (validation instanceof StatusCodeAPIValidation statusCodeAPIValidation) {
            return new StatusCodeValidationEntity(statusCodeAPIValidation.getExpectedStatusCode());
        }
        if (validation instanceof MaxResponseTimeAPIValidation maxResponseTimeAPIValidation) {
            return new MaxResponseTimeAPIValidationEntity(maxResponseTimeAPIValidation.getMaxResponseTimeMS());
        }
        if (validation instanceof JsonPathAPIValidation jsonPathAPIValidation) {
            return new JsonPathAPIValidationEntity(jsonPathAPIValidation.getPath(),
                jsonPathAPIValidation.getExpectedResponse());
        }
        throw new RuntimeException("Unknown validation type: " + validation.getClass());
    }

    private static Map<String, String> cloneMap(Map<String, String> headers) {
        return new HashMap<>(headers);
    }

    private static Set<String> cloneSet(Set<String> tags) {
        return new HashSet<>(tags);
    }

    public static ResultMatcher jsonMatcher(JSONCompareMode mode, String expectedPayload) {
        return result -> {
            String actual = result.getResponse().getContentAsString();
            JSONAssert.assertEquals(
                "",
                expectedPayload,
                actual,
                mode
            );
        };
    }

    public static <T> Stream<T> concat(Stream<T>... streams) {
        Stream<T> mainStream = null;
        for (Stream<T> stream : streams) {
            if (mainStream == null) {
                mainStream = stream;
            } else {
                mainStream = Stream.concat(mainStream, stream);
            }
        }
        return mainStream;
    }

    public static String replaceJsonPath(String json, String jsonPath, Object replacement) {
        DocumentContext parsed = JsonPath.parse(json);
        parsed.set(jsonPath, replacement);
        return parsed.jsonString();
    }

    public static String addJsonPath(String json, String jsonPath, String key, Object value) {
        DocumentContext parsed = JsonPath.parse(json);
        parsed.put(jsonPath, key, value);
        return parsed.jsonString();
    }

    public static String deleteJsonPath(String json, String jsonPath) {
        DocumentContext parsed = JsonPath.parse(json);
        parsed.delete(jsonPath);
        return parsed.jsonString();
    }

    public static void isUnmodifiable(Collection<?> collection) {
        assertThrows(UnsupportedOperationException.class,
            () -> collection.add(null));
    }
}
