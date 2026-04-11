DO $$ BEGIN
    CREATE TYPE activity_category AS ENUM
    ('SIGHTSEEING', 'ADVENTURE', 'DINING', 'CULTURAL', 'LEISURE');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE TABLE IF NOT EXISTS activities (
    id BIGSERIAL PRIMARY KEY,
    itinerary_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    category activity_category NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    scheduled_time TIMESTAMP NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL
);

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