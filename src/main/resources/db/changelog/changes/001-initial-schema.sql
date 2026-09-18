--liquibase formatted sql
-- Baseline schema captured from Hibernate ddl-auto=create DDL (PostgreSQL 17), 2026-09.
-- One changeset per table; constraints and indexes folded into their owning table.
-- Dev phase (pre-UAT): editing changelogs is OK — reset the DB after edits.
-- Once a persistent environment exists: never edit applied changesets; add a new file.

--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' AND table_name NOT IN ('databasechangelog', 'databasechangeloglock')

--changeset ledger-engine:001-create-account
CREATE TABLE public.account (
    allow_negative boolean NOT NULL,
    available_balance numeric(38,18) NOT NULL,
    is_active boolean NOT NULL,
    ledger_balance numeric(38,18) NOT NULL,
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    update_dt timestamp(6) with time zone,
    wallet_id bigint,
    buffer character varying(255),
    created_by character varying(255),
    currency character varying(255) NOT NULL,
    entity character varying(255),
    full_number character varying(255),
    main_account character varying(255),
    status character varying(255) NOT NULL,
    sub_type character varying(255),
    type character varying(255),
    updated_by character varying(255),
    CONSTRAINT account_currency_check CHECK (((currency)::text = ANY ((ARRAY['JPY'::character varying, 'HKD'::character varying, 'CNH'::character varying, 'CNY'::character varying, 'USD'::character varying, 'GBP'::character varying, 'AUD'::character varying, 'EUR'::character varying, 'CAD'::character varying, 'CHF'::character varying, 'SGD'::character varying, 'SEK'::character varying, 'KRW'::character varying, 'NOK'::character varying, 'NZD'::character varying, 'INR'::character varying, 'MXN'::character varying, 'TWD'::character varying, 'ZAR'::character varying, 'BRL'::character varying, 'DKK'::character varying, 'PLN'::character varying, 'THB'::character varying, 'ILS'::character varying, 'IDR'::character varying, 'CZK'::character varying, 'AED'::character varying, 'TRY'::character varying, 'HUF'::character varying, 'CLP'::character varying, 'SAR'::character varying, 'PHP'::character varying, 'MYR'::character varying, 'COP'::character varying, 'RUB'::character varying, 'RON'::character varying, 'PEN'::character varying, 'BHD'::character varying, 'BGN'::character varying, 'ARS'::character varying, 'VND'::character varying, 'RDN'::character varying, 'NDK'::character varying, 'BTC'::character varying, 'ETH'::character varying, 'SOL'::character varying, 'ADA'::character varying, 'XRP'::character varying, 'DOGE'::character varying, 'DOT'::character varying, 'AVAX'::character varying, 'MATIC'::character varying, 'POL'::character varying, 'TRX'::character varying, 'TON'::character varying, 'NEAR'::character varying, 'ATOM'::character varying, 'LTC'::character varying, 'BCH'::character varying, 'XLM'::character varying, 'ALGO'::character varying, 'APT'::character varying, 'SUI'::character varying, 'BNB'::character varying, 'WBTC'::character varying, 'WETH'::character varying, 'USDT'::character varying, 'USDC'::character varying, 'DAI'::character varying, 'FDUSD'::character varying, 'TUSD'::character varying, 'BUSD'::character varying, 'LINK'::character varying, 'UNI'::character varying, 'AAVE'::character varying, 'SHIB'::character varying, 'PEPE'::character varying, 'LP'::character varying, 'ALL'::character varying])::text[]))),
    CONSTRAINT account_status_check CHECK (((status)::text = ANY ((ARRAY['NEW'::character varying, 'VERIFIED'::character varying, 'ACTIVE'::character varying, 'DORMANT'::character varying, 'CLOSED'::character varying, 'SUSPENDED'::character varying])::text[])))
);
ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.account
    ADD CONSTRAINT uk_account_full_number UNIQUE (full_number);
