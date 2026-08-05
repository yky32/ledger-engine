CREATE TABLE wallet (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    alias VARCHAR(100) NOT NULL,
    owner_id VARCHAR(100) NOT NULL,
    currency VARCHAR(4) NOT NULL,
    external_id VARCHAR(100),
    external_type VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_wallet_account FOREIGN KEY (account_id) REFERENCES ledger_account(id),
    CONSTRAINT uk_wallet_alias UNIQUE (alias),
    CONSTRAINT uk_wallet_owner_currency UNIQUE (owner_id, currency),
    CONSTRAINT ck_wallet_status CHECK (status IN ('PENDING','ACTIVE','FROZEN','CLOSED'))
);

CREATE TABLE ledger_movement (
    id UUID PRIMARY KEY,
    movement_key VARCHAR(150) NOT NULL,
    wallet_id UUID NOT NULL,
    order_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    mode VARCHAR(10) NOT NULL DEFAULT 'AUTO',
    originator_id VARCHAR(100),
    target_id VARCHAR(100),
    amount NUMERIC(38,18) NOT NULL,
    currency VARCHAR(4) NOT NULL,
    journal_transaction_id UUID,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_movement_wallet FOREIGN KEY (wallet_id) REFERENCES wallet(id),
    CONSTRAINT fk_movement_journal FOREIGN KEY (journal_transaction_id) REFERENCES journal_transaction(id),
    CONSTRAINT uk_movement_key UNIQUE (movement_key),
    CONSTRAINT ck_movement_status CHECK (status IN ('PENDING','PROCESSING','SETTLED','REJECTED','REVERSED')),
    CONSTRAINT ck_movement_mode CHECK (mode IN ('AUTO','MANUAL')),
    CONSTRAINT ck_movement_order_type CHECK (order_type IN (
        'DEPOSIT','WITHDRAWAL','IN_WALLET_TRANSFER','SWIFT_TRANSFER',
        'EARN','BURN','PROCESS','CHARGE'
    )),
    CONSTRAINT ck_movement_amount_positive CHECK (amount > 0)
);

CREATE INDEX ix_movement_wallet_created ON ledger_movement(wallet_id, created_at);
CREATE INDEX ix_wallet_owner ON wallet(owner_id);
