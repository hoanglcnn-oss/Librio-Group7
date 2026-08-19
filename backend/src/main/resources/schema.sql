DROP TABLE IF EXISTS borrowing CASCADE;
DROP TABLE IF EXISTS digital_item CASCADE;
DROP TABLE IF EXISTS physical_item CASCADE;
DROP TABLE IF EXISTS resource CASCADE;
DROP TABLE IF EXISTS user_account CASCADE;

CREATE TABLE resource (
    id BIGINT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    authors VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE TABLE physical_item (
    id BIGINT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_physical_resource FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE CASCADE
);

CREATE TABLE digital_item (
    id BIGINT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    CONSTRAINT fk_digital_resource FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE CASCADE
);

CREATE TABLE user_account (
    id BIGINT PRIMARY KEY,
    username VARCHAR(100) NOT NULL
);

CREATE TABLE borrowing (
    id BIGINT PRIMARY KEY,
    physical_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    borrowed_at TIMESTAMP NOT NULL,
    due_at TIMESTAMP NOT NULL,
    returned_at TIMESTAMP,
    CONSTRAINT fk_borrowing_physical FOREIGN KEY (physical_item_id) REFERENCES physical_item(id),
    CONSTRAINT fk_borrowing_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);
