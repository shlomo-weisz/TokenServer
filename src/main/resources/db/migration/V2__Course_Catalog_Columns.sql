ALTER TABLE courses
    ADD course_number VARCHAR(20) NULL;
GO

ALTER TABLE courses
    ADD name_he NVARCHAR(255) NOT NULL CONSTRAINT DF_courses_name_he DEFAULT N'';
GO

ALTER TABLE courses
    ADD name_en VARCHAR(255) NOT NULL CONSTRAINT DF_courses_name_en DEFAULT '';
GO

UPDATE courses
SET name_he = CASE
    WHEN LTRIM(RTRIM(name_he)) = '' THEN name
    ELSE name_he
END;
GO

UPDATE courses
SET name_en = CASE
    WHEN LTRIM(RTRIM(name_en)) = '' THEN CAST(name AS VARCHAR(255))
    ELSE name_en
END;
GO

ALTER TABLE courses
    ADD CONSTRAINT CK_courses_number_not_blank CHECK (course_number IS NULL OR LTRIM(RTRIM(course_number)) <> '');
GO

CREATE UNIQUE INDEX UX_courses_course_number ON courses(course_number) WHERE course_number IS NOT NULL;
CREATE INDEX IX_courses_name_he ON courses(name_he);
CREATE INDEX IX_courses_name_en ON courses(name_en);
