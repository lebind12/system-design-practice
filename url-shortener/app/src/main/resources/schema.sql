CREATE SEQUENCE IF NOT EXISTS url_id_seq START 100000;

CREATE TABLE IF NOT EXISTS urls (
  id          BIGINT       PRIMARY KEY,
  short_key   VARCHAR(16)  UNIQUE NOT NULL,
  long_url    TEXT         NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
