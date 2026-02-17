-- Flyway Migration V1: Initialize Payments Table
-- Description: Creates the payments table with all necessary columns for payment processing
-- Date: 2024-02-16

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create payments table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    client_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    external_payment_id VARCHAR(255),
    payment_method VARCHAR(50) DEFAULT 'pix',
    qr_code TEXT,
    qr_code_base64 TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    error_message TEXT,
    
    -- Constraints
    CONSTRAINT fk_payment_status 
        CHECK (status IN ('PENDING', 'PROCESSING', 'APPROVED', 'REJECTED', 'FAILED'))
);

-- Create indexes for performance optimization
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_client_id ON payments(client_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);
CREATE INDEX idx_payments_external_id ON payments(external_payment_id);

-- Create index for finding pending payments
CREATE INDEX idx_payments_status_pending ON payments(status) WHERE status = 'PENDING';

-- Add comment to table
COMMENT ON TABLE payments IS 'Stores payment transaction records processed through Mercado Pago PIX';

-- Add comments to columns
COMMENT ON COLUMN payments.id IS 'Unique payment identifier (UUID)';
COMMENT ON COLUMN payments.order_id IS 'Foreign key reference to order in order microservice (UUID)';
COMMENT ON COLUMN payments.client_id IS 'Client identifier (UUID)';
COMMENT ON COLUMN payments.amount IS 'Payment amount in currency (precision: 10.2 digits)';
COMMENT ON COLUMN payments.status IS 'Payment status: PENDING, PROCESSING, APPROVED, REJECTED, FAILED';
COMMENT ON COLUMN payments.external_payment_id IS 'Mercado Pago payment/order ID';
COMMENT ON COLUMN payments.payment_method IS 'Payment method type (currently: pix)';
COMMENT ON COLUMN payments.qr_code IS 'PIX QR code string representation';
COMMENT ON COLUMN payments.qr_code_base64 IS 'PIX QR code as base64 encoded PNG image';
COMMENT ON COLUMN payments.created_at IS 'Payment creation timestamp';
COMMENT ON COLUMN payments.processed_at IS 'Payment processing completion timestamp';
COMMENT ON COLUMN payments.error_message IS 'Error details if payment processing failed';
