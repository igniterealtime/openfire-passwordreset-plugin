CREATE TABLE IF NOT EXISTS ofPasswordResetToken
(
    token         VARCHAR(32) NOT NULL,
    userId        VARCHAR(64) NOT NULL,
    sourceAddress VARCHAR(45) NOT NULL,
    expires       TIMESTAMP   NOT NULL,
    PRIMARY KEY (token)
);

INSERT INTO ofVersion (name, version)
VALUES ('password-reset', 1);