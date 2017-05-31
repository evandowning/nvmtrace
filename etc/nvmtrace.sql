CREATE TABLE sample(
    sha256 char(64) UNIQUE NOT NULL PRIMARY KEY,
    submit_time int NOT NULL,
    process_time int
);
