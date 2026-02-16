-- Ensure milestone status constraint matches the Java enum and new workflow.
-- @Enumerated(EnumType.STRING) writes enum names; the old CHECK constraint did not allow
-- the new values (e.g. SUBMITTED/DRAFT), so data updates were failing against it.

-- 1) Drop existing constraints first so data normalization is not blocked
ALTER TABLE milestones DROP CONSTRAINT IF EXISTS ck_milestones_status;
ALTER TABLE milestones DROP CONSTRAINT IF EXISTS milestones_status_check;

-- 2) Normalize legacy values to the new workflow
UPDATE milestones SET status = 'SUBMITTED' WHERE status IN ('PENDING_VALIDATION', 'DELIVERED');
UPDATE milestones SET status = 'DRAFT' WHERE status IN ('PENDING', 'CREATED');
UPDATE milestones SET status = 'COMPLETED' WHERE status = 'VALIDATED';
UPDATE milestones SET status = 'DRAFT' WHERE status IS NULL; -- safety for nulls

-- 3) Recreate constraint aligned with the enum and set the default
ALTER TABLE milestones
    ALTER COLUMN status SET DEFAULT 'DRAFT',
    ADD CONSTRAINT ck_milestones_status CHECK (status IN (
        'DRAFT',
        'IN_PROGRESS',
        'SUBMITTED',
        'COMPLETED',
        'REJECTED',
        'PAID'
    ));
