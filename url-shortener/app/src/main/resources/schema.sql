CREATE SEQUENCE IF NOT EXISTS url_id_seq START 100000;

CREATE TABLE IF NOT EXISTS urls (
  id          BIGINT       PRIMARY KEY,
  short_key   VARCHAR(16)  UNIQUE NOT NULL,
  long_url    TEXT         NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Functional md5 index used by experiment 3 (DbDedupStrategy).
-- Present unconditionally so the schema is stable across variants; unused
-- when DEDUP_MODE is off, so cost to MVP is only the extra index maintenance
-- on inserts (measured separately in the experiment 3 writeup).
CREATE INDEX IF NOT EXISTS idx_urls_long_url_md5 ON urls (md5(long_url));
