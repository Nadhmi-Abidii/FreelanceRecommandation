-- Ensure milestone status constraint matches application enum and
-- clean up legacy constraint names.

UPDATE milestones SET status = 'SUBMITTED' WHERE status = 'PENDING_VALIDATION';
UPDATE milestones SET status = 'SUBMITTED' WHERE status = 'DELIVERED';

ALTER TABLE milestones DROP CONSTRAINT IF EXISTS milestones_status_check;
ALTER TABLE milestones DROP CONSTRAINT IF EXISTS ck_milestones_status;
ALTER TABLE milestones
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ADD CONSTRAINT milestones_status_check CHECK (status IN (
        'PENDING',
        'IN_PROGRESS',
        'SUBMITTED',
        'VALIDATED',
        'PAID',
        'REJECTED'
    ));
