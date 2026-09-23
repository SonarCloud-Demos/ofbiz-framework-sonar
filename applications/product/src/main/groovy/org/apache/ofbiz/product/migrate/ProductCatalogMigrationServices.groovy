/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0.
 */
package org.apache.ofbiz.product.migrate

import java.time.Instant

import groovy.transform.Field
import org.apache.ofbiz.base.util.collections.PagedList
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityFunction
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil

@Field final int DEFAULT_PAGE_SIZE = 20
@Field final int MAXIMUM_PAGE_SIZE = 100
@Field final String PRODUCT_ID = 'productId'
@Field final String PRODUCT_TYPE_ID = 'productTypeId'
@Field final String INTERNAL_NAME = 'internalName'
@Field final String BRAND_NAME = 'brandName'
@Field final String PRODUCT_NAME = 'productName'
@Field final String DESCRIPTION = 'description'
@Field final Map<String, String> SORT_FIELDS = [
        (PRODUCT_ID): PRODUCT_ID,
        (PRODUCT_TYPE_ID): PRODUCT_TYPE_ID,
        (INTERNAL_NAME): INTERNAL_NAME,
        (BRAND_NAME): BRAND_NAME,
        (PRODUCT_NAME): PRODUCT_NAME,
        (DESCRIPTION): DESCRIPTION
].asImmutable()

Map findProductsForCatalogMigration() {
    String productId = (parameters.productId ?: '').strip()
    String internalName = (parameters.internalName ?: '').strip()
    String sort = parameters.sort ?: 'productId'
    String direction = (parameters.direction ?: 'asc').toLowerCase(Locale.ROOT)
    int page = parameters.page == null ? 0 : parameters.page as int
    int size = parameters.size == null ? DEFAULT_PAGE_SIZE : parameters.size as int
    if (productId.length() > 20 || internalName.length() > 255 || page < 0
            || size < 1 || size > MAXIMUM_PAGE_SIZE || !SORT_FIELDS.containsKey(sort)
            || !(direction in ['asc', 'desc'])) {
        return ServiceUtil.returnError('Invalid catalog migration search parameters')
    }

    List<EntityCondition> conditions = []
    if (productId) {
        conditions.add(EntityCondition.makeCondition(PRODUCT_ID, EntityOperator.EQUALS, productId))
    }
    if (internalName) {
        String escaped = internalName.replace('\\', '\\\\').replace('%', '\\%').replace('_', '\\_')
        conditions.add(EntityCondition.makeCondition(
                EntityFunction.upperField(INTERNAL_NAME),
                EntityOperator.LIKE,
                EntityFunction.upper('%' + escaped + '%')))
    }
    EntityCondition where = conditions
            ? EntityCondition.makeCondition(conditions, EntityOperator.AND)
            : null
    String sortField = SORT_FIELDS[sort]
    String directionPrefix = direction == 'desc' ? '-' : ''
    List<String> orderBy = [directionPrefix + sortField]
    if (sortField != PRODUCT_ID) {
        orderBy.add(directionPrefix + PRODUCT_ID)
    }

    EntityQuery productQuery = EntityQuery.use(delegator)
            .from('Product')
            .select(PRODUCT_ID, PRODUCT_TYPE_ID, INTERNAL_NAME, BRAND_NAME, PRODUCT_NAME, DESCRIPTION)
    if (where) {
        productQuery.where(where)
    }
    PagedList<GenericValue> products = productQuery
            .orderBy(orderBy)
            .cursorScrollInsensitive()
            .queryPagedList(page, size)
    List<Map<String, Object>> items = products.data.collect { GenericValue product ->
        [
            (PRODUCT_ID): product.getString(PRODUCT_ID),
            (PRODUCT_TYPE_ID): product.getString(PRODUCT_TYPE_ID),
            (INTERNAL_NAME): product.getString(INTERNAL_NAME),
            (BRAND_NAME): product.getString(BRAND_NAME),
            (PRODUCT_NAME): product.getString(PRODUCT_NAME),
            (DESCRIPTION): product.getString(DESCRIPTION)
        ]
    }
    return ServiceUtil.returnSuccess() + [
            items: items,
            page: page,
            size: size,
            total: products.size as long,
            projectionTimestamp: Instant.now().toString()
    ]
}
