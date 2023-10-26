CREATE SCHEMA IF NOT EXISTS scheduler_application;
-- DROP SCHEMA scheduler_application;

-- CREATE SCHEMA scheduler_application AUTHORIZATION postgres;

-- DROP SEQUENCE scheduler_application.action_entity_id_seq;

CREATE SEQUENCE scheduler_application.action_entity_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1
    NO CYCLE;
-- DROP SEQUENCE scheduler_application.apivalidation_entity_id_seq;

CREATE SEQUENCE scheduler_application.apivalidation_entity_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1
    NO CYCLE;
-- DROP SEQUENCE scheduler_application.revinfo_seq;

CREATE SEQUENCE scheduler_application.revinfo_seq
    INCREMENT BY 50
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1
    NO CYCLE;
-- DROP SEQUENCE scheduler_application.task_entity_task_id_seq;

CREATE SEQUENCE scheduler_application.task_entity_task_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1
    NO CYCLE;
-- DROP SEQUENCE scheduler_application.users_id_seq;

CREATE SEQUENCE scheduler_application.users_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 10
    CACHE 1
    NO CYCLE;
-- scheduler_application.apijob_details_entity definition

-- Drop table

-- DROP TABLE scheduler_application.apijob_details_entity;

CREATE TABLE scheduler_application.apijob_details_entity
(
    authentication_default int2          NULL,
    "method"               int2          NOT NULL,
    created_at             timestamp(6)  NULL,
    last_updated_at        timestamp(6)  NULL,
    description            varchar(2500) NULL,
    "name"                 varchar(2500) NOT NULL,
    payload                varchar(2500) NULL,
    cron_expression        varchar(255)  NULL,
    "key"                  varchar(255)  NOT NULL,
    url                    varchar(255)  NOT NULL,
    CONSTRAINT apijob_details_entity_authentication_default_check CHECK (((authentication_default >= 0) AND (authentication_default <= 2))),
    CONSTRAINT apijob_details_entity_method_check CHECK (((method >= 0) AND (method <= 7))),
    CONSTRAINT apijob_details_entity_pkey PRIMARY KEY (key)
);


-- scheduler_application."permission" definition

-- Drop table

-- DROP TABLE scheduler_application."permission";

CREATE TABLE scheduler_application."permission"
(
    "name" varchar(255) NOT NULL,
    CONSTRAINT permission_pkey PRIMARY KEY (name)
);


-- scheduler_application.qrtz_calendars definition

-- Drop table

-- DROP TABLE scheduler_application.qrtz_calendars;

CREATE TABLE scheduler_application.qrtz_calendars
(
    sched_name    varchar(120) NOT NULL,
    calendar_name varchar(200) NOT NULL,
    calendar      bytea        NOT NULL,
    CONSTRAINT qrtz_calendars_pkey PRIMARY KEY (sched_name, calendar_name)
);


-- scheduler_application.qrtz_fired_triggers definition

-- Drop table

-- DROP TABLE scheduler_application.qrtz_fired_triggers;

CREATE TABLE scheduler_application.qrtz_fired_triggers
(
    sched_name        varchar(120) NOT NULL,
    entry_id          varchar(95)  NOT NULL,
    trigger_name      varchar(200) NOT NULL,
    trigger_group     varchar(200) NOT NULL,
    instance_name     varchar(200) NOT NULL,
    fired_time        int8         NOT NULL,
    sched_time        int8         NOT NULL,
    priority          int4         NOT NULL,
    state             varchar(16)  NOT NULL,
    job_name          varchar(200) NULL,
    job_group         varchar(200) NULL,
    is_nonconcurrent  bool         NULL,
    requests_recovery bool         NULL,
    CONSTRAINT qrtz_fired_triggers_pkey PRIMARY KEY (sched_name, entry_id)
);
CREATE INDEX idx_qrtz_ft_inst_job_req_rcvry ON scheduler_application.qrtz_fired_triggers USING btree (sched_name, instance_name, requests_recovery);
CREATE INDEX idx_qrtz_ft_j_g ON scheduler_application.qrtz_fired_triggers USING btree (sched_name, job_name, job_group);
CREATE INDEX idx_qrtz_ft_jg ON scheduler_application.qrtz_fired_triggers USING btree (sched_name, job_group);
CREATE INDEX idx_qrtz_ft_t_g ON scheduler_application.qrtz_fired_triggers USING btree (sched_name, trigger_name, trigger_group);
CREATE INDEX idx_qrtz_ft_tg ON scheduler_application.qrtz_fired_triggers USING btree (sched_name, trigger_group);
CREATE INDEX idx_qrtz_ft_trig_inst_name ON scheduler_application.qrtz_fired_triggers USING btree (sched_name, instance_name);


-- scheduler_application.qrtz_job_details definition

-- Drop table

-- DROP TABLE scheduler_application.qrtz_job_details;

