-- ══════════════════════════════════════════════════════════
-- Flyway Migration V1 — Initial Schema
-- Used by: prod profile via spring.flyway.enabled=true
-- ══════════════════════════════════════════════════════════

CREATE TABLE urls (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    short_code      VARCHAR(10)     UNIQUE NULL,
    url_hash        VARCHAR(64)     UNIQUE NOT NULL,
    long_url        VARCHAR(2048)   NOT NULL,
    click_count     BIGINT          DEFAULT 0,
    is_active       BOOLEAN         DEFAULT TRUE,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    last_accessed   TIMESTAMP       NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_short_code   ON urls(short_code);
CREATE INDEX idx_url_hash     ON urls(url_hash);
CREATE INDEX idx_is_active    ON urls(is_active);
