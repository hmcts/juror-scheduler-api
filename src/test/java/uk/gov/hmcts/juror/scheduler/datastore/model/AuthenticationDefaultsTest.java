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
    "scheduler.security.juror-api-service-authentication.id=" + AuthenticationDefaultsTest.id,
    "scheduler.security.juror-api-service-authentication.issuer=" + AuthenticationDefaultsTest.issuer,
    "scheduler.security.juror-api-service-authentication.subject="+ AuthenticationDefaultsTest.subject,
    "scheduler.security.juror-api-service-authentication.token-validity="+AuthenticationDefaultsTest.tokenValidity,
    "scheduler.security.juror-api-service-authentication.secret=" + AuthenticationDefaultsTest.secret
})
class AuthenticationDefaultsTest extends EnumTest<AuthenticationDefaults> {

    static final String id ="JDCRON001";
    static final String issuer ="Scheduler Service";
    static final String subject = "Cron";
    static final long tokenValidity = 160000L;

    static final String secret =
        "RXhhbXBsZVRva2VuMTIzRXhhbXBsZVRva2VuMTIzRXhhbXBsZVRva2VuMTIzRXhhbXBsZVRva2VuMTIzRXhhbXBsZVRva2VuMTIz";


    @MockBean
    private JwtService jwtService;

    @Autowired
    AuthenticationDefaults.AuthenticationDefaultsInjector authenticationDefaultsInjector;

    @MockBean
    RequestSpecification requestSpecification;

    private final String token = "ExampleToken123";

    @Override
    protected Class<AuthenticationDefaults> getEnumClass() {
        return AuthenticationDefaults.class;
    }

    @Override
    protected String getErrorPrefix() {
        return "authentication default";
    }

    @Test
    void positive_verify_none_has_auth_provider_set() {
        assertNotNull(AuthenticationDefaults.NONE.getAuthenticationProvider());
        AuthenticationDefaults.NONE.addAuthentication(APIJobDetailsEntity.builder().build(), requestSpecification);
        verifyNoInteractions(requestSpecification);
    }

    @Captor
    private ArgumentCaptor<Map<String, Object>> claimsCaptor;

    @Test
    void positive_verify_juror_api_service_has_auth_provider_set() {
        assertNotNull(AuthenticationDefaults.JUROR_API_SERVICE.getAuthenticationProvider());

        when(jwtService.generateJwtToken(eq(id), eq(issuer), eq(subject), eq(tokenValidity), any(Key.class), anyMap())).thenReturn(token);

        AuthenticationDefaults.JUROR_API_SERVICE.addAuthentication(APIJobDetailsEntity.builder().build(),
            requestSpecification);

        final ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);

        verify(jwtService, times(1)).generateJwtToken(eq(id), eq(issuer), eq(subject), eq(tokenValidity),
            keyCaptor.capture(),  claimsCaptor.capture());

        Key key = keyCaptor.getValue();
        assertEquals("HmacSHA512",key.getAlgorithm());;
        assertArrayEquals("ExampleToken123ExampleToken123ExampleToken123ExampleToken123ExampleToken123".getBytes(StandardCharsets.UTF_8), key.getEncoded());

        Map<String,Object> claims = claimsCaptor.getValue();
        assertEquals("AUTO",claims.get("login"));
        assertEquals("1",claims.get("userLevel"));
        assertEquals(6,claims.get("daysToExpire"));
        assertEquals(true,claims.get("passwordWarning"));
        assertEquals(5,claims.size());


        Object staffObj = claims.get("staff");
        if(staffObj instanceof Map staffMap){
            assertEquals("AUTO",staffMap.get("name"));
            assertEquals(-1,staffMap.get("rank"));
            assertEquals(1,staffMap.get("active"));
            assertEquals(Collections.emptyList(),staffMap.get("courts"));
            assertEquals(4,staffMap.size());
        }else{
            fail("Claim staff is invalid type");
        }
        verify(requestSpecification, times(1)).header("Authorization", token);
        verifyNoMoreInteractions(requestSpecification);
    }
}
