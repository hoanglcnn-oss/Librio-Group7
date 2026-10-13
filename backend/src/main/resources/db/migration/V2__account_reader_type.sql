-- =========================================================================
-- Librio V2: Batch 20C — Account & Institutional Eligibility
-- Replaces membership_eligible with reader_type for identity classification.
-- =========================================================================

-- 1. Add reader_type column
ALTER TABLE accounts
    ADD COLUMN reader_type VARCHAR(32);

-- 2. Backfill existing READER accounts to INSTITUTIONAL as baseline assumption
UPDATE accounts
    SET reader_type = 'INSTITUTIONAL'
    WHERE role = 'READER';

-- 3. Add CHECK constraints for reader_type values and role alignment
ALTER TABLE accounts
    ADD CONSTRAINT chk_account_reader_type
        CHECK (reader_type IS NULL OR reader_type IN ('INSTITUTIONAL', 'EXTERNAL'));

ALTER TABLE accounts
    ADD CONSTRAINT chk_account_reader_type_role
        CHECK ((role = 'READER' AND reader_type IS NOT NULL) OR (role = 'LIBRARIAN' AND reader_type IS NULL));

-- 4. Remove legacy membership_eligible column
ALTER TABLE accounts
    DROP COLUMN IF EXISTS membership_eligible;
