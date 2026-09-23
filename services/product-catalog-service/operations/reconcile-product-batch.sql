-- Run with psql -v batch_id='<approved snapshot batch UUID>'.
-- Zero rows from the second query is the promotion gate.
SELECT
    (SELECT COUNT(*) FROM catalog_product_stage WHERE batch_id = :'batch_id'::uuid) AS source_count,
    (SELECT COUNT(*) FROM catalog_product) AS projection_count;

SELECT
    COALESCE(source.product_id, projection.product_id) AS product_id,
    CASE
        WHEN source.product_id IS NULL THEN 'projection_only'
        WHEN projection.product_id IS NULL THEN 'source_only'
        ELSE 'field_mismatch'
    END AS mismatch
FROM (
    SELECT *
    FROM catalog_product_stage
    WHERE batch_id = :'batch_id'::uuid
) source
FULL OUTER JOIN catalog_product projection USING (product_id)
WHERE (
      source.product_id IS NULL
      OR projection.product_id IS NULL
      OR ROW(
          source.product_type_id,
          source.internal_name,
          source.brand_name,
          source.product_name,
          source.description
      ) IS DISTINCT FROM ROW(
          projection.product_type_id,
          projection.internal_name,
          projection.brand_name,
          projection.product_name,
          projection.description
      )
  )
ORDER BY product_id;
