-- liquibase formatted sql

-- changeset wkrzywiec:1690866150000-1
-- comment create events table
CREATE TABLE IF NOT EXISTS events
 (
    id                  VARCHAR(256) NOT NULL,
    stream_id           VARCHAR(256) NOT NULL,
    version             BIGINT NOT NULL,
    channel             VARCHAR(256) NOT NULL,
    type                VARCHAR(256) NOT NULL,
    body                JSONB NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL
);