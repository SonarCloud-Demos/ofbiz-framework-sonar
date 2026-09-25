package org.apache.ofbiz.product.product;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

/** Transitional read-only anti-corruption endpoint for the modern catalog shadow. */
public final class ModernCatalogReadEvents {
    private static final String SYNC_TOKEN_HEADER = "X-Modern-Catalog-Token";
    private static final String PRODUCT_ID = "productId";
    private static final String PRODUCT_ENTITY = "Product";
    private static final String PRODUCT_NAME = "productName";
    private static final String INTERNAL_NAME = "internalName";
    private static final String CATEGORY_ID = "productCategoryId";
    private static final String CATEGORY_ENTITY = "ProductCategory";
    private static final String CATEGORY_NAME = "categoryName";
    private static final String PRIMARY_CATEGORY_ID = "primaryProductCategoryId";
    private static final String DISCONTINUATION_DATE = "salesDiscontinuationDate";
    private static final String UNCATEGORIZED = "UNCATEGORIZED";
    private static final String NOT_CONFIGURED = "Not configured";
    private static final String CHANGE_ENTITY = "ModernCatalogChange";
    private static final String CHANGE_SEQUENCE = "changeSequence";
    private static final String CHANGED_INSTANCE = "changedInstance";
    private static final String EVENT_TYPE = "eventType";
    private static final String CATALOG_STATUS = "catalogStatus";
    private static final int CHANGE_BATCH_SIZE = 500;

    private ModernCatalogReadEvents() {
    }

