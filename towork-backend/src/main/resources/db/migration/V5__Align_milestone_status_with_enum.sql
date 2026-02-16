-- Align milestone status constraint with the enum used in the application
-- and normalize any legacy values.

UPDATE milestones SET status = 'SUBMITTED' WHERE status = 'PENDING_VALIDATION';
UPDATE milestones SET status = 'SUBMITTED' WHERE status = 'DELIVERED';

ALTER TABLE milestones DROP CONSTRAINT IF EXISTS ck_milestones_status;
ALTER TABLE milestones
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ADD CONSTRAINT ck_milestones_status CHECK (status IN (
        'PENDING',
        'IN_PROGRESS',
        'SUBMITTED',
        'VALIDATED',
        'PAID',
        'REJECTED'
    ));
