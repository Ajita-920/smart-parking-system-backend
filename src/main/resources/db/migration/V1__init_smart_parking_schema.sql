-- V1__init_smart_parking_schema.sql
-- PostgreSQL / Supabase production schema for SmartParking
-- Generated from uploaded JPA entities.
--
-- Run this only on an empty production database, or review carefully before running.
-- If using Flyway, place this file at:
-- src/main/resources/db/migration/V1__init_smart_parking_schema.sql

BEGIN;

-- Needed for gen_random_uuid() on most PostgreSQL/Supabase setups.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Shared updated_at trigger helper.
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

-- =========================================================
-- users
-- =========================================================
CREATE TABLE IF NOT EXISTS public.users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at timestamp(6) with time zone DEFAULT now(),
    updated_at timestamp(6) with time zone DEFAULT now(),
    deleted_at timestamp(6) with time zone,

    name varchar(100) NOT NULL,
    email varchar(150) NOT NULL,
    password varchar(255) NOT NULL,
    phone varchar(20),
    role varchar(10) NOT NULL DEFAULT 'DRIVER',
    banned boolean NOT NULL DEFAULT false,
    approved boolean NOT NULL DEFAULT true,

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'DRIVER', 'VENDOR'))
);

CREATE INDEX IF NOT EXISTS idx_users_email ON public.users (email);
CREATE INDEX IF NOT EXISTS idx_users_role ON public.users (role);

DROP TRIGGER IF EXISTS trg_users_set_updated_at ON public.users;
CREATE TRIGGER trg_users_set_updated_at
BEFORE UPDATE ON public.users
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- blacklisted_tokens
-- =========================================================
CREATE TABLE IF NOT EXISTS public.blacklisted_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at timestamp(6) with time zone DEFAULT now(),
    updated_at timestamp(6) with time zone DEFAULT now(),
    deleted_at timestamp(6) with time zone,

    jti varchar(100) NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,

    CONSTRAINT uk_blacklisted_tokens_jti UNIQUE (jti)
);

CREATE INDEX IF NOT EXISTS idx_blacklisted_tokens_jti ON public.blacklisted_tokens (jti);
CREATE INDEX IF NOT EXISTS idx_blacklisted_tokens_expires_at ON public.blacklisted_tokens (expires_at);

DROP TRIGGER IF EXISTS trg_blacklisted_tokens_set_updated_at ON public.blacklisted_tokens;
CREATE TRIGGER trg_blacklisted_tokens_set_updated_at
BEFORE UPDATE ON public.blacklisted_tokens
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- refresh_tokens
-- =========================================================
CREATE TABLE IF NOT EXISTS public.refresh_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at timestamp(6) with time zone DEFAULT now(),
    updated_at timestamp(6) with time zone DEFAULT now(),
    deleted_at timestamp(6) with time zone,

    user_id uuid NOT NULL,
    token_hash varchar(255) NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    revoked_at timestamp(6) with time zone,
    replaced_by_token_hash varchar(255),
    device_info varchar(255),

    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES public.users (id)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON public.refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON public.refresh_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON public.refresh_tokens (expires_at);

