package uk.gov.hmcts.juror.scheduler.controllers;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobPatch;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.juror.scheduler.testsupport.ConvertUtilIT.mapObjectToJson;
import static uk.gov.hmcts.juror.scheduler.testsupport.DataUtilIT.loginRequest;
import static uk.gov.hmcts.juror.scheduler.testsupport.DataUtilIT.updateJson;
import static uk.gov.hmcts.juror.scheduler.testsupport.ITestUtil.getNextUniqueIndex;
import static uk.gov.hmcts.juror.scheduler.testsupport.ITestUtil.getTestDataAsStringFromFile;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SchedulerIT extends AbstractIT {
    private String jwtAdmin;
    private String uniqueJobKey;

    private static final String URL_TASKS = "/tasks";

    @LocalServerPort
    protected int port;

    @Autowired
    protected SchedulerIT(MockMvc mockMvc) {
        super(mockMvc);
    }

    @BeforeEach
    void init() throws Exception {
        jwtAdmin = generateJwt(loginRequest(ADMIN_EMAIL, ADMIN_PASSWORD_ENCRYPTED));
        uniqueJobKey = "KEY" + getNextUniqueIndex();
    }

    @DisplayName("Schedule a new API job")
    @Test
    void scheduleANewApiJob() throws Exception {
        String payload = updateJson(getTestDataAsStringFromFile(API_DUMMY_CRON_JOB_JSON),
            "key",
            uniqueJobKey);

        mockMvcPerform(URL_JOBS_API, jwtAdmin, POST, payload).andExpect(status().isOk());

        mockMvcPerform("/job/" + uniqueJobKey, jwtAdmin, GET,"")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isNotEmpty())
            .andExpect(jsonPath("$.key").value(uniqueJobKey));
    }

    @DisplayName("Cannot schedule a new API job with a 'key' value that already exists")
    @Test
    void cannotScheduleJobWithNonUniqueKey() throws Exception {
        String payload = updateJson(getTestDataAsStringFromFile(API_DUMMY_CRON_JOB_JSON),
            "key",
            uniqueJobKey);

        mockMvcPerform(URL_JOBS_API, jwtAdmin, POST, payload).andExpect(status().isOk());

        mockMvcPerform(URL_JOBS_API, jwtAdmin, POST, payload)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$").isNotEmpty())
            .andExpect(jsonPath("$.code").value("KEY_ALREADY_IN_USE"))
            .andExpect(jsonPath("$.messages")
                .value("The key you have provided is already in use. " + "Please choice a unique key."));
    }

    @DisplayName("Attempt to get details of a job that does not exist and error message is returned")
    @Test
    void getDetailsOfAJobThatDoesNotExist() throws Exception {
        mockMvcPerform("/job/IDONOTEXIST", jwtAdmin, GET,"")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$").isNotEmpty())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.messages")
                     .value("The requested resource could not be located."));
    }

    @DisplayName("Get the tasks executed for a scheduled job")
    @Test
    void getJobTasks() throws Exception {
        String payload = updateJson(getTestDataAsStringFromFile(API_DUMMY_CRON_JOB_JSON),
            "key",
            uniqueJobKey);

        mockMvcPerform(URL_JOBS_API, jwtAdmin, POST, payload)
            .andExpect(status().isOk());

        //Give job a few seconds to execute the tasks as per the cron expression
        TimeUnit.SECONDS.sleep(3);

        String url = "/job/" + uniqueJobKey + URL_TASKS;
        mockMvcPerform(url, jwtAdmin, GET, "")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$",hasSize(greaterThan(1))))
            .andExpect(jsonPath("$[1].job_key").value(uniqueJobKey)); //Validating against second task
    }

    @DisplayName("Disable and enable a scheduled job")
    @Test
    void disableAndEnableScheduledJob() throws Exception {
        String payload = updateJson(getTestDataAsStringFromFile(API_DUMMY_CRON_JOB_JSON),
            "key",
            uniqueJobKey);

        mockMvcPerform(URL_JOBS_API, jwtAdmin, POST, payload).andExpect(status().isOk());

        String url = "/job/" + uniqueJobKey;
        int tasksCounter;
        int counter = 0; // this is required to ensure don't have an infinite loop
        MvcResult resultBefore;
        do {
            resultBefore = mockMvcPerform(url + URL_TASKS, jwtAdmin, GET, "")
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();
            tasksCounter = JsonPath.read(resultBefore.getResponse().getContentAsString(), "$.length()");
            counter++;
        } while (tasksCounter == 0 && counter < 100);

        mockMvcPerform(url + "/disable", jwtAdmin, PUT,"")
            .andExpect(status().isAccepted());

        MvcResult resultAfter = mockMvcPerform(url + URL_TASKS, jwtAdmin, GET,
            "")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andReturn();

        //Get count of tasks executed
        Integer tasksExecutedBeforeDisable = JsonPath.read(resultAfter.getResponse().getContentAsString(),
            "$.length()");

        //Enable the job
        mockMvcPerform(url + "/enable", jwtAdmin, PUT, "")
            .andExpect(status().isAccepted());

        //Give job a few seconds to execute
        TimeUnit.SECONDS.sleep(2);

        //If the job has successfully resumed, there should now be more tasks executed
        mockMvcPerform(url + URL_TASKS, jwtAdmin, GET, "")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(greaterThan(tasksExecutedBeforeDisable))));
    }

    @DisplayName("Update a scheduled job")
    @Test
    void updatedScheduledJob() throws Exception {
        String payload = updateJson(getTestDataAsStringFromFile(API_DUMMY_CRON_JOB_JSON),
            "key",
            uniqueJobKey);

        mockMvcPerform(URL_JOBS_API, jwtAdmin, POST, payload).andExpect(status().isOk());

        APIJobPatch apiJobPatch = new APIJobPatch();
        apiJobPatch.setCronExpression("0 0 12 * * ?");

        mockMvcPerform("/job/" + uniqueJobKey, jwtAdmin, PATCH, mapObjectToJson(apiJobPatch))
            .andExpect(status().isOk());
    }

    @DisplayName("Search for a job using the job Key")
    @Test
    void searchForJobUsingJobKey() throws Exception {
        String payload = updateJson(getTestDataAsStringFromFile(API_DUMMY_CRON_JOB_JSON),
            "key",
            uniqueJobKey);

        mockMvcPerform(URL_JOBS_API, jwtAdmin, POST, payload)
            .andExpect(status().isOk());

        mockMvcPerform("/jobs/search?job_key=" + uniqueJobKey, jwtAdmin, GET, "")
            .andExpect(status().isOk());
    }
}
