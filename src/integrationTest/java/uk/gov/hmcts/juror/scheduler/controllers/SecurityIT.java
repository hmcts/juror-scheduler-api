package uk.gov.hmcts.juror.scheduler.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.juror.scheduler.config.PermissionConstants;
import uk.gov.hmcts.juror.standard.api.model.auth.AssignPermissionsRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.juror.scheduler.config.PermissionConstants.Job.API_CREATE;
import static uk.gov.hmcts.juror.scheduler.config.PermissionConstants.Job.API_UPDATE;
import static uk.gov.hmcts.juror.scheduler.config.PermissionConstants.Job.DELETE;
import static uk.gov.hmcts.juror.scheduler.config.PermissionConstants.Job.DISABLE;
import static uk.gov.hmcts.juror.scheduler.config.PermissionConstants.Job.ENABLE;
import static uk.gov.hmcts.juror.scheduler.config.PermissionConstants.Job.RUN;
import static uk.gov.hmcts.juror.scheduler.config.PermissionConstants.Job.SEARCH;
import static uk.gov.hmcts.juror.scheduler.config.PermissionConstants.Job.VIEW;
import static uk.gov.hmcts.juror.scheduler.config.PermissionConstants.Job.VIEW_STATUS;
import static uk.gov.hmcts.juror.scheduler.testsupport.ConvertUtilIT.asJsonString;
import static uk.gov.hmcts.juror.scheduler.testsupport.DataUtilIT.apiJobPatchRequest;
import static uk.gov.hmcts.juror.scheduler.testsupport.DataUtilIT.loginRequest;
import static uk.gov.hmcts.juror.scheduler.testsupport.DataUtilIT.registerRequest;
import static uk.gov.hmcts.juror.scheduler.testsupport.DataUtilIT.statusUpdateRequest;
import static uk.gov.hmcts.juror.scheduler.testsupport.DataUtilIT.updateJson;
import static uk.gov.hmcts.juror.scheduler.testsupport.DataUtilIT.userEmailRequest;
import static uk.gov.hmcts.juror.scheduler.testsupport.ITestUtil.dynamicEmailGenerator;
import static uk.gov.hmcts.juror.scheduler.testsupport.ITestUtil.getNextUniqueIndex;
import static uk.gov.hmcts.juror.scheduler.testsupport.ITestUtil.getTestDataAsStringFromFile;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SecurityIT extends AbstractIT {
    private String jwtAdmin;
    private int uniqueId;

    private static final String URL_AUTH_USER = "/auth/user";
    private static final String URL_AUTH_REGISTER = "/auth/register";
    private static final String URL_AUTH_USER_PERMISSIONS = "/auth/user/permissions";
    private static final String URL_JOB_KEY = "/job/JOBKEY";
    private static final String URL_DISABLE = "/disable";

    @Autowired
    protected SecurityIT(MockMvc mockMvc) {
        super(mockMvc);
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void init() throws Exception {
        jwtAdmin = generateJwt(loginRequest(ADMIN_EMAIL, ADMIN_PASSWORD_ENCRYPTED));
        uniqueId = getNextUniqueIndex();
    }

    @DisplayName("JobController - Test permissions associated with getJobDetails operation")
    @Test
    void getJobDetailsPermissionsTest() throws Exception {
        String userEmail = createUser();
        scheduleJob();

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(VIEW);
        Set<String> permissions = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permissions to get job details - is unauthorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform(URL_JOB_KEY + uniqueId, jwtUser, GET, "").andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissions.add(VIEW);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissions, "", userEmail))
            .andExpect(status().isAccepted());

        mockMvcPerform(URL_JOB_KEY + uniqueId, jwtUser,  GET, "").andExpect(status().isOk());
    }

    @DisplayName("JobController - Test permissions associated with enable job operation")
    @Test
    void enablePermissionsTest() throws Exception {
        String userEmail = createUser();
        scheduleJob();

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(ENABLE);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //Disable job first - user is authorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform(URL_JOB_KEY + uniqueId + URL_DISABLE, jwtUser, PUT,"")
            .andExpect(status().isAccepted());

        //User does not have permission to enable a job - is unauthorised
        mockMvcPerform(URL_JOB_KEY + uniqueId + "/enable", jwtUser, PUT,"")
            .andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(ENABLE);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        mockMvcPerform(URL_JOB_KEY + uniqueId + "/enable", jwtUser, PUT,"")
            .andExpect(status().isAccepted());
    }

    @DisplayName("JobController - Test permissions associated with disable job operation")
    @Test
    void disablePermissionsTest() throws Exception {
        String userEmail = createUser();
        scheduleJob();

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(DISABLE);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permission to disable a job - not authorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform(URL_JOB_KEY + uniqueId + URL_DISABLE, jwtUser, PUT, "")
            .andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(DISABLE);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        mockMvcPerform(URL_JOB_KEY + uniqueId + URL_DISABLE, jwtUser, PUT,"")
            .andExpect(status().isAccepted());
    }

    @DisplayName("JobController - Test permissions associated with delete operation")
    @Test
    void deletePermissionsTest() throws Exception {
        String userEmail = createUser();
        scheduleJob();

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(DELETE);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permission to delete a job - not authorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform(URL_JOB_KEY + uniqueId, jwtUser, HttpMethod.DELETE, "")
            .andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(DELETE);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        mockMvcPerform(URL_JOB_KEY + uniqueId, jwtUser, HttpMethod.DELETE,"")
            .andExpect(status().isOk());
    }

    @DisplayName("JobController - Test permissions associated with run job operation")
    @Test
    void runJobPermissionsTest() throws Exception {
        String userEmail = createUser();
        scheduleJob();

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(RUN);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permission to run a job - not authorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform(URL_JOB_KEY + uniqueId + "/run", jwtUser, PUT, "")
            .andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(RUN);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        mockMvcPerform(URL_JOB_KEY + uniqueId + "/run", jwtUser, PUT, "")
            .andExpect(status().isAccepted());
    }

    @DisplayName("JobController - Test permissions associated with update job operation")
    @Test
    void updatePermissionsTest() throws Exception {
        String userEmail = createUser();
        String updateJobDetails = "This is a test";
        scheduleJob();

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(API_UPDATE);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permission to update a job - not authorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform(URL_JOB_KEY + uniqueId, jwtUser, PATCH, apiJobPatchRequest(updateJobDetails))
            .andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(API_UPDATE);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        mockMvcPerform(URL_JOB_KEY + uniqueId, jwtUser, PATCH, apiJobPatchRequest(updateJobDetails))
            .andExpect(status().isOk());

        //Check update was successful
        mockMvcPerform(URL_JOB_KEY + uniqueId, jwtUser, GET, "")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payload").value(updateJobDetails));
    }

    @DisplayName("JobController - Test permissions associated with get job tasks")
    @Test
    void searchForJobUsingJobKeyPermissionsTest() throws Exception {
        String userEmail = createUser();
        scheduleJob();

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(VIEW);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permission to get job tasks - user not authorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform(URL_JOB_KEY + uniqueId + "/tasks", jwtUser, GET,"")
            .andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(VIEW);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        mockMvcPerform(URL_JOB_KEY + uniqueId + "/tasks", jwtUser, GET,"")
            .andExpect(status().isOk());
    }

    @DisplayName("JobController - Test permissions associated with getting job status")
    @Test
    void getJobStatusWithAllViewPermissionsTest() throws Exception {
        scheduleJob();

        //Grant user relevant permissions (exclude permissions under test)
        String userEmail = createUser();
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(VIEW_STATUS);
        permissionsToExclude.add(VIEW);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permission to get job status - not authorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform(URL_JOB_KEY + uniqueId + "/status", jwtUser, GET, "")
            .andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(VIEW_STATUS);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        mockMvcPerform(URL_JOB_KEY + uniqueId + "/status", jwtUser, GET, "")
            .andExpect(status().isOk());
    }

    @DisplayName("JobController - Test permissions associated with getting job status "
        + "(job view status - VIEW_STATUS)")
    @Test
    void getJobStatusWithJobViewStatusPermissionsTest() throws Exception {
        scheduleJob();

        //Grant user relevant permissions (exclude permissions under test)
        String userEmail = createUser();
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(VIEW_STATUS);
        grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User will be authorised because they have VIEW status
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform(URL_JOB_KEY + uniqueId + "/status", jwtUser, GET, "")
            .andExpect(status().isOk());
    }

    @DisplayName("JobController - Test permissions associated with getting job status "
        + "(job view - VIEW)")
    @Test
    void getJobStatusWithJobViewPermissionsTest() throws Exception {
        scheduleJob();

        //Grant user relevant permissions (exclude permissions under test)
        String userEmail = createUser();
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(VIEW);
        grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User will be authorised because they have VIEW_STATUS status
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform(URL_JOB_KEY + uniqueId + "/status", jwtUser, GET, "")
            .andExpect(status().isOk());
    }

    @DisplayName("JobsController - Test permissions associated with creating an API Job")
    @Test
    void createAPIJobPermissionsTest() throws Exception {
        String userEmail = createUser();

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(API_CREATE);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permission to get job status - not authorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        String payload = updateJson(getTestDataAsStringFromFile(API_DUMMY_CRON_JOB_JSON),
            "key",
            "JOBKEY" + uniqueId);
        mockMvcPerform(URL_JOBS_API, jwtUser, POST,  payload).andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(API_CREATE);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        mockMvcPerform(URL_JOBS_API, jwtUser, POST, payload).andExpect(status().isOk());
    }

    @DisplayName("JobsController - Test permissions associated with searching for a job")
    @Test
    void getJobsPermissionsTest() throws Exception {
        String userEmail = createUser();
        scheduleJob();

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(SEARCH);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permission to get job status - not authorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform("/jobs/search?job_key=JOBKEY" + uniqueId, jwtUser, GET,"")
            .andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(SEARCH);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        mockMvcPerform("/jobs/search?job_key=JOBKEY" + uniqueId, jwtUser, GET, "")
            .andExpect(status().isOk());
    }

    @DisplayName("JobTaskController - Test permissions associated with getting task details")
    @Test
    void getTaskDetailPermissionsTest() throws Exception {
        String userEmail = createUser();
        String jwtUser = getUserJwt(userEmail, jwtAdmin);

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(PermissionConstants.Task.VIEW);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permission to get job status - not authorised
        mockMvcPerform(URL_JOB_KEY + uniqueId + "/task/1", jwtUser, GET,"")
            .andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(PermissionConstants.Task.VIEW);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        //Note: NotFound means the task hasn't executed yet, which is okay for this test
        mockMvcPerform(URL_JOB_KEY + uniqueId + "/task/1", jwtUser, GET, "")
            .andExpect(status().isNotFound());
    }

    @DisplayName("JobTaskController - Test permissions associated with updating tasks status")
    @Test
    void updateTaskStatusPermissionsTest() throws Exception {
        String userEmail = createUser();

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(PermissionConstants.Task.STATUS_UPDATE);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permission to get job status - not authorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform(URL_JOB_KEY + uniqueId + "/task/1/status", jwtUser, PUT, statusUpdateRequest())
            .andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(PermissionConstants.Task.STATUS_UPDATE);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        //Note: NotFound means the task hasn't executed yet, which is okay for this test
        mockMvcPerform(URL_JOB_KEY + uniqueId + "/task/1/status", jwtUser, PUT, statusUpdateRequest())
            .andExpect(status().isNotFound());
    }

    @DisplayName("JobTaskController - Test permissions associated with getting tasks")
    @Test
    void getTasksPermissionsTest() throws Exception {
        String userEmail = createUser();

        //Grant user relevant permissions (exclude permissions under test)
        Set<String> permissionsToExclude = new HashSet<>();
        permissionsToExclude.add(PermissionConstants.Task.SEARCH);
        Set<String> permissionSet = grantUserRelevantPermissions(permissionsToExclude, userEmail);

        //User does not have permission to get job status - not authorised
        String jwtUser = getUserJwt(userEmail, jwtAdmin);
        mockMvcPerform("/tasks/search?job_key=ABC", jwtUser, GET, "")
            .andExpect(status().isUnauthorized());

        //Grant user missing permissions
        permissionSet.add(PermissionConstants.Task.SEARCH);
        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        //Note: NotFound means the task hasn't executed yet, which is okay for this test
        mockMvcPerform("/tasks/search?job_key=ABC", jwtUser, GET, "").andExpect(status().isNotFound());
    }

    private Set<String> grantUserRelevantPermissions(Set<String> permissionToExclude,
                                                     String userEmail) throws Exception {
        Set<String> permissionSet = new HashSet<>();
        permissionSet.add(ENABLE);
        permissionSet.add(DISABLE);
        permissionSet.add(DELETE);
        permissionSet.add(RUN);
        permissionSet.add(API_CREATE);
        permissionSet.add(API_UPDATE);
        permissionSet.add(PermissionConstants.Job.VIEW);
        permissionSet.add(SEARCH);
        permissionSet.add(VIEW_STATUS);
        permissionSet.add(PermissionConstants.Task.VIEW);
        permissionSet.add(PermissionConstants.Task.STATUS_UPDATE);
        permissionSet.add(PermissionConstants.Task.SEARCH);

        permissionSet.removeAll(permissionToExclude);

        mockMvcPerform(URL_AUTH_USER_PERMISSIONS, jwtAdmin, PUT,
            assignPermissionsRequest(permissionSet, "", userEmail))
            .andExpect(status().isAccepted());

        return permissionSet;
    }

    private void scheduleJob() throws Exception {
        //Admin user schedules a job
        String payload = updateJson(getTestDataAsStringFromFile(API_DUMMY_CRON_JOB_JSON),
            "key",
            "JOBKEY" + uniqueId);

        mockMvcPerform(URL_JOBS_API, jwtAdmin, POST, payload).andExpect(status().isOk());
    }

    private String assignPermissionsRequest(Set<String> permissions,
                                            String role,
                                            String email) throws JsonProcessingException {
        AssignPermissionsRequest.RolePermissions rolePermissions;

        if (!role.isEmpty()) {
            rolePermissions = AssignPermissionsRequest.RolePermissions.builder()
                .permissions(permissions)
                .roles(Collections.singleton(role))
                .build();
        } else {
            rolePermissions = AssignPermissionsRequest.RolePermissions.builder()
                .permissions(permissions)
                .build();
        }

        return asJsonString(AssignPermissionsRequest.builder()
            .add(rolePermissions)
            .email(email)
            .build());
    }

    private String  createUser() throws Exception {
        String userEmail = dynamicEmailGenerator(uniqueId);
        mockMvcPerform(URL_AUTH_REGISTER, jwtAdmin, POST, registerRequest(userEmail))
            .andExpect(status().isCreated());

        //Verify user has been registered (added)
        mockMvcPerform(URL_AUTH_USER, jwtAdmin, POST, userEmailRequest(userEmail))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isNotEmpty());

        return userEmail;
    }
}
