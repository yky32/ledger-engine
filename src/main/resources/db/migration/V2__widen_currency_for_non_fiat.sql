ALTER TABLE ledger_account DROP CONSTRAINT ck_account_currency;
ALTER TABLE journal_entry DROP CONSTRAINT ck_entry_currency;

ALTER TABLE ledger_account ALTER COLUMN currency TYPE VARCHAR(4);
ALTER TABLE journal_entry ALTER COLUMN currency TYPE VARCHAR(4);

ALTER TABLE ledger_account ADD CONSTRAINT ck_account_currency CHECK (currency ~ '^[A-Z]{2,4}$');
ALTER TABLE journal_entry ADD CONSTRAINT ck_entry_currency CHECK (currency ~ '^[A-Z]{2,4}$');
