package uk.gov.hmcts.juror.scheduler.mapping;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.actions.Action;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.actions.RunJobAction;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.JsonPathAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.MaxResponseTimeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.ActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.RunJobActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.JsonPathAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.MaxResponseTimeAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.StatusCodeValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.ActionType;
import uk.gov.hmcts.juror.scheduler.datastore.model.ValidationType;
import uk.gov.hmcts.juror.scheduler.testsupport.util.GenerateUtil;
import uk.gov.hmcts.juror.scheduler.testsupport.util.ValidateUtil;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({
    "PMD.JUnitTestsShouldIncludeAssert",//False positive done via support libraries
    "PMD.AvoidDuplicateLiterals",
    "PMD.LinguisticNaming",
    "PMD.TooManyMethods"
})
class JobDetailsMapperTest {

    private final JobDetailsMapper jobDetailsMapper;

    public JobDetailsMapperTest() {
        this.jobDetailsMapper = new JobDetailsMapperImpl();
    }

    @Test
    void toEntityStatusCodeValidationEntityTest() {
        StatusCodeAPIValidation validation = GenerateUtil.generateStatusCodeAPIValidation();
        ValidateUtil.validate(validation, jobDetailsMapper.toEntity(validation));
    }

    @Test
    void toEntityMaxResponseTimeAPIValidationEntityTest() {
        MaxResponseTimeAPIValidation validation = GenerateUtil.generateMaxResponseTimeAPIValidation();
        ValidateUtil.validate(validation, jobDetailsMapper.toEntity(validation));
    }

    @Test
    void toEntityJsonPathAPIValidationEntityTest() {
        JsonPathAPIValidation validation = GenerateUtil.generateJsonPathAPIValidation();
        ValidateUtil.validate(validation, jobDetailsMapper.toEntity(validation));
    }

    @Test
    void toEntityRunJobActionEntityTest() {
        RunJobAction action = GenerateUtil.generateRunJobAction();
        ValidateUtil.validate(action, jobDetailsMapper.toEntity(action));
    }

    @Test
    void fromEntityStatusCodeAPIValidationTest() {
        StatusCodeValidationEntity validationEntity = GenerateUtil.generateStatusCodeValidationEntity();
        ValidateUtil.validate(validationEntity, jobDetailsMapper.fromEntity(validationEntity));
    }

    @Test
    void fromEntityMaxResponseTimeAPIValidationTest() {
        MaxResponseTimeAPIValidationEntity validationEntity = GenerateUtil.generateMaxResponseTimeAPIValidationEntity();
        ValidateUtil.validate(validationEntity, jobDetailsMapper.fromEntity(validationEntity));
    }

    @Test
    void fromEntityJsonPathAPIValidationTest() {
        JsonPathAPIValidationEntity validationEntity = GenerateUtil.generateJsonPathAPIValidationEntity();
        ValidateUtil.validate(validationEntity, jobDetailsMapper.fromEntity(validationEntity));
    }

    @Test
    void fromEntityRunJobActionTest() {
        RunJobActionEntity actionEntity = GenerateUtil.generateRunJobActionEntity();
        ValidateUtil.validate(actionEntity, jobDetailsMapper.fromEntity(actionEntity));
    }


    @Test
    void assignJobsBothTest() {
        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .validations(List.of(
                GenerateUtil.generateStatusCodeValidationEntity(),
                GenerateUtil.generateMaxResponseTimeAPIValidationEntity(),
                GenerateUtil.generateJsonPathAPIValidationEntity()
            ))
            .postExecutionActions(List.of(
                GenerateUtil.generateRunJobActionEntity(),
                GenerateUtil.generateRunJobActionEntity(),
                GenerateUtil.generateRunJobActionEntity()
            ))
            .build();

        jobDetailsMapper.assignJobs(apiJobDetailsEntity);

        apiJobDetailsEntity.getValidations().forEach(apiValidationEntity ->
            assertEquals(apiJobDetailsEntity, apiValidationEntity.getJob(),
                "Validation job must match job details entity it was assigned too"));
        apiJobDetailsEntity.getPostExecutionActions().forEach(action ->
            assertEquals(apiJobDetailsEntity, action.getJob(),
                "Action job must match job details entity it was assigned too"));
    }

    @Test
    void assignJobsPostActionsTest() {
        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .validations(null)
            .postExecutionActions(List.of(
                GenerateUtil.generateRunJobActionEntity(),
                GenerateUtil.generateRunJobActionEntity(),
                GenerateUtil.generateRunJobActionEntity()
            ))
            .build();

        jobDetailsMapper.assignJobs(apiJobDetailsEntity);


        apiJobDetailsEntity.getPostExecutionActions().forEach(action ->
            assertEquals(apiJobDetailsEntity, action.getJob(),
                "Action job must match job details entity it was assigned too"));

    }