    public static String snapshot(HttpServletRequest request, HttpServletResponse response)
            throws GenericEntityException, IOException {
        if (!authorized(request.getHeader(SYNC_TOKEN_HEADER), System.getenv("LEGACY_CATALOG_SYNC_TOKEN"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return "error";
        }
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String, String> categoryNames = categoryNames(delegator);
        List<GenericValue> products = EntityQuery.use(delegator).select(PRODUCT_ID, PRODUCT_NAME, INTERNAL_NAME,
                PRIMARY_CATEGORY_ID, DISCONTINUATION_DATE).from(PRODUCT_ENTITY).orderBy(PRODUCT_ID).queryList();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        for (GenericValue product : products) {
            writeProduct(response, delegator, categoryNames, product);
        }
        return "success";
    }

    public static String changes(HttpServletRequest request, HttpServletResponse response)
            throws GenericEntityException, IOException {
        if (!authorized(request.getHeader(SYNC_TOKEN_HEADER), System.getenv("LEGACY_CATALOG_SYNC_TOKEN"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return "error";
        }
        long after = nonNegativeLong(request.getParameter("after"));
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        List<GenericValue> changes = EntityQuery.use(delegator).from(CHANGE_ENTITY)
                .where(EntityCondition.makeCondition(CHANGE_SEQUENCE, EntityOperator.GREATER_THAN, after))
                .orderBy(CHANGE_SEQUENCE).maxRows(CHANGE_BATCH_SIZE).queryList();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        for (GenericValue change : changes) {
            response.getWriter().write(changeLine(change));
        }
        return "success";
    }

    public static Map<String, Object> enqueueUpsert(DispatchContext dctx, Map<String, Object> context) {
        try {
            GenericValue changed = (GenericValue) context.get(CHANGED_INSTANCE);
            for (String productId : affectedProductIds(dctx.getDelegator(), changed)) {
                GenericValue product = EntityQuery.use(dctx.getDelegator()).from(PRODUCT_ENTITY)
                        .where(PRODUCT_ID, productId).queryOne();
                if (product != null) {
                    appendChange(dctx.getDelegator(), "UPSERT", product);
                }
            }
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException failure) {
            return ServiceUtil.returnError("Unable to append the modern catalog change: " + failure.getMessage());
        }
    }

    public static Map<String, Object> enqueueDelete(DispatchContext dctx, Map<String, Object> context) {
        try {
            GenericValue changed = (GenericValue) context.get(CHANGED_INSTANCE);
            GenericValue event = dctx.getDelegator().makeValue(CHANGE_ENTITY);
            event.set(CHANGE_SEQUENCE, nextSequence(dctx.getDelegator()));
            event.set(EVENT_TYPE, "DELETE");
            event.set(PRODUCT_ID, changed.getString(PRODUCT_ID));
            event.set("occurredAt", new Timestamp(System.currentTimeMillis()));
            event.create();
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException failure) {
            return ServiceUtil.returnError("Unable to append the modern catalog deletion: " + failure.getMessage());
        }
    }

    static boolean authorized(String supplied, String expected) {
        if (supplied == null || expected == null || expected.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, String> categoryNames(Delegator delegator) throws GenericEntityException {
        Map<String, String> names = new HashMap<>();
        for (GenericValue category : EntityQuery.use(delegator).select(CATEGORY_ID, CATEGORY_NAME)
                .from(CATEGORY_ENTITY).queryList()) {
            names.put(category.getString(CATEGORY_ID), value(category.getString(CATEGORY_NAME),
                    category.getString(CATEGORY_ID)));
        }
        return names;
    }

    private static void writeProduct(HttpServletResponse response, Delegator delegator, Map<String, String> categories,
            GenericValue product) throws GenericEntityException, IOException {
        String categoryId = value(product.getString(PRIMARY_CATEGORY_ID), UNCATEGORIZED);
        String categoryName = categories.getOrDefault(categoryId, categoryId);
        String productName = value(product.getString(PRODUCT_NAME),
                value(product.getString(INTERNAL_NAME), product.getString(PRODUCT_ID)));
        Timestamp discontinued = product.getTimestamp(DISCONTINUATION_DATE);
        String status = discontinued == null || discontinued.after(new Timestamp(System.currentTimeMillis()))
                ? "ACTIVE" : "INACTIVE";
        String priceReference = displayedPriceReference(delegator, product.getString(PRODUCT_ID));
        String line = encoded(product.getString(PRODUCT_ID)) + '|' + encoded(productName) + '|'
                + encoded(categoryId) + '|' + encoded(categoryName) + '|' + encoded(status) + '|'
                + encoded(priceReference) + '\n';
        response.getWriter().write(line);
    }

    private static String displayedPriceReference(Delegator delegator, String productId) throws GenericEntityException {
        GenericValue price = EntityQuery.use(delegator).from("ProductPrice")
                .where(PRODUCT_ID, productId, "productPriceTypeId", "LIST_PRICE")
                .orderBy("-fromDate").filterByDate().queryFirst();
        if (price == null) {
            return NOT_CONFIGURED;
        }
        BigDecimal amount = price.getBigDecimal("price");
        String currency = value(price.getString("currencyUomId"), "");
        return amount == null ? NOT_CONFIGURED : (currency + ' ' + amount.toPlainString()).trim();
    }

    private static Set<String> affectedProductIds(Delegator delegator, GenericValue changed)
            throws GenericEntityException {
        if (!CATEGORY_ENTITY.equals(changed.getEntityName())) {
            return Set.of(changed.getString(PRODUCT_ID));
        }
        Set<String> productIds = new LinkedHashSet<>();
        for (GenericValue product : EntityQuery.use(delegator).select(PRODUCT_ID).from(PRODUCT_ENTITY)
                .where(PRIMARY_CATEGORY_ID, changed.getString(CATEGORY_ID)).queryList()) {
            productIds.add(product.getString(PRODUCT_ID));
        }
        return productIds;
    }

    private static void appendChange(Delegator delegator, String eventType, GenericValue product)
            throws GenericEntityException {
        String categoryId = value(product.getString(PRIMARY_CATEGORY_ID), UNCATEGORIZED);
        GenericValue category = EntityQuery.use(delegator).from(CATEGORY_ENTITY)
                .where(CATEGORY_ID, categoryId).queryOne();
        String categoryName = category == null ? categoryId : value(category.getString(CATEGORY_NAME), categoryId);
        String productName = value(product.getString(PRODUCT_NAME),
                value(product.getString(INTERNAL_NAME), product.getString(PRODUCT_ID)));
        Timestamp discontinued = product.getTimestamp(DISCONTINUATION_DATE);
        String status = discontinued == null || discontinued.after(new Timestamp(System.currentTimeMillis()))
                ? "ACTIVE" : "INACTIVE";
        GenericValue event = delegator.makeValue(CHANGE_ENTITY);
        event.set(CHANGE_SEQUENCE, nextSequence(delegator));
        event.set(EVENT_TYPE, eventType);
        event.set(PRODUCT_ID, product.getString(PRODUCT_ID));
        event.set(PRODUCT_NAME, productName);
        event.set(CATEGORY_ID, categoryId);
        event.set(CATEGORY_NAME, categoryName);
        event.set(CATALOG_STATUS, status);
        event.set("displayedPriceReference", displayedPriceReference(delegator, product.getString(PRODUCT_ID)));
        event.set("occurredAt", new Timestamp(System.currentTimeMillis()));
        event.create();
    }

    private static Long nextSequence(Delegator delegator) {
        return Long.valueOf(delegator.getNextSeqId(CHANGE_ENTITY));
    }

    private static String changeLine(GenericValue change) {
        return change.getLong(CHANGE_SEQUENCE) + "|" + change.getString(EVENT_TYPE) + "|"
                + encoded(change.getString(PRODUCT_ID)) + '|' + encoded(change.getString(PRODUCT_NAME)) + '|'
                + encoded(change.getString(CATEGORY_ID)) + '|' + encoded(change.getString(CATEGORY_NAME)) + '|'
                + encoded(change.getString(CATALOG_STATUS)) + '|'
                + encoded(change.getString("displayedPriceReference")) + '\n';
    }

    private static long nonNegativeLong(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Long.parseLong(candidate));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String encoded(String value) {
        String safeValue = value == null ? "" : value;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(safeValue.getBytes(StandardCharsets.UTF_8));
    }

    private static String value(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate;
    }
}