DROP TRIGGER IF EXISTS trg_refresh_tokens_set_updated_at ON public.refresh_tokens;
CREATE TRIGGER trg_refresh_tokens_set_updated_at
BEFORE UPDATE ON public.refresh_tokens
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- parking_locations
-- =========================================================
CREATE TABLE IF NOT EXISTS public.parking_locations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at timestamp(6) with time zone DEFAULT now(),
    updated_at timestamp(6) with time zone DEFAULT now(),
    deleted_at timestamp(6) with time zone,

    name varchar(100) NOT NULL,
    address varchar(255) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,

    total_four_wheeler_slots integer NOT NULL,
    available_four_wheeler_slots integer NOT NULL,
    total_two_wheeler_slots integer NOT NULL,
    available_two_wheeler_slots integer NOT NULL,

    two_wheeler_rate_per_hour double precision DEFAULT 50.0,
    four_wheeler_rate_per_hour double precision DEFAULT 100.0,

    vendor_id uuid NOT NULL,

    CONSTRAINT fk_parking_locations_vendor
        FOREIGN KEY (vendor_id)
        REFERENCES public.users (id),

    CONSTRAINT chk_parking_locations_latitude
        CHECK (latitude >= -90.0 AND latitude <= 90.0),

    CONSTRAINT chk_parking_locations_longitude
        CHECK (longitude >= -180.0 AND longitude <= 180.0),

    CONSTRAINT chk_parking_locations_total_four_wheeler_slots
        CHECK (total_four_wheeler_slots >= 0),

    CONSTRAINT chk_parking_locations_available_four_wheeler_slots
        CHECK (available_four_wheeler_slots >= 0),

    CONSTRAINT chk_parking_locations_total_two_wheeler_slots
        CHECK (total_two_wheeler_slots >= 0),

    CONSTRAINT chk_parking_locations_available_two_wheeler_slots
        CHECK (available_two_wheeler_slots >= 0),

    CONSTRAINT chk_parking_locations_available_four_wheeler_not_greater
        CHECK (available_four_wheeler_slots <= total_four_wheeler_slots),

    CONSTRAINT chk_parking_locations_available_two_wheeler_not_greater
        CHECK (available_two_wheeler_slots <= total_two_wheeler_slots),

    CONSTRAINT chk_parking_locations_two_wheeler_rate_positive
        CHECK (two_wheeler_rate_per_hour IS NULL OR two_wheeler_rate_per_hour > 0.0),

    CONSTRAINT chk_parking_locations_four_wheeler_rate_positive
        CHECK (four_wheeler_rate_per_hour IS NULL OR four_wheeler_rate_per_hour > 0.0)
);

CREATE INDEX IF NOT EXISTS idx_parking_locations_vendor_id ON public.parking_locations (vendor_id);
CREATE INDEX IF NOT EXISTS idx_parking_locations_name ON public.parking_locations (name);

DROP TRIGGER IF EXISTS trg_parking_locations_set_updated_at ON public.parking_locations;
CREATE TRIGGER trg_parking_locations_set_updated_at
BEFORE UPDATE ON public.parking_locations
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- parking_slots
-- =========================================================
CREATE TABLE IF NOT EXISTS public.parking_slots (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at timestamp(6) with time zone DEFAULT now(),
    updated_at timestamp(6) with time zone DEFAULT now(),
    deleted_at timestamp(6) with time zone,

    slot_number varchar(30) NOT NULL,
    vehicle_type varchar(20) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'AVAILABLE',
    location_id uuid NOT NULL,

    CONSTRAINT fk_parking_slots_location
        FOREIGN KEY (location_id)
        REFERENCES public.parking_locations (id),

    CONSTRAINT uk_parking_slot_location_slot_number
        UNIQUE (location_id, slot_number),

    CONSTRAINT chk_parking_slots_vehicle_type
        CHECK (vehicle_type IN ('TWO_WHEELER', 'FOUR_WHEELER')),

    CONSTRAINT chk_parking_slots_status
        CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE'))
);

CREATE INDEX IF NOT EXISTS idx_parking_slots_location_id ON public.parking_slots (location_id);
CREATE INDEX IF NOT EXISTS idx_parking_slots_status ON public.parking_slots (status);
CREATE INDEX IF NOT EXISTS idx_parking_slots_vehicle_type ON public.parking_slots (vehicle_type);

