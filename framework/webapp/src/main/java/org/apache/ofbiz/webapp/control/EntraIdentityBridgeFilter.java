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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * Converts the identity context validated by the private Phase 4 edge into an OFBiz session.
 * This filter is disabled unless {@code OFBIZ_IDENTITY_BRIDGE_MODE=entra}; local development
 * therefore continues to use the normal OFBiz or local-gateway authentication paths.
 */
public final class EntraIdentityBridgeFilter implements Filter {
    private static final String MODULE = EntraIdentityBridgeFilter.class.getName();
    private static final String SUBJECT_HEADER = "X-Authenticated-Subject";
    private static final String TENANT_HEADER = "X-Authenticated-Tenant";

    private boolean enabled;
    private String expectedTenant;
    private Map<String, String> subjectMappings = Collections.emptyMap();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        enabled = "entra".equals(System.getenv("OFBIZ_IDENTITY_BRIDGE_MODE"));
        if (!enabled) {
            return;
        }

        expectedTenant = requireEnvironment("OFBIZ_ENTRA_TENANT_ID");
        subjectMappings = parseMappings(requireEnvironment("OFBIZ_ENTRA_SUBJECT_MAPPINGS"));
        if (subjectMappings.isEmpty()) {
            throw new ServletException("At least one explicit Entra subject mapping is required");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String subject = httpRequest.getHeader(SUBJECT_HEADER);
        String tenant = httpRequest.getHeader(TENANT_HEADER);
        String userLoginId = subjectMappings.get(subject);
        if (!expectedTenant.equals(tenant) || UtilValidate.isEmpty(userLoginId)) {
            deny(httpResponse);
            return;
        }

        try {
            establishSession(httpRequest, httpResponse, userLoginId);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to resolve the mapped edge identity", MODULE);
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        if (!httpResponse.isCommitted()) {
            chain.doFilter(request, response);
        }
    }

    private static void establishSession(HttpServletRequest request, HttpServletResponse response, String userLoginId)
            throws GenericEntityException, IOException {
        HttpSession session = request.getSession();
        GenericValue currentUser = (GenericValue) session.getAttribute("userLogin");
        if (currentUser != null && userLoginId.equals(currentUser.getString("userLoginId"))) {
            return;
        }
        session.invalidate();
        session = request.getSession(true);

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin")
                .where("userLoginId", userLoginId).cache(false).queryOne();
        if (userLogin == null || "N".equals(userLogin.getString("enabled"))) {
            deny(response);
            return;
        }

        String loginResult = LoginWorker.doMainLogin(request, response, userLogin, null);
        if ("error".equals(loginResult)) {
            deny(response);
            return;
        }
        session.setAttribute("entraSubject", request.getHeader(SUBJECT_HEADER));
        session.setAttribute("entraTenant", request.getHeader(TENANT_HEADER));
    }

    static Map<String, String> parseMappings(String rawMappings) throws ServletException {
        Map<String, String> mappings = new HashMap<>();
        for (String entry : rawMappings.split(",")) {
            String[] pair = entry.trim().split("=", 2);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank() || mappings.put(pair[0], pair[1]) != null) {
                throw new ServletException("Entra subject mappings must contain unique subject=userLoginId entries");
            }
        }
        return Map.copyOf(mappings);
    }

    private static String requireEnvironment(String name) throws ServletException {
        String value = System.getenv(name);
        if (UtilValidate.isEmpty(value)) {
            throw new ServletException(name + " is required when the Entra identity bridge is enabled");
        }
        return value;
    }

    private static void deny(HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
