CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(100) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    profile_image BYTEA,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL
);

/*
INSERT INTO users (email, username, profileImage, password, role)
VALUES
    ('qazyj@test.com', 'teddy.kim', decode('FFD8FFE000104A464946', 'hex'), 'password123', 'ADMIN'),
    ('test@test.com', 'noe', decode('89504E470D0A1A0A0000', 'hex'), 'password123', 'USER')*/