-- V3__create_settlements.sql
-- PostgreSQL 17 DDL script to create settlements table for S5-READ-DB architecture

CREATE TABLE IF NOT EXISTS settlements (
    id BIGSERIAL PRIMARY KEY,
    itinerary_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    status VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settled_at TIMESTAMP,
    failure_reason TEXT
);

-- Strict UNIQUE INDEX on itinerary_id for atomic isolation guarantees
CREATE UNIQUE INDEX IF NOT EXISTS uk_settlements_itinerary
    ON settlements(itinerary_id);

-- Index for common queries by user_id and status
CREATE INDEX IF NOT EXISTS idx_settlements_user_status
    ON settlements(user_id, status);

-- Index for created_at range queries
CREATE INDEX IF NOT EXISTS idx_settlements_created_at
    ON settlements(created_at DESC);

-- Index for status filtering
CREATE INDEX IF NOT EXISTS idx_settlements_status
    ON settlements(status);

