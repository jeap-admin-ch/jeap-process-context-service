ALTER TABLE milestone
    ADD COLUMN state varchar DEFAULT 'NOT_REACHED';

UPDATE milestone SET state = 'REACHED' where reached = true;

ALTER TABLE milestone
    ALTER COLUMN state SET NOT NULL;

ALTER TABLE milestone
    DROP COLUMN reached;

CREATE INDEX milestone_state ON milestone (state);
