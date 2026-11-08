CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(64) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    size BIGINT NOT NULL,
    hash VARCHAR(64),
    uploaded_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, filename)
);