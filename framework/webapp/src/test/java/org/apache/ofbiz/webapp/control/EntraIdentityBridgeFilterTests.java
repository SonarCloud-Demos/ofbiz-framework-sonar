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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
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

    @Test
    void disabledBridgePassesThrough() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        EntraIdentityBridgeFilter filter = new EntraIdentityBridgeFilter(name -> null);

        filter.init(mock(FilterConfig.class));
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void enabledBridgeRequiresConfiguration() {
        Map<String, String> environment = new HashMap<>();
        environment.put("OFBIZ_IDENTITY_BRIDGE_MODE", "entra");
        EntraIdentityBridgeFilter filter = new EntraIdentityBridgeFilter(environment::get);

        assertThrows(ServletException.class, () -> filter.init(mock(FilterConfig.class)));

        environment.put("OFBIZ_ENTRA_TENANT_ID", "tenant-1");
        assertThrows(ServletException.class, () -> filter.init(mock(FilterConfig.class)));
    }

    @Test
    void enabledBridgeRejectsMissingOrUnmappedIdentity() throws Exception {
        EntraIdentityBridgeFilter filter = enabledBridge(null, false);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void enabledBridgeReusesMatchingSession() throws Exception {
        GenericValue currentUser = mock(GenericValue.class);
        when(currentUser.getString("userLoginId")).thenReturn("admin");
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("userLogin")).thenReturn(currentUser);
        HttpServletRequest request = authenticatedRequest(session);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        EntraIdentityBridgeFilter filter = enabledBridge(currentUser, false);

        filter.doFilter(request, response, chain);

        verify(session, never()).invalidate();
        verify(chain).doFilter(request, response);
    }

    @Test
    void enabledBridgeRotatesSessionAndLogsInMappedUser() throws Exception {
        GenericValue mappedUser = mock(GenericValue.class);
        when(mappedUser.getString("enabled")).thenReturn("Y");
        HttpSession oldSession = mock(HttpSession.class);
        HttpSession newSession = mock(HttpSession.class);
        HttpServletRequest request = authenticatedRequest(oldSession);
        when(request.getSession(true)).thenReturn(newSession);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        EntraIdentityBridgeFilter filter = enabledBridge(mappedUser, false);

        filter.doFilter(request, response, chain);

        verify(oldSession).invalidate();
        verify(newSession).setAttribute("entraSubject", "subject-1");
        verify(newSession).setAttribute("entraTenant", "tenant-1");
        verify(chain).doFilter(request, response);
    }

    @Test
    void enabledBridgeRejectsDisabledMappedUser() throws Exception {
        GenericValue mappedUser = mock(GenericValue.class);
        when(mappedUser.getString("enabled")).thenReturn("N");
        HttpSession oldSession = mock(HttpSession.class);
        HttpServletRequest request = authenticatedRequest(oldSession);
        when(request.getSession(true)).thenReturn(mock(HttpSession.class));
        HttpServletResponse response = mock(HttpServletResponse.class);
        EntraIdentityBridgeFilter filter = enabledBridge(mappedUser, false);

        filter.doFilter(request, response, mock(FilterChain.class));

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void enabledBridgeReturnsUnavailableWhenLookupFails() throws Exception {
        HttpServletRequest request = authenticatedRequest(mock(HttpSession.class));
        when(request.getSession(true)).thenReturn(mock(HttpSession.class));
        HttpServletResponse response = mock(HttpServletResponse.class);
        EntraIdentityBridgeFilter filter = enabledBridge(null, true);

        filter.doFilter(request, response, mock(FilterChain.class));

        verify(response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    private static EntraIdentityBridgeFilter enabledBridge(GenericValue userLogin, boolean lookupFailure)
            throws ServletException {
        EntraIdentityBridgeFilter.UserResolver resolver = (delegator, userLoginId) -> {
            if (lookupFailure) {
                throw new GenericEntityException("lookup failed");
            }
            return userLogin;
        };
        EntraIdentityBridgeFilter.LoginHandler loginHandler = (request, response, mappedUser) -> "success";
        EntraIdentityBridgeFilter filter = new EntraIdentityBridgeFilter(name -> Map.of(
                "OFBIZ_IDENTITY_BRIDGE_MODE", "entra",
                "OFBIZ_ENTRA_TENANT_ID", "tenant-1",
                "OFBIZ_ENTRA_SUBJECT_MAPPINGS", "subject-1=admin").get(name), resolver,
                loginHandler);
        filter.init(mock(FilterConfig.class));
        return filter;
    }

    private static HttpServletRequest authenticatedRequest(HttpSession session) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Authenticated-Subject")).thenReturn("subject-1");
        when(request.getHeader("X-Authenticated-Tenant")).thenReturn("tenant-1");
        when(request.getSession()).thenReturn(session);
        when(request.getAttribute("delegator")).thenReturn(mock(Delegator.class));
        return request;
    }

}
