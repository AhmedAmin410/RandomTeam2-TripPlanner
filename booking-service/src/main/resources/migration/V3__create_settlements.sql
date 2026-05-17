-- Migration: Create settlements table for isolated tripdb-bookings database
-- PostgreSQL 17 compatible

CREATE TABLE IF NOT EXISTS settlements (
                                           id BIGSERIAL PRIMARY KEY,
                                           itinerary_id BIGINT NOT NULL,
                                           user_id BIGINT NOT NULL,
                                           amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    settled_at TIMESTAMP WITH TIME ZONE NULL,
                             failure_reason TEXT NULL
                             );

-- Crucial M3 Constraint: Strict unique index to enforce exactly-one settlement per itinerary
CREATE UNIQUE INDEX IF NOT EXISTS uk_settlements_itinerary
    ON settlements (itinerary_id);

-- Performance indices for isolated service lookups
CREATE INDEX IF NOT EXISTS idx_settlements_user_id ON settlements (user_id);
CREATE INDEX IF NOT EXISTS idx_settlements_status ON settlements (status);