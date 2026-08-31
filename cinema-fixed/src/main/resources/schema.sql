-- ============================================================
-- Cinema Management System - Database Initialization Script
-- Run this ONCE before starting the application for the first time
-- ============================================================

CREATE DATABASE IF NOT EXISTS cinema_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cinema_db;

-- ---- Users ----
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL
);

-- ---- Programs ----
CREATE TABLE IF NOT EXISTS programs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    creation_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    state VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    creator_id BIGINT NOT NULL,
    CONSTRAINT fk_program_creator FOREIGN KEY (creator_id) REFERENCES users(id)
);

-- ---- User-Program Roles ----
CREATE TABLE IF NOT EXISTS user_program_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    program_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT fk_role_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_role_program FOREIGN KEY (program_id) REFERENCES programs(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_program UNIQUE (user_id, program_id)
);

-- ---- Screenings ----
CREATE TABLE IF NOT EXISTS screenings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creation_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    state VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    program_id BIGINT NOT NULL,
    submitter_id BIGINT NOT NULL,
    handler_id BIGINT,
    film_title VARCHAR(300),
    film_cast VARCHAR(1000),
    film_genre VARCHAR(300),
    film_duration_minutes INT,
    auditorium_name VARCHAR(200),
    start_time DATETIME,
    end_time DATETIME,
    review_score INT,
    review_comments TEXT,
    approval_notes TEXT,
    rejection_reason TEXT,
    finally_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_screening_program FOREIGN KEY (program_id) REFERENCES programs(id),
    CONSTRAINT fk_screening_submitter FOREIGN KEY (submitter_id) REFERENCES users(id),
    CONSTRAINT fk_screening_handler FOREIGN KEY (handler_id) REFERENCES users(id)
);

-- ---- Indexes for search performance ----
CREATE INDEX idx_programs_state ON programs(state);
CREATE INDEX idx_programs_dates ON programs(start_date, end_date);
CREATE INDEX idx_screenings_program ON screenings(program_id);
CREATE INDEX idx_screenings_state ON screenings(state);
CREATE INDEX idx_screenings_submitter ON screenings(submitter_id);
CREATE INDEX idx_screenings_handler ON screenings(handler_id);
