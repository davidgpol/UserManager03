CREATE TABLE user_record (
    id uuid PRIMARY KEY,
    first_name text NOT NULL,
    last_name text NOT NULL,
    dni text NOT NULL CONSTRAINT user_record_dni_key UNIQUE,
    email text NOT NULL,
    first_name_search text NOT NULL,
    last_name_search text NOT NULL,
    email_search text NOT NULL,
    active boolean NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT user_record_audit CHECK (updated_at >= created_at)
);
CREATE INDEX user_record_page_idx ON user_record(created_at, id);
CREATE INDEX user_record_active_page_idx ON user_record(active, created_at, id);
