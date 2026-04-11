CREATE TABLE IF NOT EXISTS itineraries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    destination_id BIGINT,
    title VARCHAR(255),
    status VARCHAR(255),
    estimated_budget DOUBLE PRECISION,
    metadata JSONB,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP
);

DO $$ BEGIN
    CREATE TYPE activity_category AS ENUM
    ('SIGHTSEEING', 'ADVENTURE', 'DINING', 'CULTURAL', 'LEISURE');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;