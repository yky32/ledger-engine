-- Remove classic double-entry journal tables (balances live on account + ledger_movement)
ALTER TABLE ledger_movement DROP CONSTRAINT IF EXISTS fk_ledger_movement_journal;
ALTER TABLE ledger_movement DROP COLUMN IF EXISTS journal_transaction_id;
DROP TABLE IF EXISTS journal_entry CASCADE;
DROP TABLE IF EXISTS journal_transaction CASCADE;
