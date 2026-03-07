CREATE TABLE IF NOT EXISTS golf_user (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    tracker_id     VARCHAR(50)  NOT NULL UNIQUE,
    role           VARCHAR(50)  NOT NULL DEFAULT 'USER',
    account_locked BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS password_reset_token (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES golf_user(id)
);

CREATE TABLE IF NOT EXISTS user_round (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    date        DATE         NOT NULL,
    FOREIGN KEY (user_id) REFERENCES golf_user(id),
    UNIQUE (user_id, course_name)
);
