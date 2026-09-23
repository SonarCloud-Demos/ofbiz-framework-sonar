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
package org.apache.ofbiz.webapp.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;

class EntraIdentityBridgeFilterTests {
    @Test
    void parsesExplicitSubjectMappings() throws ServletException {
        assertEquals("admin", EntraIdentityBridgeFilter.parseMappings("subject-1=admin, subject-2=auditor")
                .get("subject-1"));
    }

    @Test
    void rejectsDuplicateSubjects() {
        assertThrows(ServletException.class, () ->
                EntraIdentityBridgeFilter.parseMappings("subject-1=admin,subject-1=auditor"));
    }

    @Test
    void rejectsMalformedMappings() {
        assertThrows(ServletException.class, () -> EntraIdentityBridgeFilter.parseMappings("subject-1"));
    }
}