CREATE TABLE scheduler_application.qrtz_job_details
(
    sched_name        varchar(120) NOT NULL,
    job_name          varchar(200) NOT NULL,
    job_group         varchar(200) NOT NULL,
    description       varchar(250) NULL,
    job_class_name    varchar(250) NOT NULL,
    is_durable        bool         NOT NULL,
    is_nonconcurrent  bool         NOT NULL,
    is_update_data    bool         NOT NULL,
    requests_recovery bool         NOT NULL,
    job_data          bytea        NULL,
    CONSTRAINT qrtz_job_details_pkey PRIMARY KEY (sched_name, job_name, job_group)
);
CREATE INDEX idx_qrtz_j_grp ON scheduler_application.qrtz_job_details USING btree (sched_name, job_group);
CREATE INDEX idx_qrtz_j_req_recovery ON scheduler_application.qrtz_job_details USING btree (sched_name, requests_recovery);


-- scheduler_application.qrtz_locks definition

-- Drop table

-- DROP TABLE scheduler_application.qrtz_locks;

CREATE TABLE scheduler_application.qrtz_locks
(
    sched_name varchar(120) NOT NULL,
    lock_name  varchar(40)  NOT NULL,
    CONSTRAINT qrtz_locks_pkey PRIMARY KEY (sched_name, lock_name)
);


-- scheduler_application.qrtz_paused_trigger_grps definition

-- Drop table

-- DROP TABLE scheduler_application.qrtz_paused_trigger_grps;

CREATE TABLE scheduler_application.qrtz_paused_trigger_grps
(
    sched_name    varchar(120) NOT NULL,
    trigger_group varchar(200) NOT NULL,
    CONSTRAINT qrtz_paused_trigger_grps_pkey PRIMARY KEY (sched_name, trigger_group)
);


-- scheduler_application.qrtz_scheduler_state definition

-- Drop table

-- DROP TABLE scheduler_application.qrtz_scheduler_state;

CREATE TABLE scheduler_application.qrtz_scheduler_state
(
    sched_name        varchar(120) NOT NULL,
    instance_name     varchar(200) NOT NULL,
    last_checkin_time int8         NOT NULL,
    checkin_interval  int8         NOT NULL,
    CONSTRAINT qrtz_scheduler_state_pkey PRIMARY KEY (sched_name, instance_name)
);


-- scheduler_application.revinfo definition

-- Drop table

-- DROP TABLE scheduler_application.revinfo;

CREATE TABLE scheduler_application.revinfo
(
    rev      int4 NOT NULL,
    revtstmp int8 NULL,
    CONSTRAINT revinfo_pkey PRIMARY KEY (rev)
);


-- scheduler_application."role" definition

-- Drop table

-- DROP TABLE scheduler_application."role";

CREATE TABLE scheduler_application."role"
(
    "name" varchar(255) NOT NULL,
    CONSTRAINT role_pkey PRIMARY KEY (name)
);


-- scheduler_application.users definition

-- Drop table

-- DROP TABLE scheduler_application.users;

CREATE TABLE scheduler_application.users
(
    account_non_expired     bool         NOT NULL,
    account_non_locked      bool         NOT NULL,
    credentials_non_expired bool         NOT NULL,
    enabled                 bool         NOT NULL,
    id                      bigserial    NOT NULL,
    email                   varchar(255) NULL,
    firstname               varchar(255) NULL,
    lastname                varchar(255) NULL,
    "password"              varchar(255) NULL,
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_pkey PRIMARY KEY (id)
);


-- scheduler_application.action_entity definition

-- Drop table

-- DROP TABLE scheduler_application.action_entity;

CREATE TABLE scheduler_application.action_entity
(
    "condition" int2         NOT NULL,
    id          bigserial    NOT NULL,
    "type"      varchar(31)  NOT NULL,
    job_key     varchar(255) NOT NULL,
    CONSTRAINT action_entity_condition_check CHECK (((condition >= 0) AND (condition <= 7))),
    CONSTRAINT action_entity_pkey PRIMARY KEY (id),
    CONSTRAINT fkr471nwul7h1x6tqlkkgqg0skb FOREIGN KEY (job_key) REFERENCES scheduler_application.apijob_details_entity ("key")
);


-- scheduler_application.action_entity_aud definition

-- Drop table

-- DROP TABLE scheduler_application.action_entity_aud;

CREATE TABLE scheduler_application.action_entity_aud
(
    "condition" int2         NULL,
    rev         int4         NOT NULL,
    revtype     int2         NULL,
    id          int8         NOT NULL,
    "type"      varchar(31)  NOT NULL,
    job_key     varchar(255) NULL,
    CONSTRAINT action_entity_aud_condition_check CHECK (((condition >= 0) AND (condition <= 7))),
    CONSTRAINT action_entity_aud_pkey PRIMARY KEY (rev, id),
    CONSTRAINT fkdm09011dmwi70bbbji9w997fd FOREIGN KEY (rev) REFERENCES scheduler_application.revinfo (rev)
);


