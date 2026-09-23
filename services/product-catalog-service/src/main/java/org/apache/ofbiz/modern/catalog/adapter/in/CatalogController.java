package org.apache.ofbiz.modern.catalog.adapter.in;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.apache.ofbiz.modern.catalog.application.ProductCatalog;
import org.apache.ofbiz.modern.catalog.application.CatalogShadowComparison;
import org.apache.ofbiz.modern.catalog.config.PilotReadEnabledCondition;
import org.apache.ofbiz.modern.catalog.domain.ProductPage;
import org.apache.ofbiz.modern.catalog.domain.ProductSearch;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/catalog/v1/products")
@Conditional(PilotReadEnabledCondition.class)
public class CatalogController {
    private static final int MAXIMUM_PAGE_SIZE = 100;
    private static final int MAXIMUM_PRODUCT_ID_LENGTH = 20;
    private static final int MAXIMUM_INTERNAL_NAME_LENGTH = 255;
    private final ProductCatalog productCatalog;
    private final CatalogShadowComparison shadowComparison;

    public CatalogController(ProductCatalog productCatalog, CatalogShadowComparison shadowComparison) {
        this.productCatalog = productCatalog;
        this.shadowComparison = shadowComparison;
    }

    @GetMapping
    public Mono<ResponseEntity<ProductPage>> search(
            @RequestParam(defaultValue = "") String productId,
            @RequestParam(defaultValue = "") String internalName,
            @RequestParam(defaultValue = "productId") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ProductSearch search = parseSearch(productId, internalName, sort, direction, page, size);
        return Mono.fromCallable(() -> productCatalog.search(search))
                .subscribeOn(Schedulers.boundedElastic())
                .map(products -> {
                    shadowComparison.compare(search, products);
                    return ResponseEntity.ok()
                            .cacheControl(CacheControl.noStore())
                            .body(products);
                });
    }

    private ProductSearch parseSearch(
            String productId,
            String internalName,
            String sort,
            String direction,
            int page,
            int size) {
        if (productId.length() > MAXIMUM_PRODUCT_ID_LENGTH
                || internalName.length() > MAXIMUM_INTERNAL_NAME_LENGTH
                || page < 0
                || size < 1
                || size > MAXIMUM_PAGE_SIZE) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid catalog search parameters");
        }
        try {
            return new ProductSearch(
                    productId.strip(),
                    internalName.strip(),
                    ProductSearch.SortField.fromContract(sort),
                    ProductSearch.Direction.fromContract(direction),
                    page,
                    size);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid catalog search parameters", exception);
        }
    }
}
