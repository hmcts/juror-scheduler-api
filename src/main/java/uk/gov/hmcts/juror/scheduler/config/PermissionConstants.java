package uk.gov.hmcts.juror.scheduler.config;

public final class PermissionConstants {

    private PermissionConstants() {

    }

    public static final class Job {

        private Job() {
        }

        public static final String ENABLE = "job::enable";
        public static final String DISABLE = "job::disable";
        public static final String DELETE = "job::delete";
        public static final String RUN = "job::run";
        public static final String API_CREATE = "job::api::create";
        public static final String API_UPDATE = "job::api::update";
        public static final String VIEW = "job::view";
        public static final String VIEW_STATUS = "job::view::status";
        public static final String SEARCH = "jobs::search";
    }


    public static final class Task {

        private Task() {

        }

        public static final String VIEW = "task::view";
        public static final String STATUS_UPDATE = "task::status::update";
        public static final String SEARCH = "tasks::search";
    }

}
