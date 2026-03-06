ALTER TABLE notifications
ADD message_body NVARCHAR(MAX) NULL,
    sender_user_id INT NULL;

ALTER TABLE notifications
DROP CONSTRAINT CHK_notifications_event_type;

ALTER TABLE notifications
ADD CONSTRAINT CHK_notifications_event_type
CHECK (event_type IN (
    'LESSON_REQUEST_CREATED',
    'LESSON_REQUEST_APPROVED',
    'LESSON_REQUEST_REJECTED',
    'LESSON_CANCELLED',
    'LESSON_REMINDER',
    'LESSON_MESSAGE'
));

CREATE INDEX IX_notifications_user_lesson_created
ON notifications(user_id, lesson_id, created_at DESC);
