-- Separate schemas for service data ownership (ADR-002)
CREATE SCHEMA IF NOT EXISTS account_service;
CREATE SCHEMA IF NOT EXISTS transaction_service;

GRANT ALL ON SCHEMA account_service TO fintrack;
GRANT ALL ON SCHEMA transaction_service TO fintrack;
