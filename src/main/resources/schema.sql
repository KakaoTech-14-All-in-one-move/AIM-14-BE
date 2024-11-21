-- users 테이블 생성
CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(100) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    profile_image VARCHAR(100),
    password VARCHAR(100),
    role VARCHAR(20) NOT NULL
    );

CREATE TABLE IF NOT EXISTS servers (
    server_id SERIAL PRIMARY KEY,
    server_name VARCHAR(100) NOT NULL,
    server_image VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS user_server_memberships (
    email VARCHAR(100) REFERENCES users(email),
    server_id BIGINT REFERENCES servers(server_id),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (email, server_id)
    );