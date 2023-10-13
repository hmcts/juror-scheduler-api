package uk.gov.hmcts.juror.scheduler.api.model;

import uk.gov.hmcts.juror.scheduler.api.model.error.KeyAlreadyInUseError;

@SuppressWarnings("All")
class KeyAlreadyInUseErrorTest extends GenericErrorTest<KeyAlreadyInUseError>{
    @Override
    protected String getErrorCode() {
        return "KEY_ALREADY_IN_USE";
    }

    @Override
    protected String getDefaultMessage() {
        return "The key you have provided is already in use. Please choice a unique key.";
    }

    @Override
    protected KeyAlreadyInUseError createErrorObject() {
        return new KeyAlreadyInUseError();
    }
}
