INSERT INTO event_frame(frame_id, name) VALUES ('frame_id1', 'test');

INSERT INTO event_metadata(event_type, event_frame_id, event_id)
VALUES (1, 1, 'HD_240808_001');

INSERT INTO draw_event(event_metadata_id, is_drawn)
VALUES (1, 0);

INSERT INTO event_metadata(event_type, event_frame_id, event_id)
VALUES (1, 1, 'HD_240808_002');

INSERT INTO draw_event(event_metadata_id, is_drawn)
VALUES (2, 0);

INSERT INTO event_user(score, event_frame_id, user_id)
VALUES (0, 1, 'user1');

INSERT INTO event_user(score, event_frame_id, user_id)
VALUES (0, 1, 'user2');

INSERT INTO event_user(score, event_frame_id, user_id)
VALUES (0, 1, 'user3');

INSERT INTO event_user(score, event_frame_id, user_id)
VALUES (0, 1, 'user4');

INSERT INTO event_user(score, event_frame_id, user_id)
VALUES (0, 1, 'user5');

INSERT INTO event_user(score, event_frame_id, user_id)
VALUES (0, 1, 'user6');