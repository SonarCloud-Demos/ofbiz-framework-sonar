package org.apache.ofbiz.modern.catalog.adapter.out;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JdbcProductCatalogTest {
    @Test
    void escapesLikeMetacharactersBeforeBinding() {
        assertEquals("name\\%\\_\\\\value", JdbcProductCatalog.escapeLike("name%_\\value"));
    }
}