    @Test
    void assignJobsValidationsTest() {

        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .validations(List.of(
                GenerateUtil.generateStatusCodeValidationEntity(),
                GenerateUtil.generateMaxResponseTimeAPIValidationEntity(),
                GenerateUtil.generateJsonPathAPIValidationEntity()
            ))
            .postExecutionActions(null)
            .build();

        jobDetailsMapper.assignJobs(apiJobDetailsEntity);

        apiJobDetailsEntity.getValidations().forEach(apiValidationEntity ->
            assertEquals(apiJobDetailsEntity, apiValidationEntity.getJob(),
                "Validation job must match job details entity it was assigned too"));
    }

    @Test
    void apiValidationToEntryNullTest() {
        assertNull(jobDetailsMapper.apiValidationToEntry(null),
            "Must return null if null is provided");
    }

    @Test
    void apiValidationToEntryStatusCodeAPIValidationTest() {
        StatusCodeAPIValidation validation = GenerateUtil.generateStatusCodeAPIValidation();
        ValidateUtil.validate(validation, jobDetailsMapper.apiValidationToEntry(validation));
    }

    @Test
    void apiValidationToEntryMaxResponseTimeAPIValidationTest() {
        MaxResponseTimeAPIValidation validation = GenerateUtil.generateMaxResponseTimeAPIValidation();
        ValidateUtil.validate(validation, jobDetailsMapper.apiValidationToEntry(validation));
    }

    @Test
    void apiValidationToEntryJsonPathAPIValidationTest() {
        JsonPathAPIValidation validation = GenerateUtil.generateJsonPathAPIValidation();
        ValidateUtil.validate(validation, jobDetailsMapper.apiValidationToEntry(validation));
    }

    @Test
    void apiValidationToEntryUnknownTypeTest() {
        UnsupportedOperationException unsupportedOperationException = assertThrows(
            UnsupportedOperationException.class,
            () -> jobDetailsMapper.apiValidationToEntry(new APIValidation() {
                @Override
                public ValidationType getType() {
                    return null;
                }
            }),
            "Must throw exception hen type not found"
        );
        assertEquals("Unknown validation type: class uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapperTest$1",
            unsupportedOperationException.getMessage(),
            "Error message must match");
    }

    @Test
    void apiValidationEntityListNullTest() {
        assertEquals(0, jobDetailsMapper.apiValidationEntityList(null).size(),
            "An empty list should be returned if null is given");
    }

    @Test
    void apiValidationEntityListEmptyTest() {
        assertEquals(0, jobDetailsMapper.apiValidationEntityList(Collections.emptyList()).size(),
            "An empty list should be returned if no entries is given");

    }

    @Test
    void apiValidationEntityListTest() {
        List<APIValidation> validationList = List.of(
            GenerateUtil.generateStatusCodeAPIValidation(),
            GenerateUtil.generateMaxResponseTimeAPIValidation(),
            GenerateUtil.generateJsonPathAPIValidation(),
            GenerateUtil.generateStatusCodeAPIValidation()
        );
        ValidateUtil.validateAPIValidationList(validationList,
            jobDetailsMapper.apiValidationEntityList(validationList));
    }


    @Test
    void apiValidationEntryToValidationNullTest() {
        assertNull(jobDetailsMapper.apiValidationEntryToValidation(null),
            "Must return null if null is provided");
    }

    @Test
    void apiValidationEntryToValidationStatusCodeValidationEntityTest() {
        StatusCodeValidationEntity entity = GenerateUtil.generateStatusCodeValidationEntity();
        ValidateUtil.validate(entity, jobDetailsMapper.apiValidationEntryToValidation(entity));
    }

    @Test
    void apiValidationEntryToValidationMaxResponseTimeAPIValidationEntityTest() {
        MaxResponseTimeAPIValidationEntity entity = GenerateUtil.generateMaxResponseTimeAPIValidationEntity();
        ValidateUtil.validate(entity, jobDetailsMapper.apiValidationEntryToValidation(entity));
    }

    @Test
    void apiValidationEntryToValidationJsonPathAPIValidationEntityTest() {
        JsonPathAPIValidationEntity entity = GenerateUtil.generateJsonPathAPIValidationEntity();
        ValidateUtil.validate(entity, jobDetailsMapper.apiValidationEntryToValidation(entity));
    }

