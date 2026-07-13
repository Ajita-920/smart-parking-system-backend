-- Adds vendor walk-in booking fields.

BEGIN;

ALTER TABLE public.bookings
    ALTER COLUMN driver_id DROP NOT NULL;

ALTER TABLE public.bookings
    ADD COLUMN IF NOT EXISTS customer_name varchar(100),
    ADD COLUMN IF NOT EXISTS customer_phone varchar(20),
    ADD COLUMN IF NOT EXISTS vehicle_number varchar(30),
    ADD COLUMN IF NOT EXISTS walk_in boolean NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_bookings_walk_in ON public.bookings (walk_in);

COMMIT;