-- scheduler_application.apijob_details_entity_aud definition

-- Drop table

-- DROP TABLE scheduler_application.apijob_details_entity_aud;

CREATE TABLE scheduler_application.apijob_details_entity_aud
(
    authentication_default int2          NULL,
    "method"               int2          NULL,
    rev                    int4          NOT NULL,
    revtype                int2          NULL,
    created_at             timestamp(6)  NULL,
    last_updated_at        timestamp(6)  NULL,
    description            varchar(2500) NULL,
    "name"                 varchar(2500) NULL,
    cron_expression        varchar(255)  NULL,
    "key"                  varchar(255)  NOT NULL,
    payload                varchar(255)  NULL,
    url                    varchar(255)  NULL,
    CONSTRAINT apijob_details_entity_aud_authentication_default_check CHECK (((authentication_default >= 0) AND (authentication_default <= 2))),
    CONSTRAINT apijob_details_entity_aud_method_check CHECK (((method >= 0) AND (method <= 7))),
    CONSTRAINT apijob_details_entity_aud_pkey PRIMARY KEY (rev, key),
    CONSTRAINT fkfd0c0fu4668r7fhjwverv0y40 FOREIGN KEY (rev) REFERENCES scheduler_application.revinfo (rev)
);


-- scheduler_application.apijob_details_entity_post_execution_actions definition

-- Drop table

-- DROP TABLE scheduler_application.apijob_details_entity_post_execution_actions;

CREATE TABLE scheduler_application.apijob_details_entity_post_execution_actions
(
    post_execution_actions_id int8         NOT NULL,
    apijob_details_entity_key varchar(255) NOT NULL,
    CONSTRAINT apijob_details_entity_post_execut_post_execution_actions_id_key UNIQUE (post_execution_actions_id),
    CONSTRAINT fk65b3vlxmu3qyfg2lctnanejym FOREIGN KEY (apijob_details_entity_key) REFERENCES scheduler_application.apijob_details_entity ("key"),
    CONSTRAINT fkci0rk10kmxm3lbdgy86kbcpxs FOREIGN KEY (post_execution_actions_id) REFERENCES scheduler_application.action_entity (id)
);


-- scheduler_application.apijob_details_entity_post_execution_actions_aud definition

-- Drop table

-- DROP TABLE scheduler_application.apijob_details_entity_post_execution_actions_aud;

CREATE TABLE scheduler_application.apijob_details_entity_post_execution_actions_aud
(
    rev                       int4         NOT NULL,
    revtype                   int2         NULL,
    post_execution_actions_id int8         NOT NULL,
    apijob_details_entity_key varchar(255) NOT NULL,
    CONSTRAINT apijob_details_entity_post_execution_actions_aud_pkey PRIMARY KEY (rev, post_execution_actions_id, apijob_details_entity_key),
    CONSTRAINT fkfsd7qwjejaobkldddci6tvnyd FOREIGN KEY (rev) REFERENCES scheduler_application.revinfo (rev)
);


-- scheduler_application.apijob_details_entity_tags definition

-- Drop table

-- DROP TABLE scheduler_application.apijob_details_entity_tags;

CREATE TABLE scheduler_application.apijob_details_entity_tags
(
    apijob_details_entity_key varchar(255) NOT NULL,
    tags                      varchar(255) NULL,
    CONSTRAINT fkremwiff9tsfiumokku27938wu FOREIGN KEY (apijob_details_entity_key) REFERENCES scheduler_application.apijob_details_entity ("key")
);


-- scheduler_application.apijob_details_entity_tags_aud definition

-- Drop table

-- DROP TABLE scheduler_application.apijob_details_entity_tags_aud;

CREATE TABLE scheduler_application.apijob_details_entity_tags_aud
(
    rev                       int4         NOT NULL,
    revtype                   int2         NULL,
    apijob_details_entity_key varchar(255) NOT NULL,
    tags                      varchar(255) NOT NULL,
    CONSTRAINT apijob_details_entity_tags_aud_pkey PRIMARY KEY (rev, apijob_details_entity_key, tags),
    CONSTRAINT fkpamboca8k8b2txiqikg4qn7un FOREIGN KEY (rev) REFERENCES scheduler_application.revinfo (rev)
);


-- scheduler_application.apijob_details_entity_validations_aud definition

-- Drop table

-- DROP TABLE scheduler_application.apijob_details_entity_validations_aud;

