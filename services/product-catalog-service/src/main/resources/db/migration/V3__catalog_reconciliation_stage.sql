CREATE TABLE catalog_product_stage (
    batch_id UUID NOT NULL,
    product_id TEXT NOT NULL,
    product_type_id TEXT NOT NULL,
    internal_name TEXT,
    brand_name TEXT,
    product_name TEXT,
    description TEXT,
    source_version BIGINT NOT NULL,
    extracted_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (batch_id, product_id)
);

CREATE INDEX catalog_product_stage_extracted_idx
    ON catalog_product_stage (extracted_at, batch_id);
