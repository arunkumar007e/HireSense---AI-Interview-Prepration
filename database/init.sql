-- HireSense database bootstrap.
-- Runs only once, when the PostgreSQL volume is first created.
-- Application tables are created automatically by Hibernate (ddl-auto: update).

CREATE TABLE IF NOT EXISTS app_meta (
    id SERIAL PRIMARY KEY,
    key VARCHAR(100) NOT NULL UNIQUE,
    value VARCHAR(255)
);

INSERT INTO app_meta (key, value)
VALUES ('initialized_at', NOW()::text)
ON CONFLICT (key) DO NOTHING;