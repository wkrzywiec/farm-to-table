-- liquibase formatted sql

-- changeset wkrzywiec:1689569361000-1
-- comment create inbox table
CREATE TABLE inbox
(
    id                  VARCHAR(256) NOT NULL,
    channel             VARCHAR(256) NOT NULL,
    message             JSONB NOT NULL,
    publish_timestamp   TIMESTAMP WITH TIME ZONE NOT NULL
)