CREATE TABLE scheduler_application.apijob_details_entity_validations_aud
(
    rev                       int4         NOT NULL,
    revtype                   int2         NULL,
    validations_id            int8         NOT NULL,
    apijob_details_entity_key varchar(255) NOT NULL,
    CONSTRAINT apijob_details_entity_validations_aud_pkey PRIMARY KEY (rev, validations_id, apijob_details_entity_key),
    CONSTRAINT fkmebutyu5i625hst1vdykcxqpl FOREIGN KEY (rev) REFERENCES scheduler_application.revinfo (rev)
);


-- scheduler_application.apijob_headers definition

-- Drop table

-- DROP TABLE scheduler_application.apijob_headers;

CREATE TABLE scheduler_application.apijob_headers
(
    "key"   varchar(2500) NOT NULL,
    value   varchar(2500) NULL,
    job_key varchar(255)  NOT NULL,
    CONSTRAINT apijob_headers_pkey PRIMARY KEY (key, job_key),
    CONSTRAINT fkl6r2aof4vygillr6p4n3ykk85 FOREIGN KEY (job_key) REFERENCES scheduler_application.apijob_details_entity ("key")
);


-- scheduler_application.apijob_headers_aud definition

-- Drop table

-- DROP TABLE scheduler_application.apijob_headers_aud;

CREATE TABLE scheduler_application.apijob_headers_aud
(
    rev     int4          NOT NULL,
    revtype int2          NULL,
    "key"   varchar(2500) NOT NULL,
    value   varchar(2500) NOT NULL,
    job_key varchar(255)  NOT NULL,
    CONSTRAINT apijob_headers_aud_pkey PRIMARY KEY (rev, key, value, job_key),
    CONSTRAINT fk51kr90tvkdenwdi7esj1o8r67 FOREIGN KEY (rev) REFERENCES scheduler_application.revinfo (rev)
);


-- scheduler_application.apivalidation_entity definition

-- Drop table

-- DROP TABLE scheduler_application.apivalidation_entity;

CREATE TABLE scheduler_application.apivalidation_entity
(
    id      bigserial    NOT NULL,
    "type"  varchar(31)  NOT NULL,
    job_key varchar(255) NOT NULL,
    CONSTRAINT apivalidation_entity_pkey PRIMARY KEY (id),
    CONSTRAINT fk9wdxw4t76ospw10g8mrimne5p FOREIGN KEY (job_key) REFERENCES scheduler_application.apijob_details_entity ("key")
);


-- scheduler_application.apivalidation_entity_aud definition

-- Drop table

-- DROP TABLE scheduler_application.apivalidation_entity_aud;

CREATE TABLE scheduler_application.apivalidation_entity_aud
(
    rev     int4         NOT NULL,
    revtype int2         NULL,
    id      int8         NOT NULL,
    "type"  varchar(31)  NOT NULL,
    job_key varchar(255) NULL,
    CONSTRAINT apivalidation_entity_aud_pkey PRIMARY KEY (rev, id),
    CONSTRAINT fk8lho6q4pjoeo379t0l894uyxo FOREIGN KEY (rev) REFERENCES scheduler_application.revinfo (rev)
);


-- scheduler_application.json_pathapivalidation_entity definition

-- Drop table

-- DROP TABLE scheduler_application.json_pathapivalidation_entity;

CREATE TABLE scheduler_application.json_pathapivalidation_entity
(
    validation_id     int8         NOT NULL,
    expected_response varchar(255) NOT NULL,
    "path"            varchar(255) NOT NULL,
    CONSTRAINT json_pathapivalidation_entity_pkey PRIMARY KEY (validation_id),
    CONSTRAINT fkurg4an1yyrk00n6yiw5r6c3m FOREIGN KEY (validation_id) REFERENCES scheduler_application.apivalidation_entity (id)
);


-- scheduler_application.json_pathapivalidation_entity_aud definition

-- Drop table

-- DROP TABLE scheduler_application.json_pathapivalidation_entity_aud;

CREATE TABLE scheduler_application.json_pathapivalidation_entity_aud
(
    rev               int4         NOT NULL,
    validation_id     int8         NOT NULL,
    expected_response varchar(255) NULL,
    "path"            varchar(255) NULL,
    CONSTRAINT json_pathapivalidation_entity_aud_pkey PRIMARY KEY (rev, validation_id),
    CONSTRAINT fkaxe5wkhpj77c676tp53kouvfi FOREIGN KEY (rev, validation_id) REFERENCES scheduler_application.apivalidation_entity_aud (rev, id)
);


-- scheduler_application.max_response_timeapivalidation_entity definition

-- Drop table

-- DROP TABLE scheduler_application.max_response_timeapivalidation_entity;

CREATE TABLE scheduler_application.max_response_timeapivalidation_entity
(
    max_response_timems int4 NOT NULL,
    validation_id       int8 NOT NULL,
    CONSTRAINT max_response_timeapivalidation_entity_max_response_timems_check CHECK (((max_response_timems <= 30000) AND (max_response_timems >= 1))),
    CONSTRAINT max_response_timeapivalidation_entity_pkey PRIMARY KEY (validation_id),
    CONSTRAINT fkgj8mg9n1o9b66evt4rfhg4u4w FOREIGN KEY (validation_id) REFERENCES scheduler_application.apivalidation_entity (id)
);


