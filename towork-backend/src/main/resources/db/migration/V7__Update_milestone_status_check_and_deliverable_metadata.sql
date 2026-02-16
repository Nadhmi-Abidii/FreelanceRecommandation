-- Fix ck_milestones_status so SUBMITTED and the new workflow values are allowed.
-- The previous constraint blocked updates when a freelancer submitted a deliverable.

ALTER TABLE milestones DROP CONSTRAINT IF EXISTS ck_milestones_status;
ALTER TABLE milestones DROP CONSTRAINT IF EXISTS milestones_status_check;

-- Normalize legacy/older values to the current enum
UPDATE milestones SET status = 'SUBMITTED' WHERE status IN ('PENDING_VALIDATION', 'DELIVERED');
UPDATE milestones SET status = 'DRAFT' WHERE status IN ('PENDING', 'CREATED');
UPDATE milestones SET status = 'COMPLETED' WHERE status = 'VALIDATED';
UPDATE milestones SET status = 'DRAFT' WHERE status IS NULL;

-- Recreate the CHECK constraint aligned with the enum
ALTER TABLE milestones
    ALTER COLUMN status SET DEFAULT 'DRAFT',
    ADD CONSTRAINT ck_milestones_status CHECK (status IN (
        'DRAFT',
        'IN_PROGRESS',
        'SUBMITTED',
        'REJECTED',
        'COMPLETED',
        'PAID'
    ));

-- Keep lightweight metadata of the latest deliverable on the milestone
ALTER TABLE milestones
    ADD COLUMN IF NOT EXISTS deliverable_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS deliverable_original_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS deliverable_file_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS deliverable_file_size BIGINT,
    ADD COLUMN IF NOT EXISTS deliverable_uploaded_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deliverable_comment TEXT;
