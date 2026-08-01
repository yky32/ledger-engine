CREATE TABLE ledger_account (
    id UUID PRIMARY KEY,
    external_reference VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    allow_negative BOOLEAN NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_account_external_reference UNIQUE (external_reference),
    CONSTRAINT ck_account_type CHECK (type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    CONSTRAINT ck_account_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_account_status CHECK (status IN ('ACTIVE','FROZEN','CLOSED'))
);

CREATE TABLE journal_transaction (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(150) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    reference VARCHAR(150),
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    effective_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reversal_of_id UUID,
    CONSTRAINT uk_transaction_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uk_transaction_reversal_of UNIQUE (reversal_of_id),
    CONSTRAINT fk_transaction_reversal_of FOREIGN KEY (reversal_of_id) REFERENCES journal_transaction(id),
    CONSTRAINT ck_transaction_status CHECK (status IN ('POSTED','REVERSED'))
);

CREATE TABLE journal_entry (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    side VARCHAR(10) NOT NULL,
    amount NUMERIC(38,18) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    sequence_number INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_entry_transaction FOREIGN KEY (transaction_id) REFERENCES journal_transaction(id),
    CONSTRAINT fk_entry_account FOREIGN KEY (account_id) REFERENCES ledger_account(id),
    CONSTRAINT uk_entry_transaction_sequence UNIQUE (transaction_id, sequence_number),
    CONSTRAINT ck_entry_side CHECK (side IN ('DEBIT','CREDIT')),
    CONSTRAINT ck_entry_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_entry_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_entry_sequence_positive CHECK (sequence_number > 0)
);

CREATE INDEX ix_entry_account_created ON journal_entry(account_id, created_at, id);
CREATE INDEX ix_entry_transaction ON journal_entry(transaction_id);
