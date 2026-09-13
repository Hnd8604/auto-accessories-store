CREATE UNIQUE INDEX ux_payments_reference_code
    ON payments (reference_code)
    WHERE reference_code IS NOT NULL;