ALTER TABLE ONLY public.account
    ADD CONSTRAINT uniqueaccountkey UNIQUE (entity, type, sub_type, main_account, buffer, currency);
CREATE INDEX account_idx_wallet_id ON public.account USING btree (wallet_id);
--rollback DROP TABLE IF EXISTS public.account;

--changeset ledger-engine:001-create-accounting_rule
CREATE TABLE public.accounting_rule (
    is_active boolean NOT NULL,
    multiplier numeric(38,18),
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    update_dt timestamp(6) with time zone,
    content text,
    created_by character varying(255),
    description character varying(255),
    direction character varying(255),
    name character varying(255),
    target_account character varying(255),
    updated_by character varying(255),
    CONSTRAINT accounting_rule_direction_check CHECK (((direction)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT'::character varying])::text[])))
);
ALTER TABLE ONLY public.accounting_rule
    ADD CONSTRAINT accounting_rule_pkey PRIMARY KEY (id);
--rollback DROP TABLE IF EXISTS public.accounting_rule;

--changeset ledger-engine:001-create-accounting_rule_execution
CREATE TABLE public.accounting_rule_execution (
    is_active boolean NOT NULL,
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    update_dt timestamp(6) with time zone,
    created_by character varying(255),
    description character varying(255),
    event_type character varying(255),
    metadata text,
    name character varying(255),
    order_type character varying(255),
    updated_by character varying(255),
    CONSTRAINT accounting_rule_execution_order_type_check CHECK (((order_type)::text = ANY ((ARRAY['PAYMENT_LINK'::character varying, 'WITHDRAWAL'::character varying, 'WALLET_TRANSFER'::character varying, 'DEPOSIT'::character varying, 'ADJUSTMENT'::character varying, 'ADJUSTMENT_REFUND'::character varying, 'ADJUSTMENT_TOTAL'::character varying, 'BANK_CHARGE'::character varying, 'HANDLING_CHARGE'::character varying, 'IN_WALLET_TRANSFER'::character varying, 'SWIFT_TRANSFER'::character varying, 'EARN'::character varying, 'BURN'::character varying, 'PROCESS'::character varying, 'CHARGE'::character varying, 'HOLD'::character varying, 'RELEASE'::character varying])::text[])))
);
ALTER TABLE ONLY public.accounting_rule_execution
    ADD CONSTRAINT accounting_rule_execution_name_key UNIQUE (name);
ALTER TABLE ONLY public.accounting_rule_execution
    ADD CONSTRAINT accounting_rule_execution_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.accounting_rule_execution
    ADD CONSTRAINT uk_accounting_rule_execution_event_type UNIQUE (event_type);
--rollback DROP TABLE IF EXISTS public.accounting_rule_execution;

--changeset ledger-engine:001-create-coa_dictionary
CREATE TABLE public.coa_dictionary (
    is_active boolean NOT NULL,
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    update_dt timestamp(6) with time zone,
    definition character varying(2000),
    code character varying(255) NOT NULL,
    created_by character varying(255),
    example character varying(255),
    kind character varying(255) NOT NULL,
    name character varying(255),
    side character varying(255),
    updated_by character varying(255),
    CONSTRAINT coa_dictionary_kind_check CHECK (((kind)::text = ANY ((ARRAY['ENTITY'::character varying, 'TYPE'::character varying, 'SUB_TYPE'::character varying, 'STEM'::character varying, 'PATH'::character varying, 'BUFFER'::character varying])::text[])))
);
ALTER TABLE ONLY public.coa_dictionary
    ADD CONSTRAINT coa_dictionary_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.coa_dictionary
    ADD CONSTRAINT uk_coa_dictionary_kind_code UNIQUE (kind, code);
--rollback DROP TABLE IF EXISTS public.coa_dictionary;

--changeset ledger-engine:001-create-coa_profile
CREATE TABLE public.coa_profile (
    is_active boolean NOT NULL,
    is_default boolean NOT NULL,
    is_enabled boolean NOT NULL,
    pool_allow_negative boolean NOT NULL,
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    update_dt timestamp(6) with time zone,
    wallet_id bigint,
    buffer character varying(255) NOT NULL,
    code character varying(255) NOT NULL,
    created_by character varying(255),
    currency character varying(255) NOT NULL,
    entity character varying(255) NOT NULL,
    name character varying(255),
    sub_type character varying(255) NOT NULL,
    transaction_code character varying(255),
    type character varying(255) NOT NULL,
    updated_by character varying(255)
);
ALTER TABLE ONLY public.coa_profile
    ADD CONSTRAINT coa_profile_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.coa_profile
    ADD CONSTRAINT uk_coa_profile_code UNIQUE (code);
ALTER TABLE ONLY public.coa_profile
    ADD CONSTRAINT uk_coa_profile_transaction_code UNIQUE (transaction_code);
--rollback DROP TABLE IF EXISTS public.coa_profile;

--changeset ledger-engine:001-create-digestion_rule
CREATE TABLE public.digestion_rule (
    is_active boolean NOT NULL,
    is_enabled boolean NOT NULL,
    max_age_days integer,
    min_amount numeric(36,18),
    priority integer NOT NULL,
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    update_dt timestamp(6) with time zone,
    code character varying(255) NOT NULL,
    created_by character varying(255),
    eligible_currencies character varying(255),
    eligible_mccs character varying(255),
    event_type character varying(255) NOT NULL,
    name character varying(255),
    operation character varying(255) NOT NULL,
    process_type character varying(255),
    result_currency character varying(255) NOT NULL,
    updated_by character varying(255),
    formula jsonb NOT NULL,
    when_factors jsonb
);
ALTER TABLE ONLY public.digestion_rule
    ADD CONSTRAINT digestion_rule_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.digestion_rule
    ADD CONSTRAINT uk_digestion_rule_code UNIQUE (code);
--rollback DROP TABLE IF EXISTS public.digestion_rule;

--changeset ledger-engine:001-create-failed_transaction_ingest
CREATE TABLE public.failed_transaction_ingest (
    amount numeric(36,18),
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    occurred_at timestamp(6) with time zone,
    update_dt timestamp(6) with time zone,
    created_by character varying(255),
    currency character varying(255),
    event_id character varying(255) NOT NULL,
    event_type character varying(255),
    failure_code character varying(255) NOT NULL,
    owner_id character varying(255),
    reason character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    updated_by character varying(255),
    raw_payload jsonb
);
ALTER TABLE ONLY public.failed_transaction_ingest
    ADD CONSTRAINT failed_transaction_ingest_pkey PRIMARY KEY (id);
--rollback DROP TABLE IF EXISTS public.failed_transaction_ingest;

--changeset ledger-engine:001-create-fx_rate
CREATE TABLE public.fx_rate (
    is_active boolean NOT NULL,
    rate numeric(32,10) NOT NULL,
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    update_dt timestamp(6) with time zone,
    base character varying(255) NOT NULL,
    created_by character varying(255),
    target character varying(255) NOT NULL,
    updated_by character varying(255),
    CONSTRAINT fx_rate_base_check CHECK (((base)::text = ANY ((ARRAY['JPY'::character varying, 'HKD'::character varying, 'CNH'::character varying, 'CNY'::character varying, 'USD'::character varying, 'GBP'::character varying, 'AUD'::character varying, 'EUR'::character varying, 'CAD'::character varying, 'CHF'::character varying, 'SGD'::character varying, 'SEK'::character varying, 'KRW'::character varying, 'NOK'::character varying, 'NZD'::character varying, 'INR'::character varying, 'MXN'::character varying, 'TWD'::character varying, 'ZAR'::character varying, 'BRL'::character varying, 'DKK'::character varying, 'PLN'::character varying, 'THB'::character varying, 'ILS'::character varying, 'IDR'::character varying, 'CZK'::character varying, 'AED'::character varying, 'TRY'::character varying, 'HUF'::character varying, 'CLP'::character varying, 'SAR'::character varying, 'PHP'::character varying, 'MYR'::character varying, 'COP'::character varying, 'RUB'::character varying, 'RON'::character varying, 'PEN'::character varying, 'BHD'::character varying, 'BGN'::character varying, 'ARS'::character varying, 'VND'::character varying, 'RDN'::character varying, 'NDK'::character varying, 'BTC'::character varying, 'ETH'::character varying, 'SOL'::character varying, 'ADA'::character varying, 'XRP'::character varying, 'DOGE'::character varying, 'DOT'::character varying, 'AVAX'::character varying, 'MATIC'::character varying, 'POL'::character varying, 'TRX'::character varying, 'TON'::character varying, 'NEAR'::character varying, 'ATOM'::character varying, 'LTC'::character varying, 'BCH'::character varying, 'XLM'::character varying, 'ALGO'::character varying, 'APT'::character varying, 'SUI'::character varying, 'BNB'::character varying, 'WBTC'::character varying, 'WETH'::character varying, 'USDT'::character varying, 'USDC'::character varying, 'DAI'::character varying, 'FDUSD'::character varying, 'TUSD'::character varying, 'BUSD'::character varying, 'LINK'::character varying, 'UNI'::character varying, 'AAVE'::character varying, 'SHIB'::character varying, 'PEPE'::character varying, 'LP'::character varying, 'ALL'::character varying])::text[]))),
    CONSTRAINT fx_rate_target_check CHECK (((target)::text = ANY ((ARRAY['JPY'::character varying, 'HKD'::character varying, 'CNH'::character varying, 'CNY'::character varying, 'USD'::character varying, 'GBP'::character varying, 'AUD'::character varying, 'EUR'::character varying, 'CAD'::character varying, 'CHF'::character varying, 'SGD'::character varying, 'SEK'::character varying, 'KRW'::character varying, 'NOK'::character varying, 'NZD'::character varying, 'INR'::character varying, 'MXN'::character varying, 'TWD'::character varying, 'ZAR'::character varying, 'BRL'::character varying, 'DKK'::character varying, 'PLN'::character varying, 'THB'::character varying, 'ILS'::character varying, 'IDR'::character varying, 'CZK'::character varying, 'AED'::character varying, 'TRY'::character varying, 'HUF'::character varying, 'CLP'::character varying, 'SAR'::character varying, 'PHP'::character varying, 'MYR'::character varying, 'COP'::character varying, 'RUB'::character varying, 'RON'::character varying, 'PEN'::character varying, 'BHD'::character varying, 'BGN'::character varying, 'ARS'::character varying, 'VND'::character varying, 'RDN'::character varying, 'NDK'::character varying, 'BTC'::character varying, 'ETH'::character varying, 'SOL'::character varying, 'ADA'::character varying, 'XRP'::character varying, 'DOGE'::character varying, 'DOT'::character varying, 'AVAX'::character varying, 'MATIC'::character varying, 'POL'::character varying, 'TRX'::character varying, 'TON'::character varying, 'NEAR'::character varying, 'ATOM'::character varying, 'LTC'::character varying, 'BCH'::character varying, 'XLM'::character varying, 'ALGO'::character varying, 'APT'::character varying, 'SUI'::character varying, 'BNB'::character varying, 'WBTC'::character varying, 'WETH'::character varying, 'USDT'::character varying, 'USDC'::character varying, 'DAI'::character varying, 'FDUSD'::character varying, 'TUSD'::character varying, 'BUSD'::character varying, 'LINK'::character varying, 'UNI'::character varying, 'AAVE'::character varying, 'SHIB'::character varying, 'PEPE'::character varying, 'LP'::character varying, 'ALL'::character varying])::text[])))
);
ALTER TABLE ONLY public.fx_rate
    ADD CONSTRAINT fx_rate_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.fx_rate
    ADD CONSTRAINT uniquefxratekey UNIQUE (base, target);
--rollback DROP TABLE IF EXISTS public.fx_rate;

--changeset ledger-engine:001-create-ingest_policy
CREATE TABLE public.ingest_policy (
    is_active boolean NOT NULL,
    is_auto_create_wallet boolean NOT NULL,
    is_enabled boolean NOT NULL,
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    update_dt timestamp(6) with time zone,
    auto_wallet_associated_from character varying(255),
    auto_wallet_coa_profile_code character varying(255),
    auto_wallet_ensure_currency character varying(255) NOT NULL,
    auto_wallet_name_prefix character varying(255),
    auto_wallet_settlement_currency character varying(255) NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    entry_factors jsonb
);
ALTER TABLE ONLY public.ingest_policy
    ADD CONSTRAINT ingest_policy_pkey PRIMARY KEY (id);
--rollback DROP TABLE IF EXISTS public.ingest_policy;

--changeset ledger-engine:001-create-ledger_entry
CREATE TABLE public.ledger_entry (
    affects_available boolean,
    affects_ledger boolean,
    amount numeric(38,18) NOT NULL,
    is_active boolean NOT NULL,
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    txn_id bigint,
    update_dt timestamp(6) with time zone,
    created_by character varying(255),
    currency character varying(255) NOT NULL,
    direction character varying(255) NOT NULL,
    target_id character varying(255),
    updated_by character varying(255),
    CONSTRAINT ledger_entry_currency_check CHECK (((currency)::text = ANY ((ARRAY['JPY'::character varying, 'HKD'::character varying, 'CNH'::character varying, 'CNY'::character varying, 'USD'::character varying, 'GBP'::character varying, 'AUD'::character varying, 'EUR'::character varying, 'CAD'::character varying, 'CHF'::character varying, 'SGD'::character varying, 'SEK'::character varying, 'KRW'::character varying, 'NOK'::character varying, 'NZD'::character varying, 'INR'::character varying, 'MXN'::character varying, 'TWD'::character varying, 'ZAR'::character varying, 'BRL'::character varying, 'DKK'::character varying, 'PLN'::character varying, 'THB'::character varying, 'ILS'::character varying, 'IDR'::character varying, 'CZK'::character varying, 'AED'::character varying, 'TRY'::character varying, 'HUF'::character varying, 'CLP'::character varying, 'SAR'::character varying, 'PHP'::character varying, 'MYR'::character varying, 'COP'::character varying, 'RUB'::character varying, 'RON'::character varying, 'PEN'::character varying, 'BHD'::character varying, 'BGN'::character varying, 'ARS'::character varying, 'VND'::character varying, 'RDN'::character varying, 'NDK'::character varying, 'BTC'::character varying, 'ETH'::character varying, 'SOL'::character varying, 'ADA'::character varying, 'XRP'::character varying, 'DOGE'::character varying, 'DOT'::character varying, 'AVAX'::character varying, 'MATIC'::character varying, 'POL'::character varying, 'TRX'::character varying, 'TON'::character varying, 'NEAR'::character varying, 'ATOM'::character varying, 'LTC'::character varying, 'BCH'::character varying, 'XLM'::character varying, 'ALGO'::character varying, 'APT'::character varying, 'SUI'::character varying, 'BNB'::character varying, 'WBTC'::character varying, 'WETH'::character varying, 'USDT'::character varying, 'USDC'::character varying, 'DAI'::character varying, 'FDUSD'::character varying, 'TUSD'::character varying, 'BUSD'::character varying, 'LINK'::character varying, 'UNI'::character varying, 'AAVE'::character varying, 'SHIB'::character varying, 'PEPE'::character varying, 'LP'::character varying, 'ALL'::character varying])::text[]))),
    CONSTRAINT ledger_entry_direction_check CHECK (((direction)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT'::character varying])::text[])))
);
ALTER TABLE ONLY public.ledger_entry
    ADD CONSTRAINT ledger_entry_pkey PRIMARY KEY (id);
--rollback DROP TABLE IF EXISTS public.ledger_entry;

--changeset ledger-engine:001-create-ledger_movement
CREATE TABLE public.ledger_movement (
    amount numeric(38,18) NOT NULL,
    is_active boolean NOT NULL,
    version integer,
    associated_ledger_movement_id bigint,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    txn_id bigint,
    update_dt timestamp(6) with time zone,
    wallet_id bigint NOT NULL,
    main_account character varying(32),
    alias character varying(255),
    apply_to character varying(255) NOT NULL,
    compliance_context text,
    created_by character varying(255),
    currency character varying(255) NOT NULL,
    event text,
    files text,
    metadata text,
    mode character varying(255) NOT NULL,
    movement_key character varying(255) NOT NULL,
    order_type character varying(255) NOT NULL,
    originator_id character varying(255),
    payer_context text,
    recipient_context text,
    remarks text,
    status character varying(255) NOT NULL,
    target_id character varying(255),
    type character varying(255) NOT NULL,
    updated_by character varying(255),
    CONSTRAINT ledger_movement_apply_to_check CHECK (((apply_to)::text = ANY ((ARRAY['BOTH'::character varying, 'LEDGER'::character varying, 'AVAILABLE'::character varying])::text[]))),
    CONSTRAINT ledger_movement_currency_check CHECK (((currency)::text = ANY ((ARRAY['JPY'::character varying, 'HKD'::character varying, 'CNH'::character varying, 'CNY'::character varying, 'USD'::character varying, 'GBP'::character varying, 'AUD'::character varying, 'EUR'::character varying, 'CAD'::character varying, 'CHF'::character varying, 'SGD'::character varying, 'SEK'::character varying, 'KRW'::character varying, 'NOK'::character varying, 'NZD'::character varying, 'INR'::character varying, 'MXN'::character varying, 'TWD'::character varying, 'ZAR'::character varying, 'BRL'::character varying, 'DKK'::character varying, 'PLN'::character varying, 'THB'::character varying, 'ILS'::character varying, 'IDR'::character varying, 'CZK'::character varying, 'AED'::character varying, 'TRY'::character varying, 'HUF'::character varying, 'CLP'::character varying, 'SAR'::character varying, 'PHP'::character varying, 'MYR'::character varying, 'COP'::character varying, 'RUB'::character varying, 'RON'::character varying, 'PEN'::character varying, 'BHD'::character varying, 'BGN'::character varying, 'ARS'::character varying, 'VND'::character varying, 'RDN'::character varying, 'NDK'::character varying, 'BTC'::character varying, 'ETH'::character varying, 'SOL'::character varying, 'ADA'::character varying, 'XRP'::character varying, 'DOGE'::character varying, 'DOT'::character varying, 'AVAX'::character varying, 'MATIC'::character varying, 'POL'::character varying, 'TRX'::character varying, 'TON'::character varying, 'NEAR'::character varying, 'ATOM'::character varying, 'LTC'::character varying, 'BCH'::character varying, 'XLM'::character varying, 'ALGO'::character varying, 'APT'::character varying, 'SUI'::character varying, 'BNB'::character varying, 'WBTC'::character varying, 'WETH'::character varying, 'USDT'::character varying, 'USDC'::character varying, 'DAI'::character varying, 'FDUSD'::character varying, 'TUSD'::character varying, 'BUSD'::character varying, 'LINK'::character varying, 'UNI'::character varying, 'AAVE'::character varying, 'SHIB'::character varying, 'PEPE'::character varying, 'LP'::character varying, 'ALL'::character varying])::text[]))),
    CONSTRAINT ledger_movement_mode_check CHECK (((mode)::text = ANY ((ARRAY['AUTO'::character varying, 'MANUAL'::character varying, 'ALL'::character varying])::text[]))),
    CONSTRAINT ledger_movement_order_type_check CHECK (((order_type)::text = ANY ((ARRAY['PAYMENT_LINK'::character varying, 'WITHDRAWAL'::character varying, 'WALLET_TRANSFER'::character varying, 'DEPOSIT'::character varying, 'ADJUSTMENT'::character varying, 'ADJUSTMENT_REFUND'::character varying, 'ADJUSTMENT_TOTAL'::character varying, 'BANK_CHARGE'::character varying, 'HANDLING_CHARGE'::character varying, 'IN_WALLET_TRANSFER'::character varying, 'SWIFT_TRANSFER'::character varying, 'EARN'::character varying, 'BURN'::character varying, 'PROCESS'::character varying, 'CHARGE'::character varying, 'HOLD'::character varying, 'RELEASE'::character varying])::text[]))),
    CONSTRAINT ledger_movement_status_check CHECK (((status)::text = ANY ((ARRAY['PROCESSING'::character varying, 'PENDING_DOCS'::character varying, 'REQUEST_FURTHER_INFORMATION'::character varying, 'SETTLED'::character varying, 'REJECTED'::character varying, 'VOIDED_BY_ASSESSMENT'::character varying, 'REFUNDED'::character varying, 'ERROR'::character varying, 'ALL'::character varying, 'PENDING'::character varying, 'REVERSED'::character varying])::text[]))),
    CONSTRAINT ledger_movement_type_check CHECK (((type)::text = ANY ((ARRAY['CHARGE'::character varying, 'TRANSFER'::character varying])::text[])))
);
ALTER TABLE ONLY public.ledger_movement
    ADD CONSTRAINT ledger_movement_movement_key_key UNIQUE (movement_key);
ALTER TABLE ONLY public.ledger_movement
    ADD CONSTRAINT ledger_movement_pkey PRIMARY KEY (id);
CREATE INDEX ledger_movement_idx_walletid ON public.ledger_movement USING btree (wallet_id);
--rollback DROP TABLE IF EXISTS public.ledger_movement;

--changeset ledger-engine:001-create-system_configuration
CREATE TABLE public.system_configuration (
    is_active boolean NOT NULL,
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    update_dt timestamp(6) with time zone,
    created_by character varying(255),
    name character varying(255),
    scope character varying(255),
    target character varying(255),
    updated_by character varying(255),
    value jsonb
);
ALTER TABLE ONLY public.system_configuration
    ADD CONSTRAINT system_configuration_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.system_configuration
    ADD CONSTRAINT uniquetargetandscope UNIQUE (target, scope);
--rollback DROP TABLE IF EXISTS public.system_configuration;

--changeset ledger-engine:001-create-wallet
CREATE TABLE public.wallet (
    is_active boolean NOT NULL,
    version integer,
    account_id bigint NOT NULL,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    update_dt timestamp(6) with time zone,
    created_by character varying(255),
    name character varying(255),
    owner_id character varying(255) NOT NULL,
    settlement_currency character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    tier character varying(255),
    type character varying(255) NOT NULL,
    updated_by character varying(255),
    vanity_code character varying(255),
    wallet_type character varying(255) NOT NULL,
    CONSTRAINT wallet_settlement_currency_check CHECK (((settlement_currency)::text = ANY ((ARRAY['JPY'::character varying, 'HKD'::character varying, 'CNH'::character varying, 'CNY'::character varying, 'USD'::character varying, 'GBP'::character varying, 'AUD'::character varying, 'EUR'::character varying, 'CAD'::character varying, 'CHF'::character varying, 'SGD'::character varying, 'SEK'::character varying, 'KRW'::character varying, 'NOK'::character varying, 'NZD'::character varying, 'INR'::character varying, 'MXN'::character varying, 'TWD'::character varying, 'ZAR'::character varying, 'BRL'::character varying, 'DKK'::character varying, 'PLN'::character varying, 'THB'::character varying, 'ILS'::character varying, 'IDR'::character varying, 'CZK'::character varying, 'AED'::character varying, 'TRY'::character varying, 'HUF'::character varying, 'CLP'::character varying, 'SAR'::character varying, 'PHP'::character varying, 'MYR'::character varying, 'COP'::character varying, 'RUB'::character varying, 'RON'::character varying, 'PEN'::character varying, 'BHD'::character varying, 'BGN'::character varying, 'ARS'::character varying, 'VND'::character varying, 'RDN'::character varying, 'NDK'::character varying, 'BTC'::character varying, 'ETH'::character varying, 'SOL'::character varying, 'ADA'::character varying, 'XRP'::character varying, 'DOGE'::character varying, 'DOT'::character varying, 'AVAX'::character varying, 'MATIC'::character varying, 'POL'::character varying, 'TRX'::character varying, 'TON'::character varying, 'NEAR'::character varying, 'ATOM'::character varying, 'LTC'::character varying, 'BCH'::character varying, 'XLM'::character varying, 'ALGO'::character varying, 'APT'::character varying, 'SUI'::character varying, 'BNB'::character varying, 'WBTC'::character varying, 'WETH'::character varying, 'USDT'::character varying, 'USDC'::character varying, 'DAI'::character varying, 'FDUSD'::character varying, 'TUSD'::character varying, 'BUSD'::character varying, 'LINK'::character varying, 'UNI'::character varying, 'AAVE'::character varying, 'SHIB'::character varying, 'PEPE'::character varying, 'LP'::character varying, 'ALL'::character varying])::text[]))),
    CONSTRAINT wallet_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'VERIFIED'::character varying, 'ACTIVE'::character varying, 'DORMANT'::character varying, 'CLOSED'::character varying, 'SUSPENDED'::character varying])::text[]))),
    CONSTRAINT wallet_type_check CHECK (((type)::text = ANY ((ARRAY['CUSTODIAN'::character varying, 'CRYPTO'::character varying])::text[]))),
    CONSTRAINT wallet_wallet_type_check CHECK (((wallet_type)::text = ANY ((ARRAY['INDIVIDUAL'::character varying, 'CORPORATE'::character varying])::text[])))
);
ALTER TABLE ONLY public.wallet
    ADD CONSTRAINT uk_wallet_owner UNIQUE (owner_id);
