-- ============================================================
-- V4 — Remove crm_telephony_config table
-- Global telephony provider config moved to telephony.yml
-- (env vars: TELEPHONY_GLOBAL_PROVIDER, TWILIO_*)
-- ============================================================

drop table if exists crm_telephony_config;
