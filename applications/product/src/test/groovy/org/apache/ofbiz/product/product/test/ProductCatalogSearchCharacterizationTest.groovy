/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.product.product.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@JunitJupiterTest
class ProductCatalogSearchCharacterizationTest implements JupiterTestHelper {

    private static final String PRODUCT_ID_PREFIX = 'Phase1_catalog_'
    private static final String PRIMARY_PRODUCT_ID = PRODUCT_ID_PREFIX + 'A'
    private static final String PRODUCT_TYPE_ID = 'Phase1_type'
    private static final String NO_CONDITION_FIND = 'Y'

    @BeforeEach
    void setUpCharacterizationProducts() {
        delegator.createOrStore(delegator.makeValue('ProductType', [productTypeId: PRODUCT_TYPE_ID]))
        delegator.createOrStore(delegator.makeValue('Product', [
                productId: PRIMARY_PRODUCT_ID,
                productTypeId: PRODUCT_TYPE_ID,
                internalName: 'Phase 1 internal name',
                brandName: 'Phase 1 brand',
                productName: 'Phase 1 product',
                description: 'Phase 1 characterization product'
        ]))
        delegator.createOrStore(delegator.makeValue('Product', [
                productId: PRODUCT_ID_PREFIX + 'B',
                productTypeId: PRODUCT_TYPE_ID,
                internalName: 'Second Phase 1 product'
        ]))
        delegator.createOrStore(delegator.makeValue('Product', [
                productId: PRODUCT_ID_PREFIX + 'C',
                productTypeId: PRODUCT_TYPE_ID,
                internalName: 'Third phase 1 product'
        ]))
    }

    @Test
    void exactProductIdSearchReturnsLegacyGridFieldsWithoutWritingProducts() {
        long productCountBefore = from('Product').queryCount()
        long keywordCountBefore = from('ProductKeyword').where('productId', PRIMARY_PRODUCT_ID).queryCount()

        Map result = dispatcher.runSync('performFindList', [
                entityName: 'Product',
                inputFields: [productId: PRIMARY_PRODUCT_ID, noConditionFind: NO_CONDITION_FIND],
                orderBy: 'productId',
                viewIndex: 0,
                viewSize: 20,
                userLogin: userLogin
        ])

        assert ServiceUtil.isSuccess(result)
        assert result.listSize == 1
        assert result.list.size() == 1

        GenericValue product = result.list.first()
        assert product.productId == PRIMARY_PRODUCT_ID
        assert product.productTypeId == PRODUCT_TYPE_ID
        assert product.internalName == 'Phase 1 internal name'
        assert product.brandName == 'Phase 1 brand'
        assert product.productName == 'Phase 1 product'
        assert product.description == 'Phase 1 characterization product'
        assert from('Product').queryCount() == productCountBefore
        assert from('ProductKeyword').where('productId', PRIMARY_PRODUCT_ID).queryCount() == keywordCountBefore
    }

    @Test
    void unfilteredSearchHonorsLegacyPagination() {
        Map result = dispatcher.runSync('performFindList', [
                entityName: 'Product',
                inputFields: [
                        productId: PRODUCT_ID_PREFIX,
                        productId_op: 'contains',
                        noConditionFind: NO_CONDITION_FIND
                ],
                orderBy: 'productId',
                viewIndex: 0,
                viewSize: 2,
                userLogin: userLogin
        ])

        assert ServiceUtil.isSuccess(result)
        assert result.listSize >= result.list.size()
        assert result.list.size() == 2
        assert result.listSize == 3
        assert result.list*.productId == result.list*.productId.sort()
    }

    @Test
    void internalNameSearchIsCaseInsensitiveWhenRequested() {
        Map result = dispatcher.runSync('performFindList', [
                entityName: 'Product',
                inputFields: [
                        internalName: 'PHASE 1',
                        internalName_op: 'contains',
                        internalName_ic: NO_CONDITION_FIND,
                        noConditionFind: NO_CONDITION_FIND
                ],
                orderBy: 'productId',
                viewIndex: 0,
                viewSize: 20,
                userLogin: userLogin
        ])

        assert ServiceUtil.isSuccess(result)
        assert result.listSize == 3
        assert result.list*.productId == [PRIMARY_PRODUCT_ID, PRODUCT_ID_PREFIX + 'B', PRODUCT_ID_PREFIX + 'C']
    }

    @Test
    void secondPageReturnsRemainingMatchingProduct() {
        Map result = dispatcher.runSync('performFindList', [
                entityName: 'Product',
                inputFields: [
                        productId: PRODUCT_ID_PREFIX,
                        productId_op: 'contains',
                        noConditionFind: NO_CONDITION_FIND
                ],
                orderBy: 'productId',
                viewIndex: 1,
                viewSize: 2,
                userLogin: userLogin
        ])

        assert ServiceUtil.isSuccess(result)
        assert result.listSize == 3
        assert result.list*.productId == [PRODUCT_ID_PREFIX + 'C']
    }

    @Test
    void emptyFiltersReturnLegacyNullListWithoutExplicitOptIn() {
        Map result = dispatcher.runSync('performFindList', [
                entityName: 'Product',
                inputFields: [:],
                orderBy: 'productId',
                viewIndex: 0,
                viewSize: 20,
                userLogin: userLogin
        ])

        assert ServiceUtil.isSuccess(result)
        assert result.listSize == 0
        assert result.list == null
    }

}
