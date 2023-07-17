-- liquibase formatted sql

-- changeset wkrzywiec:1689569361000-1
-- comment create inbox table
CREATE TABLE inbox
(
    message             JSONB NOT NULL,
    channel             VARCHAR(256) NOT NULL,
    publish_timestamp   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
)