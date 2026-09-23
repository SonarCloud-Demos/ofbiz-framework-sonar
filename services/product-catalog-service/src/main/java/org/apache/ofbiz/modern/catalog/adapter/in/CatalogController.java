package org.apache.ofbiz.modern.catalog.adapter.in;

import java.util.List;

import org.apache.ofbiz.modern.catalog.application.ProductCatalog;
import org.apache.ofbiz.modern.catalog.domain.ProductSummary;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/catalog/v1/products")
public class CatalogController {
    private static final int MAXIMUM_PAGE_SIZE = 100;
    private final ProductCatalog productCatalog;

    public CatalogController(ProductCatalog productCatalog) {
        this.productCatalog = productCatalog;
    }

    @GetMapping
    public Mono<ResponseEntity<SearchResponse>> search(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "25") int limit) {
        int boundedLimit = Math.clamp(limit, 1, MAXIMUM_PAGE_SIZE);
        return Mono.fromCallable(() -> productCatalog.search(query, boundedLimit))
                .subscribeOn(Schedulers.boundedElastic())
                .map(products -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(new SearchResponse(products)));
    }

    public record SearchResponse(List<ProductSummary> products) {
    }
}