ALTER TABLE ONLY public.wallet
    ADD CONSTRAINT uk_wallet_vanity_code UNIQUE (vanity_code);
ALTER TABLE ONLY public.wallet
    ADD CONSTRAINT wallet_pkey PRIMARY KEY (id);
--rollback DROP TABLE IF EXISTS public.wallet;

--changeset ledger-engine:001-create-wallet_tier_policy
CREATE TABLE public.wallet_tier_policy (
    is_active boolean NOT NULL,
    is_enabled boolean NOT NULL,
    version integer,
    create_dt timestamp(6) with time zone,
    id bigint NOT NULL,
    update_dt timestamp(6) with time zone,
    created_by character varying(255),
    criterion character varying(255) NOT NULL,
    currency character varying(255) NOT NULL,
    updated_by character varying(255),
    bands jsonb,
    CONSTRAINT wallet_tier_policy_criterion_check CHECK (((criterion)::text = 'LEDGER_BALANCE'::text))
);
ALTER TABLE ONLY public.wallet_tier_policy
    ADD CONSTRAINT uk_wallet_tier_policy_criterion_currency UNIQUE (criterion, currency);
ALTER TABLE ONLY public.wallet_tier_policy
    ADD CONSTRAINT wallet_tier_policy_pkey PRIMARY KEY (id);
--rollback DROP TABLE IF EXISTS public.wallet_tier_policy;

