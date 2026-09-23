-- Run only after the batch has passed its source-side validation.
-- psql stops and rolls back on any error.
\set ON_ERROR_STOP on
BEGIN;

INSERT INTO catalog_product (
    product_id,
    product_type_id,
    internal_name,
    brand_name,
    product_name,
    description,
    source_version,
    updated_at
)
SELECT
    product_id,
    product_type_id,
    internal_name,
    brand_name,
    product_name,
    description,
    source_version,
    extracted_at
FROM catalog_product_stage
WHERE batch_id = :'batch_id'::uuid
ON CONFLICT (product_id) DO UPDATE SET
    product_type_id = EXCLUDED.product_type_id,
    internal_name = EXCLUDED.internal_name,
    brand_name = EXCLUDED.brand_name,
    product_name = EXCLUDED.product_name,
    description = EXCLUDED.description,
    source_version = EXCLUDED.source_version,
    updated_at = EXCLUDED.updated_at
WHERE catalog_product.source_version <= EXCLUDED.source_version;

DELETE FROM catalog_product projection
WHERE NOT EXISTS (
    SELECT 1
    FROM catalog_product_stage source
    WHERE source.batch_id = :'batch_id'::uuid
      AND source.product_id = projection.product_id
);

COMMIT;
