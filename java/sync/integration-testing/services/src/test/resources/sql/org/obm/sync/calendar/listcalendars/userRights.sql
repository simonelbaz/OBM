INSERT INTO UserObm (userobm_domain_id, userobm_login, userobm_password, userobm_password_type, userobm_perms, userobm_lastname, userobm_firstname, userobm_uid, userobm_gid, userobm_archive, userobm_email) 
  VALUES
    (1, 'userp','userp','PLAIN','user', 'Lastname_p', 'Firstname', '1000', '512', '0', 'userp'),
    (1, 'userq','userq','PLAIN','user', 'Lastname_q', 'Firstname', '1000', '512', '0', 'userq'),
    (1, 'userr','userr','PLAIN','user', 'Lastname_r', 'Firstname', '1000', '512', '0', 'userr'),
    (1, 'users','users','PLAIN','user', 'Lastname_s', 'Firstname', '1000', '512', '0', 'users'),
    (1, 'usert','usert','PLAIN','user', 'Lastname_t', 'Firstname', '1000', '512', '0', 'usert'),
    (1, 'testuser1','testuser1','PLAIN','user', 'TestLastname', 'TestFirstname1', '1000', '512', '0', 'testuser1'),
    (1, 'testuser2','testuser2','PLAIN','user', 'TestLastname', 'TestFirstname2', '1000', '512', '0', 'testuser2'),
    (1, 'testuser3','testuser3','PLAIN','user', 'TestLastname', 'TestFirstname3', '1000', '512', '0', 'testuser3'),
    (1, 'testuser4','testuser4','PLAIN','user', 'TestLastname', 'TestFirstname4', '1000', '512', '0', 'testuser4'),
    (1, 'testuser5','testuser5','PLAIN','user', 'TestLastname', 'TestFirstname5', '1000', '512', '0', 'testuser5');

INSERT INTO Entity (entity_mailing)
  VALUES
    (TRUE), (TRUE), (TRUE), (TRUE), (TRUE), (TRUE), (TRUE), (TRUE), (TRUE), (TRUE);

INSERT INTO UserEntity (userentity_entity_id, userentity_user_id)
  VALUES
    (23, 20), (24, 21), (25, 22), (26, 23), (27, 24), (28, 25), (29, 26), (30, 27), (31, 28), (32, 29);

INSERT INTO CalendarEntity (calendarentity_entity_id, calendarentity_calendar_id)
  VALUES
    (23, 20), (24, 21), (25, 22), (26, 23), (27, 24), (28, 25), (29, 26), (30, 27), (31, 28), (32, 29);

INSERT INTO EntityRight (entityright_entity_id, entityright_consumer_id, entityright_access, entityright_read, entityright_write, entityright_admin)
  VALUES
    (23, 1, 1, 1, 1, 0),
    (24, 1, 1, 1, 1, 0),
    (25, 1, 1, 1, 0, 0),
    (26, 1, 1, 1, 0, 0),
    (27, 1, 1, 1, 0, 0),
    (28, 1, 1, 1, 0, 0),
    (29, 1, 1, 1, 0, 0),
    (30, 1, 1, 1, 0, 0),
    (31, 1, 1, 1, 0, 0),
    (32, 1, 1, 1, 0, 0);