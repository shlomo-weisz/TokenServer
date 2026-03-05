CREATE TABLE notifications (
    notification_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    request_id INT NULL,
    lesson_id INT NULL,
    counterpart_name NVARCHAR(255),
    course_name NVARCHAR(255),
    scheduled_at DATETIME2,
    rejection_reason NVARCHAR(MAX),
    action_path VARCHAR(255),
    is_read BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    read_at DATETIME2,
    CONSTRAINT FK_notifications_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT CHK_notifications_event_type CHECK (event_type IN ('LESSON_REQUEST_APPROVED', 'LESSON_REQUEST_REJECTED'))
);

CREATE INDEX IX_notifications_user_read_created
ON notifications(user_id, is_read, created_at DESC);
