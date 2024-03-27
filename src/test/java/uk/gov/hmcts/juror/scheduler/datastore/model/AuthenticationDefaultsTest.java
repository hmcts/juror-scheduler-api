package uk.gov.hmcts.juror.scheduler.datastore.model;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.standard.service.contracts.auth.JwtService;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        AuthenticationDefaults.AuthenticationDefaultsInjector.class
    }
)
@SpringBootTest(properties = {
    "scheduler.security.juror-api-service-authentication.id=" + AuthenticationDefaultsTest.ID,
    "scheduler.security.juror-api-service-authentication.issuer=" + AuthenticationDefaultsTest.ISSUER,
    "scheduler.security.juror-api-service-authentication.subject=" + AuthenticationDefaultsTest.SUJECT,
    "scheduler.security.juror-api-service-authentication.token-validity=" + AuthenticationDefaultsTest.TOKEN_VALIDITY,
    "scheduler.security.juror-api-service-authentication.secret=" + AuthenticationDefaultsTest.SECRET
})
@SuppressWarnings({"PMD.LawOfDemeter"})
class AuthenticationDefaultsTest extends EnumTest<AuthenticationDefaults> {

    static final String ID = "JDCRON001";
    static final String ISSUER = "Scheduler Service";
    static final String SUJECT = "Cron";
    static final long TOKEN_VALIDITY = 160_000L;

    @Captor
    private ArgumentCaptor<Map<String, Object>> claimsCaptor;

    static final String SECRET =
        "RXhhbXBsZVRva2VuMTIzRXhhbXBsZVRva2VuMTIzRXhhbXBsZVRva2VuMTIzRXhhbXBsZVRva2VuMTIzRXhhbXBsZVRva2VuMTIz";


    @MockBean
    private JwtService jwtService;

    @Autowired
    AuthenticationDefaults.AuthenticationDefaultsInjector authenticationDefaultsInjector;

    @MockBean
    RequestSpecification requestSpecification;

    private static final String TOKEN = "ExampleToken123";

    @Override
    protected Class<AuthenticationDefaults> getEnumClass() {
        return AuthenticationDefaults.class;
    }

    @Override
    protected String getErrorPrefix() {
        return "authentication default";
    }

    @Test
    void positiveVerifyNoneHasAuthProviderSet() {
        assertNotNull(AuthenticationDefaults.NONE.getAuthenticationProvider(),
            "Authentication provider should not be null");
        AuthenticationDefaults.NONE.addAuthentication(APIJobDetailsEntity.builder().build(), requestSpecification);
        verifyNoInteractions(requestSpecification);
    }


    @Test
    void positiveVerifyJurorApiServiceHasAuthProviderSet() {
        assertNotNull(AuthenticationDefaults.JUROR_API_SERVICE.getAuthenticationProvider(),
            "Authentication provider must not be null");

        when(jwtService.generateJwtToken(eq(ID), eq(ISSUER), eq(SUJECT), eq(TOKEN_VALIDITY), any(Key.class),
            anyMap())).thenReturn(TOKEN);

        AuthenticationDefaults.JUROR_API_SERVICE.addAuthentication(APIJobDetailsEntity.builder().build(),
            requestSpecification);

        final ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);

        verify(jwtService, times(1)).generateJwtToken(eq(ID), eq(ISSUER), eq(SUJECT), eq(TOKEN_VALIDITY),
            keyCaptor.capture(), claimsCaptor.capture());

        Key key = keyCaptor.getValue();
        assertEquals("HmacSHA512", key.getAlgorithm(), "Algorithm must match");
        assertArrayEquals("ExampleToken123ExampleToken123ExampleToken123ExampleToken123ExampleToken123".getBytes(
            StandardCharsets.UTF_8), key.getEncoded(), "Encoded key must match");

        Map<String, Object> claims = claimsCaptor.getValue();
        assertEquals("AUTO", claims.get("login"), "Login must match");
        assertEquals("1", claims.get("userLevel"), "User level must match");
        assertEquals("SYSTEM", claims.get("userType"), "User type must match");
        assertEquals(6, claims.get("daysToExpire"), "Days to expire must match");
        assertEquals(true, claims.get("passwordWarning"), "Password warning must match");
        assertEquals(6, claims.size(), "Claim size must match");

        Object staffObj = claims.get("staff");
        if (staffObj instanceof Map staffMap) {
            assertEquals("AUTO", staffMap.get("name"), "Name must match");
            assertEquals(-1, staffMap.get("rank"), "Rank must match");
            assertEquals(1, staffMap.get("active"), "Active must match");
            assertEquals(Collections.emptyList(), staffMap.get("courts"), "Courts must match");
            assertEquals(4, staffMap.size(), "Staff Size should match");
        } else {
            fail("Claim staff is invalid type");
        }
        verify(requestSpecification, times(1)).header("Authorization", TOKEN);
        verifyNoMoreInteractions(requestSpecification);
    }
}