-- scheduler_application.max_response_timeapivalidation_entity_aud definition

-- Drop table

-- DROP TABLE scheduler_application.max_response_timeapivalidation_entity_aud;

CREATE TABLE scheduler_application.max_response_timeapivalidation_entity_aud
(
    max_response_timems int4 NULL,
    rev                 int4 NOT NULL,
    validation_id       int8 NOT NULL,
    CONSTRAINT max_response_timeapivalidation_entity_aud_pkey PRIMARY KEY (rev, validation_id),
    CONSTRAINT fk7aaxivs22tnn9u00yxxl31v4b FOREIGN KEY (rev, validation_id) REFERENCES scheduler_application.apivalidation_entity_aud (rev, id)
);


-- scheduler_application.qrtz_triggers definition

-- Drop table

-- DROP TABLE scheduler_application.qrtz_triggers;

CREATE TABLE scheduler_application.qrtz_triggers
(
    sched_name     varchar(120) NOT NULL,
    trigger_name   varchar(200) NOT NULL,
    trigger_group  varchar(200) NOT NULL,
    job_name       varchar(200) NOT NULL,
    job_group      varchar(200) NOT NULL,
    description    varchar(250) NULL,
    next_fire_time int8         NULL,
    prev_fire_time int8         NULL,
    priority       int4         NULL,
    trigger_state  varchar(16)  NOT NULL,
    trigger_type   varchar(8)   NOT NULL,
    start_time     int8         NOT NULL,
    end_time       int8         NULL,
    calendar_name  varchar(200) NULL,
    misfire_instr  int2         NULL,
    job_data       bytea        NULL,
    CONSTRAINT qrtz_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group),
    CONSTRAINT qrtz_triggers_sched_name_job_name_job_group_fkey FOREIGN KEY (sched_name, job_name, job_group) REFERENCES scheduler_application.qrtz_job_details (sched_name, job_name, job_group)
);
CREATE INDEX idx_qrtz_t_c ON scheduler_application.qrtz_triggers USING btree (sched_name, calendar_name);
CREATE INDEX idx_qrtz_t_g ON scheduler_application.qrtz_triggers USING btree (sched_name, trigger_group);
CREATE INDEX idx_qrtz_t_j ON scheduler_application.qrtz_triggers USING btree (sched_name, job_name, job_group);
CREATE INDEX idx_qrtz_t_jg ON scheduler_application.qrtz_triggers USING btree (sched_name, job_group);
CREATE INDEX idx_qrtz_t_n_g_state ON scheduler_application.qrtz_triggers USING btree (sched_name, trigger_group, trigger_state);
CREATE INDEX idx_qrtz_t_n_state ON scheduler_application.qrtz_triggers USING btree (sched_name, trigger_name, trigger_group, trigger_state);
CREATE INDEX idx_qrtz_t_next_fire_time ON scheduler_application.qrtz_triggers USING btree (sched_name, next_fire_time);
CREATE INDEX idx_qrtz_t_nft_misfire ON scheduler_application.qrtz_triggers USING btree (sched_name, misfire_instr, next_fire_time);
CREATE INDEX idx_qrtz_t_nft_st ON scheduler_application.qrtz_triggers USING btree (sched_name, trigger_state, next_fire_time);
CREATE INDEX idx_qrtz_t_nft_st_misfire ON scheduler_application.qrtz_triggers USING btree (sched_name, misfire_instr, next_fire_time, trigger_state);
CREATE INDEX idx_qrtz_t_nft_st_misfire_grp ON scheduler_application.qrtz_triggers USING btree (sched_name,
                                                                                               misfire_instr,
                                                                                               next_fire_time,
                                                                                               trigger_group,
                                                                                               trigger_state);
CREATE INDEX idx_qrtz_t_state ON scheduler_application.qrtz_triggers USING btree (sched_name, trigger_state);


-- scheduler_application.role_inherited_roles definition

-- Drop table

-- DROP TABLE scheduler_application.role_inherited_roles;

CREATE TABLE scheduler_application.role_inherited_roles
(
    inherited_roles_name varchar(255) NOT NULL,
    role_name            varchar(255) NOT NULL,
    CONSTRAINT role_inherited_roles_pkey PRIMARY KEY (inherited_roles_name, role_name),
    CONSTRAINT fkfufff8hqmybtq3kcl6226n9ak FOREIGN KEY (inherited_roles_name) REFERENCES scheduler_application."role" ("name"),
    CONSTRAINT fkkt6cb6mtv6tmq750n1lxequ38 FOREIGN KEY (role_name) REFERENCES scheduler_application."role" ("name")
);


