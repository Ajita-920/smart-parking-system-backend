UPDATE public.users
SET
    created_at = COALESCE(created_at, updated_at, now()),
    updated_at = COALESCE(updated_at, created_at, now())
WHERE created_at IS NULL
   OR updated_at IS NULL;
