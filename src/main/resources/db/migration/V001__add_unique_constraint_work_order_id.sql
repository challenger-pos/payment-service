-- Migration: Add unique constraint on work_order_id to prevent duplicate payments
-- This constraint ensures idempotency - one work order can only have one payment

-- Note: With JPA ddl-auto: update, this is automatically applied when the entity is updated
-- This file serves as documentation of the schema change

ALTER TABLE payments 
ADD CONSTRAINT uk_payment_work_order_id UNIQUE (work_order_id);

-- To verify the constraint:
-- SELECT constraint_name, constraint_type 
-- FROM information_schema.table_constraints 
-- WHERE table_name = 'payments' AND constraint_type = 'UNIQUE';
