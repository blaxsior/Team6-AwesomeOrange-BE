SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE url;
TRUNCATE TABLE admin_user;

TRUNCATE TABLE draw_event_score_policy;
TRUNCATE TABLE draw_event_metadata;
TRUNCATE TABLE draw_event_winning_info;
TRUNCATE TABLE event_participation_info;
TRUNCATE TABLE draw_event;

TRUNCATE TABLE fcfs_event_winning_info;
TRUNCATE TABLE fcfs_event;
TRUNCATE TABLE comment;
TRUNCATE TABLE event_user;
TRUNCATE TABLE event_metadata;
TRUNCATE TABLE event_frame;

ALTER TABLE url AUTO_INCREMENT = 1;
ALTER TABLE admin_user AUTO_INCREMENT = 1;

ALTER TABLE draw_event_score_policy AUTO_INCREMENT = 1;
ALTER TABLE draw_event_metadata AUTO_INCREMENT = 1;
ALTER TABLE draw_event_winning_info AUTO_INCREMENT = 1;
ALTER TABLE event_participation_info AUTO_INCREMENT = 1;
ALTER TABLE draw_event AUTO_INCREMENT = 1;

ALTER TABLE fcfs_event_winning_info AUTO_INCREMENT = 1;
ALTER TABLE fcfs_event AUTO_INCREMENT = 1;
ALTER TABLE comment AUTO_INCREMENT = 1;
ALTER TABLE event_user AUTO_INCREMENT = 1;
ALTER TABLE event_metadata AUTO_INCREMENT = 1;
ALTER TABLE event_frame AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;