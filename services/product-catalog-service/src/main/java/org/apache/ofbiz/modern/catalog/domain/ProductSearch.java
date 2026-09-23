package org.apache.ofbiz.modern.catalog.domain;

public record ProductSearch(
        String productId,
        String internalName,
        SortField sort,
        Direction direction,
        int page,
        int size) {

    public enum SortField {
        PRODUCT_ID("productId", "product_id"),
        PRODUCT_TYPE_ID("productTypeId", "product_type_id"),
        INTERNAL_NAME("internalName", "internal_name"),
        BRAND_NAME("brandName", "brand_name"),
        PRODUCT_NAME("productName", "product_name"),
        DESCRIPTION("description", "description");

        private final String contractName;
        private final String columnName;

        SortField(String contractName, String columnName) {
            this.contractName = contractName;
            this.columnName = columnName;
        }

        public String columnName() {
            return columnName;
        }

        public String contractName() {
            return contractName;
        }

        public static SortField fromContract(String value) {
            for (SortField field : values()) {
                if (field.contractName.equals(value)) {
                    return field;
                }
            }
            throw new IllegalArgumentException("Unsupported sort field");
        }
    }

    public enum Direction {
        ASC,
        DESC;

        public static Direction fromContract(String value) {
            try {
                return valueOf(value.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unsupported sort direction", exception);
            }
        }
    }
}