-- scheduler_application.role_permissions definition

-- Drop table

-- DROP TABLE scheduler_application.role_permissions;

CREATE TABLE scheduler_application.role_permissions
(
    permissions_name varchar(255) NOT NULL,
    role_name        varchar(255) NOT NULL,
    CONSTRAINT role_permissions_pkey PRIMARY KEY (permissions_name, role_name),
    CONSTRAINT fkcppvu8fk24eqqn6q4hws7ajux FOREIGN KEY (role_name) REFERENCES scheduler_application."role" ("name"),
    CONSTRAINT fkf5aljih4mxtdgalvr7xvngfn1 FOREIGN KEY (permissions_name) REFERENCES scheduler_application."permission" ("name")
);


-- scheduler_application.run_job_action_entity definition

-- Drop table

-- DROP TABLE scheduler_application.run_job_action_entity;

CREATE TABLE scheduler_application.run_job_action_entity
(
    action_id int8         NOT NULL,
    job_key   varchar(255) NOT NULL,
    CONSTRAINT run_job_action_entity_pkey PRIMARY KEY (action_id),
    CONSTRAINT fkid9bkxpheo6vkfie40xyk5tn4 FOREIGN KEY (action_id) REFERENCES scheduler_application.action_entity (id)
);


-- scheduler_application.run_job_action_entity_aud definition

-- Drop table

-- DROP TABLE scheduler_application.run_job_action_entity_aud;

CREATE TABLE scheduler_application.run_job_action_entity_aud
(
    rev       int4         NOT NULL,
    action_id int8         NOT NULL,
    job_key   varchar(255) NULL,
    CONSTRAINT run_job_action_entity_aud_pkey PRIMARY KEY (rev, action_id),
    CONSTRAINT fk6ni3a811x284lc5m9cqwsdrye FOREIGN KEY (rev, action_id) REFERENCES scheduler_application.action_entity_aud (rev, id)
);


-- scheduler_application.status_code_validation_entity definition

-- Drop table

-- DROP TABLE scheduler_application.status_code_validation_entity;

CREATE TABLE scheduler_application.status_code_validation_entity
(
    expected_status_code int4 NOT NULL,
    validation_id        int8 NOT NULL,
    CONSTRAINT status_code_validation_entity_expected_status_code_check CHECK (((expected_status_code >= 100) AND (expected_status_code <= 599))),
    CONSTRAINT status_code_validation_entity_pkey PRIMARY KEY (validation_id),
    CONSTRAINT fkcoa8m3dtygu154mwv7lk2aaaj FOREIGN KEY (validation_id) REFERENCES scheduler_application.apivalidation_entity (id)
);


-- scheduler_application.status_code_validation_entity_aud definition

-- Drop table

-- DROP TABLE scheduler_application.status_code_validation_entity_aud;

CREATE TABLE scheduler_application.status_code_validation_entity_aud
(
    expected_status_code int4 NULL,
    rev                  int4 NOT NULL,
    validation_id        int8 NOT NULL,
    CONSTRAINT status_code_validation_entity_aud_pkey PRIMARY KEY (rev, validation_id),
    CONSTRAINT fkdhnehnpfdahn267y5374s5ang FOREIGN KEY (rev, validation_id) REFERENCES scheduler_application.apivalidation_entity_aud (rev, id)
);


-- scheduler_application.task_entity definition

-- Drop table

-- DROP TABLE scheduler_application.task_entity;

CREATE TABLE scheduler_application.task_entity
(
    status               int2          NOT NULL,
    created_at           timestamp(6)  NULL,
    last_updated_at      timestamp(6)  NULL,
    task_id              bigserial     NOT NULL,
    message              varchar(2500) NULL,
    post_actions_message varchar(2500) NULL,
    job_key              varchar(255)  NOT NULL,
    CONSTRAINT task_entity_pkey PRIMARY KEY (task_id),
    CONSTRAINT task_entity_status_check CHECK (((status >= 0) AND (status <= 7))),
    CONSTRAINT fkhjpac5wkyu4x3gwn4vru4u2ax FOREIGN KEY (job_key) REFERENCES scheduler_application.apijob_details_entity ("key")
);


-- scheduler_application.task_entity_aud definition

-- Drop table

-- DROP TABLE scheduler_application.task_entity_aud;

CREATE TABLE scheduler_application.task_entity_aud
(
    rev                  int4         NOT NULL,
    revtype              int2         NULL,
    status               int2         NULL,
    created_at           timestamp(6) NULL,
    last_updated_at      timestamp(6) NULL,
    task_id              int8         NOT NULL,
    job_key              varchar(255) NULL,
    message              varchar(255) NULL,
    post_actions_message varchar(255) NULL,
    CONSTRAINT task_entity_aud_pkey PRIMARY KEY (rev, task_id),
    CONSTRAINT task_entity_aud_status_check CHECK (((status >= 0) AND (status <= 7))),
    CONSTRAINT fk3nk7paopqhardsrsldguqsj49 FOREIGN KEY (rev) REFERENCES scheduler_application.revinfo (rev)
);


