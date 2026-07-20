CREATE TABLE teachers (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE classes (
    id VARCHAR(255) PRIMARY KEY,
    teacher_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    join_code VARCHAR(10) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    minimum_ranking_count INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_classes_teacher
        FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE CASCADE
);

CREATE INDEX idx_classes_teacher_id ON classes (teacher_id);

CREATE TABLE books (
    id VARCHAR(255) PRIMARY KEY,
    class_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_books_capacity CHECK (capacity > 0),
    CONSTRAINT fk_books_class
        FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE CASCADE
);

CREATE INDEX idx_books_class_id ON books (class_id);

CREATE TABLE students (
    id VARCHAR(255) PRIMARY KEY,
    class_id VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_students_class_username UNIQUE (class_id, username),
    CONSTRAINT fk_students_class
        FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE CASCADE
);

CREATE INDEX idx_students_class_id ON students (class_id);

CREATE TABLE rankings (
    id VARCHAR(255) PRIMARY KEY,
    student_id VARCHAR(255) NOT NULL,
    book_id VARCHAR(255) NOT NULL,
    rank_value INTEGER NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_rankings_student_book UNIQUE (student_id, book_id),
    CONSTRAINT uk_rankings_student_rank UNIQUE (student_id, rank_value),
    CONSTRAINT fk_rankings_student
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_rankings_book
        FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE
);

CREATE INDEX idx_rankings_student_id ON rankings (student_id);
CREATE INDEX idx_rankings_book_id ON rankings (book_id);

CREATE TABLE assignment_runs (
    id VARCHAR(255) PRIMARY KEY,
    class_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    algorithm_version VARCHAR(50),
    total_cost INTEGER NOT NULL,
    satisfaction_score DOUBLE PRECISION NOT NULL,
    first_choice_count INTEGER NOT NULL,
    top_three_count INTEGER NOT NULL,
    worse_than_third_count INTEGER NOT NULL,
    unassigned_student_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_assignment_runs_class
        FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE CASCADE
);

CREATE INDEX idx_assignment_runs_class_id ON assignment_runs (class_id);

CREATE TABLE assignments (
    id VARCHAR(255) PRIMARY KEY,
    assignment_run_id VARCHAR(255) NOT NULL,
    student_id VARCHAR(255) NOT NULL,
    book_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_assignments_run_student UNIQUE (assignment_run_id, student_id),
    CONSTRAINT fk_assignments_run
        FOREIGN KEY (assignment_run_id) REFERENCES assignment_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_student
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_book
        FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE
);

CREATE INDEX idx_assignments_student_id ON assignments (student_id);
CREATE INDEX idx_assignments_book_id ON assignments (book_id);
