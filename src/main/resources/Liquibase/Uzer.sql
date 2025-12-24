--liquibase formatted sql

--changeset: 1
//CREATE INDEX idx_student_name ON student(name);

--changeset: 2
//CREATE INDEX idx_faculty_title_color ON faculty(title, color);