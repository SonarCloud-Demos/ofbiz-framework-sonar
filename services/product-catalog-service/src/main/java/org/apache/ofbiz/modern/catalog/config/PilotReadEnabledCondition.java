package org.apache.ofbiz.modern.catalog.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public final class PilotReadEnabledCondition implements Condition {
    private static final String LOCAL_SECURITY_MODE = "local";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String securityMode = context.getEnvironment().getProperty("catalog.security.mode", "");
        boolean explicitlyEnabled = context.getEnvironment().getProperty(
                "catalog.pilot.read-enabled", Boolean.class, false);
        return LOCAL_SECURITY_MODE.equals(securityMode) || explicitlyEnabled;
    }
}
