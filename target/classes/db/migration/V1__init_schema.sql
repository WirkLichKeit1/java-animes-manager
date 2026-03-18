-- Users table
CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     NOT NULL DEFAULT NOW(),
    updated_at  DATETIME     NOT NULL DEFAULT NOW()
);

-- Animes table
CREATE TABLE animes (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    original_title   VARCHAR(255),
    synopsis         TEXT,
    genre            VARCHAR(100),
    studio           VARCHAR(100),
    release_year     INT,
    status           VARCHAR(30)  NOT NULL DEFAULT 'ONGOING',
    cover_image_url  VARCHAR(500),
    banner_image_url VARCHAR(500),
    rating           DECIMAL(3,1),
    created_at       DATETIME     NOT NULL DEFAULT NOW(),
    updated_at       DATETIME     NOT NULL DEFAULT NOW()
);

-- Episodes table
CREATE TABLE episodes (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    anime_id         BIGINT       NOT NULL,
    title            VARCHAR(255) NOT NULL,
    episode_number   INT          NOT NULL,
    season_number    INT          NOT NULL DEFAULT 1,
    synopsis         TEXT,
    duration_seconds INT,
    thumbnail_url    VARCHAR(500),
    video_filename   VARCHAR(500),
    video_status     VARCHAR(30)  NOT NULL DEFAULT 'PROCESSING',
    views            BIGINT       NOT NULL DEFAULT 0,
    published        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       DATETIME     NOT NULL DEFAULT NOW(),
    updated_at       DATETIME     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_episode_anime FOREIGN KEY (anime_id) REFERENCES animes(id) ON DELETE CASCADE,
    UNIQUE KEY uq_episode (anime_id, season_number, episode_number)
);

-- Watch history
CREATE TABLE watch_history (
    id               BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT   NOT NULL,
    episode_id       BIGINT   NOT NULL,
    progress_seconds INT      NOT NULL DEFAULT 0,
    completed        BOOLEAN  NOT NULL DEFAULT FALSE,
    watched_at       DATETIME NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_episode FOREIGN KEY (episode_id) REFERENCES episodes(id) ON DELETE CASCADE,
    UNIQUE KEY uq_watch_history (user_id, episode_id)
);

-- Favorites
CREATE TABLE favorites (
    id         BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT   NOT NULL,
    anime_id   BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_anime FOREIGN KEY (anime_id) REFERENCES animes(id) ON DELETE CASCADE,
    UNIQUE KEY uq_favorite (user_id, anime_id)
);

-- Indexes
CREATE INDEX idx_episodes_anime_id ON episodes(anime_id);
CREATE INDEX idx_watch_history_user_id ON watch_history(user_id);
CREATE INDEX idx_watch_history_episode_id ON watch_history(episode_id);
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_animes_title ON animes(title);
CREATE INDEX idx_animes_genre ON animes(genre);

-- Seed admin user (password = "admin123")
INSERT INTO users (username, email, password, role)
VALUES ('admin', 'admin@animeapi.com',
        '$2a$12$w.p6L6iYKPBGLcr2dFoHfOMVDDPGR4vJ0TQAiU3hJm9xlV6IB8Jdu',
        'ADMIN');

-- Seed regular user (password = "user123")
INSERT INTO users (username, email, password, role)
VALUES ('user', 'user@animeapi.com',
        '$2a$12$JAvuHNc6FqRITAFnC.JeHu4E3t3gvGxPrOT7r0EhI6XRnBUBSJPJy',
        'USER');