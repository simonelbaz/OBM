UPDATE ObmInfo SET obminfo_value = '2.4.1-pre' WHERE obminfo_name = 'db_version';

DROP TABLE IF EXISTS opush_invitation_mapping;

ALTER TABLE Domain ADD COLUMN domain_uuid CHAR(36) NOT NULL;
UPDATE Domain SET domain_uuid=UUID() WHERE domain_uuid='';

ALTER TABLE P_Domain ADD COLUMN domain_uuid CHAR(36);
UPDATE P_Domain p, Domain d SET p.domain_uuid=d.domain_uuid where p.domain_id=d.domain_id;
ALTER TABLE P_Domain MODIFY domain_uuid CHAR(36) NOT NULL;

UPDATE opush_sync_mail SET timestamp='1970-01-01 01:00:01' WHERE timestamp='0000-00-00 00:00:00';

-- we want to substitute event_ext_id to event_id in opush_event_mapping without losing
-- data in it

ALTER TABLE opush_event_mapping ADD COLUMN event_ext_id varchar(300);
UPDATE opush_event_mapping, Event SET opush_event_mapping.event_ext_id = Event.event_ext_id WHERE Event.event_id = opush_event_mapping.event_id;
ALTER TABLE opush_event_mapping MODIFY event_ext_id varchar(300) NOT NULL;
ALTER TABLE opush_event_mapping DROP FOREIGN KEY opush_event_mapping_event_id_event_id_fkey;
ALTER TABLE opush_event_mapping DROP INDEX opush_event_mapping_unique;
ALTER TABLE opush_event_mapping DROP COLUMN event_id;

-- we want (device_id, event_ext_id) to be unique but the key is too long for mysql
-- we implement this by using a "hash" trigger and putting the hash in a unique key

ALTER TABLE EventLink ADD COLUMN eventlink_comment VARCHAR(255);

-- prevent lock timeout on mail deletion
CREATE INDEX opush_sync_mail_tuple_index ON opush_sync_mail (collection_id, device_id, mail_uid);

UPDATE ObmInfo SET obminfo_value = '2.4.1' WHERE obminfo_name = 'db_version';
