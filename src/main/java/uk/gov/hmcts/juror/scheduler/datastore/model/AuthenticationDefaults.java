package uk.gov.hmcts.juror.scheduler.datastore.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.restassured.specification.RequestSpecification;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.standard.service.contracts.auth.JwtService;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;
import uk.gov.hmcts.juror.standard.service.exceptions.InvalidEnumValueException;

import java.security.Key;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public enum AuthenticationDefaults {
    NONE,
    JUROR_API_SERVICE,
    API_JOB_EXECUTION_SERVICE;

    @Getter(AccessLevel.PACKAGE)
    private BiConsumer<APIJobDetailsEntity, RequestSpecification> authenticationProvider;

    private void setAuthenticationProvider(
        BiConsumer<APIJobDetailsEntity, RequestSpecification> authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    public void addAuthentication(APIJobDetailsEntity apiJobDetailsEntity, RequestSpecification requestSpecification) {
        getAuthenticationProvider().accept(apiJobDetailsEntity, requestSpecification);
    }

    @JsonCreator
    @SuppressWarnings("PMD.PreserveStackTrace")
    public static AuthenticationDefaults forValues(String value) {
        try {
            return valueOf(value);
        } catch (Exception e) {
            throw new InvalidEnumValueException(
                "Invalid authentication default entered. Allowed values are: " + Arrays.toString(
                    AuthenticationDefaults.values()));
        }
    }

    @Component
    public static class AuthenticationDefaultsInjector {
        private final JwtService jwtService;

        @Value("${scheduler.security.juror-api-service-authentication.id}")
        private String jurorApiServiceJwtId;

        @Value("${scheduler.security.juror-api-service-authentication.issuer}")
        private String jurorApiServiceJwtIssuer;

        @Value("${scheduler.security.juror-api-service-authentication.subject}")
        private String jurorApiServiceJwtSubject;

        @Value("${scheduler.security.juror-api-service-authentication.token-validity}")
        private Long jurorApiServiceJwtTokenValidity;

        @Value("${scheduler.security.juror-api-service-authentication.secret}")
        private String jurorApiServiceJwtSecret;


        @Value("${scheduler.security.api-job-execution-service-authentication.issuer}")
        private String apiJobExecutionServiceJwtIssuer;

        @Value("${scheduler.security.api-job-execution-service-authentication.subject}")
        private String apiJobExecutionServiceJwtSubject;

        @Value("${scheduler.security.api-job-execution-service-authentication.token-validity}")
        private Long apiJobExecutionServiceJwtTokenValidity;

        @Value("${scheduler.security.api-job-execution-service-authentication.secret}")
        private String apiJobExecutionServiceJwtSecret;

        @Autowired
        public AuthenticationDefaultsInjector(JwtService jwtService) {
            this.jwtService = jwtService;
        }

        @PostConstruct
        public void postConstruct() {
            NONE.setAuthenticationProvider((apiJobDetailsEntity, requestSpecification) -> {
            });
            JUROR_API_SERVICE.setAuthenticationProvider(getJurorApiServiceAuthenticationProvider());
            API_JOB_EXECUTION_SERVICE.setAuthenticationProvider(getApiJobExecutionServiceAuthenticationProvider());

            //Validate all enum values have had their providers set
            for (AuthenticationDefaults value : AuthenticationDefaults.values()) {
                if (value.authenticationProvider == null) {
                    throw new InternalServerException("Authentication provider not set for: " + value);
                }
            }
        }

        private BiConsumer<APIJobDetailsEntity, RequestSpecification>
            getApiJobExecutionServiceAuthenticationProvider() {
            return (apiJobDetailsEntity, requestSpecification) -> {
                final Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(apiJobExecutionServiceJwtSecret));

                final String token = jwtService.generateJwtToken(null, apiJobExecutionServiceJwtIssuer,
                    apiJobExecutionServiceJwtSubject,
                    apiJobExecutionServiceJwtTokenValidity, key,
                    Map.of("permissions", new String[]{"job::trigger"})
                );
                requestSpecification.header("Authorization", "Bearer " + token);
            };
        }

        private BiConsumer<APIJobDetailsEntity, RequestSpecification> getJurorApiServiceAuthenticationProvider() {
            return (apiJobDetailsEntity, requestSpecification) -> {
                final Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jurorApiServiceJwtSecret));
                final Map<String, Object> claims = new ConcurrentHashMap<>();
                claims.put("login", "AUTO");   //cronuser
                claims.put("userLevel", "1");
                claims.put("userType", "SYSTEM");
                claims.put("daysToExpire", 6);
                claims.put("passwordWarning", true);

                final Map<String, Object> staff = new ConcurrentHashMap<>();
                staff.put("name", "AUTO"); //cron user
                staff.put("rank", -1);
                staff.put("active", 1);
                staff.put("courts", Collections.emptyList());

                claims.put("staff", staff);

                final String token = jwtService.generateJwtToken(jurorApiServiceJwtId, jurorApiServiceJwtIssuer,
                    jurorApiServiceJwtSubject,
                    jurorApiServiceJwtTokenValidity, key, claims);
                requestSpecification.header("Authorization", token);
            };
        }
    }

}
