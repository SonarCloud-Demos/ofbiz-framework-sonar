package org.apache.ofbiz.modern.catalog.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class StaleProjectionException extends RuntimeException {
    public StaleProjectionException() {
        super("Catalog projection is outside its freshness policy");
    }
}
