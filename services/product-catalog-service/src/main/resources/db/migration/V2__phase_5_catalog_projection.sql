ALTER TABLE catalog_product
    ADD COLUMN product_type_id TEXT NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN internal_name TEXT,
    ADD COLUMN brand_name TEXT;

UPDATE catalog_product
SET product_type_id = 'FINISHED_GOOD',
    internal_name = product_name
WHERE product_type_id = 'UNKNOWN';

ALTER TABLE catalog_product
    ALTER COLUMN product_type_id DROP DEFAULT,
    ALTER COLUMN product_name DROP NOT NULL,
    ALTER COLUMN description DROP NOT NULL;

CREATE INDEX catalog_product_internal_name_idx
    ON catalog_product (LOWER(internal_name), product_id);

CREATE INDEX catalog_product_type_idx
    ON catalog_product (product_type_id, product_id);
