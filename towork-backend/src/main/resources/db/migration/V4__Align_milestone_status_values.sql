-- Normalize milestone status values and enforce the allowed lifecycle
UPDATE milestones SET status = 'PENDING' WHERE status = 'CREATED';
UPDATE milestones SET status = 'REJECTED' WHERE status = 'REFUSED';

ALTER TABLE milestones DROP CONSTRAINT IF EXISTS ck_milestones_status;
ALTER TABLE milestones
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ADD CONSTRAINT ck_milestones_status CHECK (status IN (
        'PENDING',
        'PENDING_VALIDATION',
        'VALIDATED',
        'REJECTED',
        'PAID',
        'DELIVERED'
    ));
