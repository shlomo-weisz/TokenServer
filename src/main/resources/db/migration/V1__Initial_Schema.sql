CREATE TABLE users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    photo_url VARCHAR(500),
    secret_question NVARCHAR(500),
    secret_answer_hash VARCHAR(255),
    about_me_as_teacher NVARCHAR(MAX),
    about_me_as_student NVARCHAR(MAX),
    is_admin BIT NOT NULL DEFAULT 0,
    is_active BIT NOT NULL DEFAULT 1,
    is_blocked_tutor BIT NOT NULL DEFAULT 0,
    available_balance DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (available_balance >= 0),
    locked_balance DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (locked_balance >= 0),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    row_version ROWVERSION
);

CREATE TABLE courses (
    course_id INT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(120),
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

CREATE TABLE user_courses_teacher (
    user_id INT NOT NULL,
    course_id INT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT PK_user_courses_teacher PRIMARY KEY (user_id, course_id),
    CONSTRAINT FK_uct_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT FK_uct_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
);

CREATE TABLE user_courses_student (
    user_id INT NOT NULL,
    course_id INT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT PK_user_courses_student PRIMARY KEY (user_id, course_id),
    CONSTRAINT FK_ucs_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT FK_ucs_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
);

CREATE TABLE availability (
    availability_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    role VARCHAR(20) NOT NULL,
    day VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT FK_av_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT CHK_av_role CHECK (role IN ('teacher', 'student')),
    CONSTRAINT CHK_av_time CHECK (end_time > start_time)
);

CREATE TABLE lesson_requests (
    request_id INT IDENTITY(1,1) PRIMARY KEY,
    student_id INT NOT NULL,
    tutor_id INT NOT NULL,
    course_id INT NOT NULL,
    token_cost DECIMAL(10,2) NOT NULL CHECK (token_cost > 0),
    requested_day VARCHAR(20),
    requested_start_time TIME,
    requested_end_time TIME,
    specific_start_time DATETIME2,
    specific_end_time DATETIME2,
    message NVARCHAR(MAX),
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    rejection_message NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT FK_lr_student FOREIGN KEY (student_id) REFERENCES users(user_id),
    CONSTRAINT FK_lr_tutor FOREIGN KEY (tutor_id) REFERENCES users(user_id),
    CONSTRAINT FK_lr_course FOREIGN KEY (course_id) REFERENCES courses(course_id),
    CONSTRAINT CHK_lr_users CHECK (student_id <> tutor_id),
    CONSTRAINT CHK_lr_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED'))
);

CREATE TABLE lessons (
    lesson_id INT IDENTITY(1,1) PRIMARY KEY,
    request_id INT NOT NULL UNIQUE,
    student_id INT NOT NULL,
    tutor_id INT NOT NULL,
    course_id INT NOT NULL,
    token_cost DECIMAL(10,2) NOT NULL CHECK (token_cost > 0),
    start_time DATETIME2 NOT NULL,
    end_time DATETIME2 NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT FK_l_request FOREIGN KEY (request_id) REFERENCES lesson_requests(request_id),
    CONSTRAINT FK_l_student FOREIGN KEY (student_id) REFERENCES users(user_id),
    CONSTRAINT FK_l_tutor FOREIGN KEY (tutor_id) REFERENCES users(user_id),
    CONSTRAINT FK_l_course FOREIGN KEY (course_id) REFERENCES courses(course_id),
    CONSTRAINT CHK_lesson_time CHECK (end_time > start_time),
    CONSTRAINT CHK_lesson_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE token_transactions (
    tx_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    request_id INT,
    lesson_id INT,
    payer_id INT NOT NULL,
    receiver_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    tx_type VARCHAR(20) NOT NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'SUCCESS',
    message_id VARCHAR(50),
    description NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT FK_tt_request FOREIGN KEY (request_id) REFERENCES lesson_requests(request_id),
    CONSTRAINT FK_tt_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id),
    CONSTRAINT FK_tt_payer FOREIGN KEY (payer_id) REFERENCES users(user_id),
    CONSTRAINT FK_tt_receiver FOREIGN KEY (receiver_id) REFERENCES users(user_id),
    CONSTRAINT CHK_tt_type CHECK (tx_type IN ('PURCHASE', 'BONUS', 'RESERVATION', 'REFUND', 'SETTLEMENT', 'ADMIN_ADJUST', 'TRANSFER')),
    CONSTRAINT CHK_tt_status CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING'))
);

CREATE UNIQUE INDEX UQ_tt_request_type_core
ON token_transactions(request_id, tx_type)
WHERE request_id IS NOT NULL AND tx_type IN ('SETTLEMENT', 'RESERVATION', 'REFUND');

CREATE UNIQUE INDEX UQ_tt_message_id
ON token_transactions(message_id)
WHERE message_id IS NOT NULL;

CREATE TABLE ratings (
    rating_id INT IDENTITY(1,1) PRIMARY KEY,
    lesson_id INT NOT NULL,
    from_user_id INT NOT NULL,
    to_user_id INT NOT NULL,
    score DECIMAL(3,2) NOT NULL CHECK (score >= 1 AND score <= 5),
    comment NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT FK_r_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id),
    CONSTRAINT FK_r_from_user FOREIGN KEY (from_user_id) REFERENCES users(user_id),
    CONSTRAINT FK_r_to_user FOREIGN KEY (to_user_id) REFERENCES users(user_id),
    CONSTRAINT UQ_rating_unique UNIQUE (lesson_id, from_user_id, to_user_id)
);

CREATE TABLE jms_outbox (
    outbox_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id INT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json NVARCHAR(MAX) NOT NULL,
    message_id VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    sent_at DATETIME2,
    error_message NVARCHAR(MAX),
    CONSTRAINT CHK_outbox_status CHECK (status IN ('NEW', 'SENT', 'FAILED'))
);

CREATE TABLE password_reset_tokens (
    token_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    reset_token VARCHAR(80) NOT NULL UNIQUE,
    expires_at DATETIME2 NOT NULL,
    used BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

CREATE TABLE admin_contacts (
    contact_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    subject NVARCHAR(255) NOT NULL,
    message NVARCHAR(MAX) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT FK_contact_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX IX_users_email ON users(email);
CREATE INDEX IX_users_admin ON users(is_admin);
CREATE INDEX IX_users_blocked_tutor ON users(is_blocked_tutor);

CREATE INDEX IX_courses_active ON courses(is_active);
CREATE INDEX IX_courses_name ON courses(name);
CREATE INDEX IX_courses_category ON courses(category);

CREATE INDEX IX_availability_user_role ON availability(user_id, role);

CREATE INDEX IX_lr_student_status_created ON lesson_requests(student_id, status, created_at DESC);
CREATE INDEX IX_lr_tutor_status_created ON lesson_requests(tutor_id, status, created_at DESC);
CREATE INDEX IX_lr_status_updated ON lesson_requests(status, updated_at);

CREATE INDEX IX_lessons_student_start ON lessons(student_id, start_time);
CREATE INDEX IX_lessons_tutor_start ON lessons(tutor_id, start_time);

CREATE INDEX IX_tt_payer_created ON token_transactions(payer_id, created_at DESC);
CREATE INDEX IX_tt_receiver_created ON token_transactions(receiver_id, created_at DESC);

CREATE INDEX IX_ratings_to_user_created ON ratings(to_user_id, created_at DESC);
CREATE INDEX IX_outbox_status_created ON jms_outbox(status, created_at);
CREATE INDEX IX_reset_email ON password_reset_tokens(email, used, expires_at);
CREATE INDEX IX_admin_contacts_user ON admin_contacts(user_id, submitted_at DESC);
