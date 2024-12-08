-- 필요 시 사용
-- DROP TABLE IF EXISTS user_server_memberships;
-- DROP TABLE IF EXISTS channels;
-- DROP TABLE IF EXISTS servers;
-- DROP TABLE IF EXISTS users;

-- users 테이블 생성
CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(100) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    profile_image VARCHAR(100),
    user_id BIGSERIAL,
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

-- channels 테이블 재정의 (ENUM 대신 VARCHAR 사용)
CREATE TABLE IF NOT EXISTS channels (
    channel_id SERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES servers(server_id) ON DELETE CASCADE,
    channel_name VARCHAR(100) NOT NULL,
    channel_category VARCHAR(20) NOT NULL,  -- ENUM 대신 VARCHAR 사용
    channel_position INTEGER NOT NULL
    );

-- 순서 제약 추가
CREATE UNIQUE INDEX IF NOT EXISTS idx_channel_position
    ON channels(server_id, channel_position);

-- 같은 서버, 같은 카테고리 내에서만 채널명 유니크하도록 변경
CREATE UNIQUE INDEX IF NOT EXISTS idx_channel_category_name
    ON channels(server_id, channel_category, channel_name);