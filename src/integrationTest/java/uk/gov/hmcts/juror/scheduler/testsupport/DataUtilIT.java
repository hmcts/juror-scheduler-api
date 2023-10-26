package uk.gov.hmcts.juror.scheduler.testsupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.StatusUpdate;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobPatch;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;
import uk.gov.hmcts.juror.standard.api.model.auth.LoginRequest;
import uk.gov.hmcts.juror.standard.api.model.auth.RegisterRequest;
import uk.gov.hmcts.juror.standard.api.model.auth.ResetPasswordRequest;
import uk.gov.hmcts.juror.standard.api.model.auth.UserEmailRequest;

import static uk.gov.hmcts.juror.scheduler.testsupport.ConvertUtilIT.asJsonString;

public class DataUtilIT {

    private DataUtilIT() {
    }

    public static String updateJson(String json, String jsonPath, Object replacement) {
        DocumentContext parsed = JsonPath.parse(json);
        parsed.set(jsonPath, replacement);
        return parsed.jsonString();
    }

    public static String loginRequest(String email, String password) throws JsonProcessingException {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        return asJsonString(loginRequest);
    }

    public static String resetPasswordRequest(String email, String password) throws JsonProcessingException {
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        resetPasswordRequest.setEmail(email);
        resetPasswordRequest.setPassword(password);

        return asJsonString(resetPasswordRequest);
    }

    public static String userEmailRequest(String email) throws JsonProcessingException {
        UserEmailRequest userEmailRequest = new UserEmailRequest();
        userEmailRequest.setEmail(email);

        return asJsonString(userEmailRequest);
    }

    public static String apiJobPatchRequest(String payload) throws JsonProcessingException {
        APIJobPatch apiJobPatch = new APIJobPatch();
        apiJobPatch.setPayload(payload);

        return asJsonString(apiJobPatch);
    }

    public static String statusUpdateRequest() throws JsonProcessingException {
        StatusUpdate statusUpdate = new StatusUpdate();
        statusUpdate.setStatus(Status.SUCCESS);

        return asJsonString(statusUpdate);
    }

    public static String registerRequest(String email) throws JsonProcessingException {
        RegisterRequest registerRequest = RegisterRequest.builder()
            .email(email)
            .password("password123")
            .firstname("Myfirstname")
            .lastname("Mylastname")
            .build();

        return asJsonString(registerRequest);
    }
}
