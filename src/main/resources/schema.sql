CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_quote_sentence_trgm ON quote USING GIN (sentence gin_trgm_ops);