DROP TRIGGER IF EXISTS trg_parking_slots_set_updated_at ON public.parking_slots;
CREATE TRIGGER trg_parking_slots_set_updated_at
BEFORE UPDATE ON public.parking_slots
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- bookings
-- =========================================================
CREATE TABLE IF NOT EXISTS public.bookings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at timestamp(6) with time zone DEFAULT now(),
    updated_at timestamp(6) with time zone DEFAULT now(),
    deleted_at timestamp(6) with time zone,

    driver_id uuid NOT NULL,
    slot_id uuid NOT NULL,
    location_id uuid NOT NULL,

    vehicle_type varchar(30),
    cancelled_at timestamp(6) with time zone,

    start_time timestamp(6) without time zone NOT NULL,
    end_time timestamp(6) without time zone NOT NULL,

    status varchar(20) NOT NULL DEFAULT 'PENDING',
    total_amount numeric(10, 2) NOT NULL DEFAULT 0.00,

    CONSTRAINT fk_bookings_driver
        FOREIGN KEY (driver_id)
        REFERENCES public.users (id),

    CONSTRAINT fk_bookings_slot
        FOREIGN KEY (slot_id)
        REFERENCES public.parking_slots (id),

    CONSTRAINT fk_bookings_location
        FOREIGN KEY (location_id)
        REFERENCES public.parking_locations (id),

    CONSTRAINT chk_bookings_vehicle_type
        CHECK (vehicle_type IS NULL OR vehicle_type IN ('TWO_WHEELER', 'FOUR_WHEELER')),

    CONSTRAINT chk_bookings_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),

    CONSTRAINT chk_bookings_total_amount_non_negative
        CHECK (total_amount >= 0.00),

    CONSTRAINT chk_bookings_end_after_start
        CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_bookings_driver_id ON public.bookings (driver_id);
CREATE INDEX IF NOT EXISTS idx_bookings_slot_id ON public.bookings (slot_id);
CREATE INDEX IF NOT EXISTS idx_bookings_location_id ON public.bookings (location_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON public.bookings (status);
CREATE INDEX IF NOT EXISTS idx_bookings_start_time ON public.bookings (start_time);
CREATE INDEX IF NOT EXISTS idx_bookings_end_time ON public.bookings (end_time);

DROP TRIGGER IF EXISTS trg_bookings_set_updated_at ON public.bookings;
CREATE TRIGGER trg_bookings_set_updated_at
BEFORE UPDATE ON public.bookings
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- payments
-- =========================================================
CREATE TABLE IF NOT EXISTS public.payments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at timestamp(6) with time zone DEFAULT now(),
    updated_at timestamp(6) with time zone DEFAULT now(),
    deleted_at timestamp(6) with time zone,

    booking_id uuid NOT NULL,
    amount numeric(10, 2) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    transaction_id varchar(150),
    payment_method varchar(20) NOT NULL,
    pidx varchar(150),
    paid_at timestamp(6) with time zone,

    refund_status varchar(30) NOT NULL DEFAULT 'NONE',
    refund_amount numeric(10, 2) DEFAULT 0.00,
    refund_requested_at timestamp(6) with time zone,
    refunded_at timestamp(6) with time zone,

    payment_url varchar(1000),

    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id)
        REFERENCES public.bookings (id),

    CONSTRAINT chk_payments_amount_positive
        CHECK (amount > 0.00),

    CONSTRAINT chk_payments_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),

    CONSTRAINT chk_payments_payment_method
        CHECK (payment_method IN ('CASH', 'KHALTI', 'ESEWA')),

    CONSTRAINT chk_payments_refund_status
        CHECK (refund_status IN ('NONE', 'PENDING', 'COMPLETED', 'FAILED')),

    CONSTRAINT chk_payments_refund_amount_non_negative
        CHECK (refund_amount IS NULL OR refund_amount >= 0.00)
);

CREATE INDEX IF NOT EXISTS idx_payments_booking_id ON public.payments (booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON public.payments (transaction_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON public.payments (status);
CREATE INDEX IF NOT EXISTS idx_payments_payment_method ON public.payments (payment_method);
CREATE INDEX IF NOT EXISTS idx_payments_pidx ON public.payments (pidx);
CREATE INDEX IF NOT EXISTS idx_payments_refund_status ON public.payments (refund_status);

DROP TRIGGER IF EXISTS trg_payments_set_updated_at ON public.payments;
CREATE TRIGGER trg_payments_set_updated_at
BEFORE UPDATE ON public.payments
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();

COMMIT;
