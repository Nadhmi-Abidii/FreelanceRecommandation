-- Allow the new mission lifecycle step where the freelancer has submitted the final delivery
-- and the client must close/pay the mission.
ALTER TABLE missions DROP CONSTRAINT IF EXISTS ck_missions_status;
ALTER TABLE missions DROP CONSTRAINT IF EXISTS missions_status_check;

UPDATE missions SET status = 'DRAFT' WHERE status IS NULL;

ALTER TABLE missions
    ALTER COLUMN status SET DEFAULT 'DRAFT',
    ADD CONSTRAINT ck_missions_status CHECK (status IN (
        'DRAFT',
        'PUBLISHED',
        'IN_PROGRESS',
        'PENDING_CLOSURE',
        'COMPLETED',
        'CANCELLED',
        'PAUSED'
    ));