-- scheduler_application.task_entity_meta_data definition

-- Drop table

-- DROP TABLE scheduler_application.task_entity_meta_data;

CREATE TABLE scheduler_application.task_entity_meta_data
(
    task_entity_task_id int8         NOT NULL,
    meta_data           varchar(255) NULL,
    meta_data_key       varchar(255) NOT NULL,
    CONSTRAINT task_entity_meta_data_pkey PRIMARY KEY (task_entity_task_id, meta_data_key),
    CONSTRAINT fkse8e09gyge1o0eky5fncx9lcf FOREIGN KEY (task_entity_task_id) REFERENCES scheduler_application.task_entity (task_id)
);


-- scheduler_application.task_entity_meta_data_aud definition

-- Drop table

-- DROP TABLE scheduler_application.task_entity_meta_data_aud;

CREATE TABLE scheduler_application.task_entity_meta_data_aud
(
    rev                 int4         NOT NULL,
    revtype             int2         NULL,
    task_entity_task_id int8         NOT NULL,
    meta_data           varchar(255) NOT NULL,
    meta_data_key       varchar(255) NOT NULL,
    CONSTRAINT task_entity_meta_data_aud_pkey PRIMARY KEY (rev, task_entity_task_id, meta_data, meta_data_key),
    CONSTRAINT fkocdko7g19jvamq70dyu12dbvf FOREIGN KEY (rev) REFERENCES scheduler_application.revinfo (rev)
);


-- scheduler_application.users_permissions definition

-- Drop table

-- DROP TABLE scheduler_application.users_permissions;

CREATE TABLE scheduler_application.users_permissions
(
    user_id          int8         NOT NULL,
    permissions_name varchar(255) NOT NULL,
    CONSTRAINT users_permissions_pkey PRIMARY KEY (user_id, permissions_name),
    CONSTRAINT fk69cfatgplsb0u6rxkfn14fv5b FOREIGN KEY (user_id) REFERENCES scheduler_application.users (id),
    CONSTRAINT fkbnmkkji6sjtiykk1gf28aacft FOREIGN KEY (permissions_name) REFERENCES scheduler_application."permission" ("name")
);


-- scheduler_application.users_roles definition

-- Drop table

-- DROP TABLE scheduler_application.users_roles;

CREATE TABLE scheduler_application.users_roles
(
    user_id    int8         NOT NULL,
    roles_name varchar(255) NOT NULL,
    CONSTRAINT users_roles_pkey PRIMARY KEY (user_id, roles_name),
    CONSTRAINT fk2o0jvgh89lemvvo17cbqvdxaa FOREIGN KEY (user_id) REFERENCES scheduler_application.users (id),
    CONSTRAINT fk7tacasmhqivyolfjjxseeha5c FOREIGN KEY (roles_name) REFERENCES scheduler_application."role" ("name")
);


-- scheduler_application.apijob_details_entity_validations definition

-- Drop table

-- DROP TABLE scheduler_application.apijob_details_entity_validations;

CREATE TABLE scheduler_application.apijob_details_entity_validations
(
    validations_id            int8         NOT NULL,
    apijob_details_entity_key varchar(255) NOT NULL,
    CONSTRAINT apijob_details_entity_validations_validations_id_key UNIQUE (validations_id),
    CONSTRAINT fk4v8bojj4blunv0dlfupdsg0mb FOREIGN KEY (validations_id) REFERENCES scheduler_application.apivalidation_entity (id),
    CONSTRAINT fk8ls2qi8ti569qm8ia7t7qpj8e FOREIGN KEY (apijob_details_entity_key) REFERENCES scheduler_application.apijob_details_entity ("key")
);


-- scheduler_application.qrtz_blob_triggers definition

-- Drop table

-- DROP TABLE scheduler_application.qrtz_blob_triggers;

CREATE TABLE scheduler_application.qrtz_blob_triggers
(
    sched_name    varchar(120) NOT NULL,
    trigger_name  varchar(200) NOT NULL,
    trigger_group varchar(200) NOT NULL,
    blob_data     bytea        NULL,
    CONSTRAINT qrtz_blob_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group),
    CONSTRAINT qrtz_blob_triggers_sched_name_trigger_name_trigger_group_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES scheduler_application.qrtz_triggers (sched_name, trigger_name, trigger_group)
);


-- scheduler_application.qrtz_cron_triggers definition

-- Drop table

-- DROP TABLE scheduler_application.qrtz_cron_triggers;