    @Test
    void apiValidationEntryToValidationUnknownClassTest() {
        UnsupportedOperationException unsupportedOperationException = assertThrows(
            UnsupportedOperationException.class,
            () -> jobDetailsMapper.apiValidationEntryToValidation(new APIValidationEntity() {
                @Override
                public Result validate(Response response, APIJobDetailsEntity jobData) {
                    return null;
                }

                @Override
                public ValidationType getType() {
                    return null;
                }
            }),
            "Must throw exception hen type not found"
        );
        assertEquals("Unknown validation type: class uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapperTest$2",
            unsupportedOperationException.getMessage(),
            "Error message must match");
    }

    @Test
    void apiValidationListNullTest() {
        assertEquals(0, jobDetailsMapper.apiValidationList(null).size(),
            "An empty list should be returned if null is given");
    }

    @Test
    void apiValidationListEmptyTest() {
        assertEquals(0, jobDetailsMapper.apiValidationList(Collections.emptyList()).size(),
            "An empty list should be returned if no entries is given");
    }

    @Test
    void apiValidationListTest() {
        List<APIValidationEntity> validationEntities = List.of(
            GenerateUtil.generateStatusCodeValidationEntity(),
            GenerateUtil.generateMaxResponseTimeAPIValidationEntity(),
            GenerateUtil.generateJsonPathAPIValidationEntity(),
            GenerateUtil.generateJsonPathAPIValidationEntity()
        );
        ValidateUtil.validateAPIValidationEntityList(validationEntities,
            jobDetailsMapper.apiValidationList(validationEntities));
    }

    @Test
    void actionToEntryNullTest() {
        assertNull(jobDetailsMapper.actionToEntry(null),
            "Must return null if null is provided");
    }

    @Test
    void actionToEntryRunJobActionTest() {
        RunJobAction action = GenerateUtil.generateRunJobAction();
        ValidateUtil.validate(action, jobDetailsMapper.actionToEntry(action));
    }

    @Test
    void actionToEntryUnknownClassTest() {
        UnsupportedOperationException unsupportedOperationException = assertThrows(
            UnsupportedOperationException.class,
            () -> jobDetailsMapper.actionToEntry(new Action() {
                @Override
                public ActionType getType() {
                    return null;
                }
            }),
            "Must throw exception hen type not found"
        );
        assertEquals("Unknown action type: class uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapperTest$3",
            unsupportedOperationException.getMessage(),
            "Error message must match");
    }


    @Test
    void actionsListListNullTest() {
        assertEquals(0, jobDetailsMapper.actionList(null).size(),
            "An empty list should be returned if null is given");
    }

    @Test
    void actionsListListEmptyTest() {
        assertEquals(0, jobDetailsMapper.actionList(Collections.emptyList()).size(),
            "An empty list should be returned if no entries is given");
    }

    @Test
    void actionsListTest() {
        List<ActionEntity> actionEntityList = List.of(
            GenerateUtil.generateRunJobActionEntity(),
            GenerateUtil.generateRunJobActionEntity(),
            GenerateUtil.generateRunJobActionEntity()
        );
        ValidateUtil.validateActionEntityList(actionEntityList, jobDetailsMapper.actionList(actionEntityList));
    }

    @Test
    void actionEntityListNullTest() {
        assertEquals(0, jobDetailsMapper.actionEntityList(null).size(),
            "An empty list should be returned if null is given");
    }

    @Test
    void actionEntityListEmptyTest() {
        assertEquals(0, jobDetailsMapper.actionEntityList(Collections.emptyList()).size(),
            "An empty list should be returned if no entries is given");
    }

    @Test
    void actionEntityListTest() {
        List<Action> actions = List.of(
            GenerateUtil.generateRunJobAction(),
            GenerateUtil.generateRunJobAction(),
            GenerateUtil.generateRunJobAction()
        );
        ValidateUtil.validateActionList(actions, jobDetailsMapper.actionEntityList(actions));
    }

    @Test
    void actionEntryToActionNullTest() {
        assertNull(jobDetailsMapper.actionEntryToAction(null),
            "Must return null if null is provided");
    }

    @Test
    void actionEntryToActionRunJobActionEntityTest() {
        RunJobActionEntity entity = GenerateUtil.generateRunJobActionEntity();
        ValidateUtil.validate(entity, jobDetailsMapper.actionEntryToAction(entity));
    }

    @Test
    void actionEntryToActionUnknownClassTest() {
        UnsupportedOperationException unsupportedOperationException = assertThrows(
            UnsupportedOperationException.class,
            () -> jobDetailsMapper.actionEntryToAction(new ActionEntity() {
                @Override
                public ActionType getType() {
                    return null;
                }
            }),
            "Must throw exception hen type not found"
        );
        assertEquals("Unknown Action type: class uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapperTest$4",
            unsupportedOperationException.getMessage(),
            "Error message must match");

    }
}
