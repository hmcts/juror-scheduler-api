package uk.gov.hmcts.juror.scheduler.controllers;

import com.jayway.jsonpath.JsonPath;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URI;

import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.juror.scheduler.testsupport.DataUtilIT.loginRequest;
import static uk.gov.hmcts.juror.scheduler.testsupport.DataUtilIT.resetPasswordRequest;


@ActiveProfiles({"test"})
public abstract class AbstractIT {
    public static final String API_DUMMY_CRON_JOB_JSON = "apiDummyCronJob.json";
    public static final String ADMIN_EMAIL = "admin@scheduler.cgi.com";
    public static final String ADMIN_PASSWORD_ENCRYPTED =
        "kj3TXdvYqmFTXXTq!9nA7ZUmDgiQ&W7Z&v7mnFyp2bvM&BZ#nPosFfL8zNvw";
    public static final String URL_JOBS_API = "/jobs/api";

    private static final String USER_PASSWORD = "testPassword123";
    private final MockMvc mockMvc;

    protected AbstractIT(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public ResultActions mockMvcPerform(@NotNull String url,
                                        @NotNull String jwt,
                                        @NotNull HttpMethod httpMethod,
                                        @NotNull String jsonContent) throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.request(
            httpMethod, new URI(url));

        if (!jwt.isEmpty()) {
            mockHttpServletRequestBuilder
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }

        if (!jsonContent.isEmpty()) {
            mockHttpServletRequestBuilder.content(jsonContent);
        }

        return mockMvc.perform(mockHttpServletRequestBuilder
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON));
    }

    public String generateJwt(String loginRequest) throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders
                .post("/auth/login")
                .content(loginRequest)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jwt").isNotEmpty())
            .andReturn();

        return JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.jwt");
    }

    public String getUserJwt(String userEmail, String jwtAdmin) throws Exception {
        //Admin user needs to reset password before user can log in the first time
        resetUserPassword(userEmail, jwtAdmin);

        //A jwt can now be generated for the user
        String loginRequest = loginRequest(userEmail, USER_PASSWORD);
        return generateJwt(loginRequest);
    }

    public void resetUserPassword(String userEmail, String jwtAdmin) throws Exception {
        mockMvcPerform("/auth/user/reset_password", jwtAdmin, PUT,
            resetPasswordRequest(userEmail, USER_PASSWORD))
            .andExpect(status().isAccepted());
    }
}
