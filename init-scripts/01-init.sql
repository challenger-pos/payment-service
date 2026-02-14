-- Initialization script for PostgreSQL 17
-- This script runs automatically when the database is created

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schema if needed (optional)
-- CREATE SCHEMA IF NOT EXISTS billing;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE billing_db TO postgres;

-- Create payments table (schema will be managed by JPA, but this is a reference)
-- You can comment this out if you prefer JPA to manage the schema
/*
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    client_id UUID NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    external_payment_id VARCHAR(255),
    payment_method VARCHAR(50),
    qr_code TEXT,
    qr_code_base64 TEXT,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    error_message VARCHAR(1000),
    CONSTRAINT chk_amount_positive CHECK (amount > 0)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_client_id ON payments(client_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);
CREATE INDEX IF NOT EXISTS idx_payments_external_id ON payments(external_payment_id);
*/