CREATE TABLE scheduler_application.qrtz_cron_triggers
(
    sched_name      varchar(120) NOT NULL,
    trigger_name    varchar(200) NOT NULL,
    trigger_group   varchar(200) NOT NULL,
    cron_expression varchar(120) NOT NULL,
    time_zone_id    varchar(80)  NULL,
    CONSTRAINT qrtz_cron_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group),
    CONSTRAINT qrtz_cron_triggers_sched_name_trigger_name_trigger_group_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES scheduler_application.qrtz_triggers (sched_name, trigger_name, trigger_group)
);


-- scheduler_application.qrtz_simple_triggers definition

-- Drop table

-- DROP TABLE scheduler_application.qrtz_simple_triggers;

CREATE TABLE scheduler_application.qrtz_simple_triggers
(
    sched_name      varchar(120) NOT NULL,
    trigger_name    varchar(200) NOT NULL,
    trigger_group   varchar(200) NOT NULL,
    repeat_count    int8         NOT NULL,
    repeat_interval int8         NOT NULL,
    times_triggered int8         NOT NULL,
    CONSTRAINT qrtz_simple_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group),
    CONSTRAINT qrtz_simple_triggers_sched_name_trigger_name_trigger_group_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES scheduler_application.qrtz_triggers (sched_name, trigger_name, trigger_group)
);


-- scheduler_application.qrtz_simprop_triggers definition

-- Drop table

-- DROP TABLE scheduler_application.qrtz_simprop_triggers;

CREATE TABLE scheduler_application.qrtz_simprop_triggers
(
    sched_name    varchar(120)   NOT NULL,
    trigger_name  varchar(200)   NOT NULL,
    trigger_group varchar(200)   NOT NULL,
    str_prop_1    varchar(512)   NULL,
    str_prop_2    varchar(512)   NULL,
    str_prop_3    varchar(512)   NULL,
    int_prop_1    int4           NULL,
    int_prop_2    int4           NULL,
    long_prop_1   int8           NULL,
    long_prop_2   int8           NULL,
    dec_prop_1    numeric(13, 4) NULL,
    dec_prop_2    numeric(13, 4) NULL,
    bool_prop_1   bool           NULL,
    bool_prop_2   bool           NULL,
    CONSTRAINT qrtz_simprop_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group),
    CONSTRAINT qrtz_simprop_triggers_sched_name_trigger_name_trigger_grou_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES scheduler_application.qrtz_triggers (sched_name, trigger_name, trigger_group)
);


INSERT INTO scheduler_application."permission" ("name")
VALUES ('user::create'),
       ('user::view::self'),
       ('user::view::all'),
       ('user::permissions::assign'),
       ('user::delete'),
       ('user::password::reset::self'),
       ('user::password::reset::all'),
       ('job::enable'),
       ('job::disable'),
       ('job::delete'),
       ('job::run'),
       ('job::api::create'),
       ('job::api::update'),
       ('job::view'),
       ('jobs::search'),
       ('job::view::status'),
       ('task::view'),
       ('task::status::update'),
       ('tasks::search');

INSERT INTO scheduler_application."role" ("name")
VALUES ('USER'),
       ('ADMIN'),
       ('EXTERNAL_API');

INSERT INTO scheduler_application.role_inherited_roles(role_name, inherited_roles_name)
VALUES ('ADMIN', 'USER');

INSERT INTO scheduler_application.role_permissions (permissions_name, role_name)
VALUES ('user::password::reset::self', 'USER'),
       ('user::view::self', 'USER'),
       ('user::password::reset::all', 'ADMIN'),
       ('user::permissions::assign', 'ADMIN'),
       ('user::view::all', 'ADMIN'),
       ('user::create', 'ADMIN'),
       ('user::delete', 'ADMIN'),
       ('jobs::search', 'USER'),
       ('job::delete', 'USER'),
       ('job::view', 'USER'),
       ('job::enable', 'USER'),
       ('task::view', 'USER'),
       ('tasks::search', 'USER'),
       ('job::api::update', 'USER'),
       ('task::status::update', 'USER'),
       ('job::run', 'USER'),
       ('job::api::create', 'USER'),
       ('job::disable', 'USER'),
       ('job::view::status', 'EXTERNAL_API'),
       ('task::status::update', 'EXTERNAL_API');

INSERT INTO scheduler_application.users
(account_non_expired, account_non_locked, credentials_non_expired, enabled, email, firstname, lastname, "password")
VALUES (true, true, true, true, 'admin@scheduler.cgi.com', 'Admin',
        'Admin',
        '$2a$10$H5xgNpQ8ZWrlkrTrzofTJep21hwn4EHw.bPEOcn.T0WkQpHsDC3mm');

INSERT INTO scheduler_application.users_roles
    (user_id, roles_name)
VALUES ((SELECT ID from scheduler_application.users WHERE email = 'admin@scheduler.cgi.com'), 'ADMIN');