DROP TYPE IF EXISTS interval_id;
CREATE TYPE interval_id AS ENUM ('Minutes', 'Hours', 'Days', 'Weeks', 'Months');
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE TABLE IF NOT EXISTS service_point_expiration_period
(
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    duration integer NOT NULL,
    interval_id interval_id NOT NULL,
    CONSTRAINT service_point_expiration_period_pkey PRIMARY KEY (id)
);