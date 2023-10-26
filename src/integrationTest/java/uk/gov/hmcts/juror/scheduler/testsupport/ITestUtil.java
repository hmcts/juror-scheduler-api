package uk.gov.hmcts.juror.scheduler.testsupport;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

public final class ITestUtil {
    private static final String USER_EMAIL_PREFIX = "user";
    private static final String USER_EMAIL_POSTFIX = "@cgi.com";
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    private ITestUtil() {
    }

    public static String getTestDataAsStringFromFile(String fileName) throws IOException {
        File resource = new ClassPathResource("testdata/" + fileName).getFile();
        return new String(Files.readAllBytes(resource.toPath()));
    }

    public static int getNextUniqueIndex() {
        return ATOMIC_INTEGER.incrementAndGet();
    }

    public static String dynamicEmailGenerator(int uniqueId) {
        return USER_EMAIL_PREFIX + uniqueId + USER_EMAIL_POSTFIX;
    }
}
