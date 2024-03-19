INSERT INTO scheduler_application.users (id, account_non_expired, account_non_locked, credentials_non_expired, enabled,
                                         email, firstname, lastname, "password")
VALUES (true, true, true, true, 'external-api@juror-scheduler-api.hmcts.net', 'System', 'Juror-Api',
        '$2a$10$QSvRxqNPA/SXm6vcWZ3a.eMijGVX9EGY7zKdXF.bOqF3MA/IFmKGa');

INSERT INTO scheduler_application.users_roles (user_id, roles_name)
VALUES (currval('scheduler_application.users_id_seq1'::regclass), 'EXTERNAL_API');
