-- ══════════════════════════════════════════════════════════
-- URL Shortener — Database Schema
-- Used by: dev & docker profiles via spring.sql.init.mode
-- ══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS urls (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    short_code      VARCHAR(10)     UNIQUE NULL,
    url_hash        VARCHAR(64)     UNIQUE NOT NULL,
    long_url        VARCHAR(2048)   NOT NULL,
    click_count     BIGINT          DEFAULT 0,
    is_active       BOOLEAN         DEFAULT TRUE,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    last_accessed   TIMESTAMP       NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_short_code   ON urls(short_code);
CREATE INDEX IF NOT EXISTS idx_url_hash     ON urls(url_hash);
CREATE INDEX IF NOT EXISTS idx_is_active    ON urls(is_active);
