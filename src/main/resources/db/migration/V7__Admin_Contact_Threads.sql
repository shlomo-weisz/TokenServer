ALTER TABLE notifications
ADD contact_id BIGINT NULL;

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
    'LESSON_MESSAGE',
    'ADMIN_CONTACT_MESSAGE'
));

ALTER TABLE notifications
ADD CONSTRAINT FK_notifications_contact
FOREIGN KEY (contact_id) REFERENCES admin_contacts(contact_id) ON DELETE CASCADE;

CREATE INDEX IX_notifications_user_contact_created
ON notifications(user_id, contact_id, created_at DESC);
