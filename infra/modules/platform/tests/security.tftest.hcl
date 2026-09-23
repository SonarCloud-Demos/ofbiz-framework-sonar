mock_provider "azurerm" {}

override_data {
  target = data.azurerm_client_config.current
  values = {
    tenant_id = "00000000-0000-0000-0000-000000000002"
  }
}

override_data {
  target = data.azurerm_policy_definition.allowed_locations
  values = {
    id = "/providers/Microsoft.Authorization/policyDefinitions/00000000-0000-0000-0000-000000000003"
  }
}

override_data {
  target = data.azurerm_policy_definition.require_tag
  values = {
    id = "/providers/Microsoft.Authorization/policyDefinitions/00000000-0000-0000-0000-000000000004"
  }
}

run "secure_platform_defaults" {
  command = plan

  variables {
    name                                  = "ofbiz-test"
    location                              = "switzerlandnorth"
    allowed_locations                     = ["switzerlandnorth", "global"]
    log_retention_days                    = 90
    budget_amount                         = 1000
    budget_contact_emails                 = ["platform@example.invalid"]
    apim_publisher_name                   = "Platform Team"
    apim_publisher_email                  = "platform@example.invalid"
    apim_sku_name                         = "Premium_1"
    postgres_administrator_login          = "platform-database-administrators"
    postgres_administrator_object_id      = "00000000-0000-0000-0000-000000000001"
    postgres_sku_name                     = "GP_Standard_D2s_v3"
    postgres_backup_retention_days        = 14
    postgres_geo_redundant_backup_enabled = true
    postgres_high_availability_enabled    = true
    edge_enabled                          = true
    entra_tenant_id                       = "00000000-0000-0000-0000-000000000002"
    entra_client_id                       = "00000000-0000-0000-0000-000000000005"
    entra_api_audience                    = "api://00000000-0000-0000-0000-000000000006"
    modern_shell_origin_url               = "https://shell.internal.example.invalid/modern"
    identity_origin_url                   = "https://identity.internal.example.invalid/auth"
    catalog_api_origin_url                = "https://catalog.internal.example.invalid/api/catalog"
    legacy_ofbiz_origin_url               = "https://ofbiz.internal.example.invalid/catalog"
    tags = {
      application         = "ofbiz-modernization"
      environment         = "test"
      service             = "platform"
      owner               = "platform-team"
      cost-center         = "test"
      data-classification = "internal"
      managed-by          = "terraform"
      criticality         = "tier-3"
    }
  }

  assert {
    condition = (
      azurerm_container_registry.platform.public_network_access_enabled == false &&
      azurerm_key_vault.platform.public_network_access_enabled == false &&
      azurerm_servicebus_namespace.platform.public_network_access_enabled == false &&
      azurerm_postgresql_flexible_server.platform.public_network_access_enabled == false &&
      azapi_update_resource.apim_disable_public_network_access.body.properties.publicNetworkAccess == "Disabled"
    )
    error_message = "Platform origins and data services must not allow public network access."
  }

  assert {
    condition = (
      azurerm_container_registry.platform.admin_enabled == false &&
      azurerm_app_configuration.platform.local_auth_enabled == false &&
      azurerm_servicebus_namespace.platform.local_auth_enabled == false &&
      azurerm_postgresql_flexible_server.platform.authentication[0].password_auth_enabled == false
    )
    error_message = "Shared keys, local authentication, and database passwords must remain disabled."
  }

  assert {
    condition = (
      azurerm_servicebus_namespace.platform.minimum_tls_version == "1.2" &&
      azurerm_api_management.platform.security[0].backend_tls10_enabled == false &&
      azurerm_api_management.platform.security[0].backend_tls11_enabled == false &&
      azurerm_api_management.platform.security[0].frontend_tls10_enabled == false &&
      azurerm_api_management.platform.security[0].frontend_tls11_enabled == false
    )
    error_message = "Deprecated TLS versions must remain disabled."
  }

  assert {
    condition     = length(azurerm_private_endpoint.services) == 4
    error_message = "Every supported shared platform service must retain a private endpoint."
  }

  assert {
    condition     = length(azurerm_monitor_diagnostic_setting.platform) == 11
    error_message = "Every supported platform resource must export diagnostics."
  }

  assert {
    condition = (
      length(azurerm_resource_group_policy_assignment.require_tags) == 8 &&
      azurerm_resource_group_policy_assignment.allowed_locations.enforce
    )
    error_message = "Region and mandatory-tag policy assignments must remain enforced."
  }

  assert {
    condition = (
      azurerm_cdn_frontdoor_endpoint.platform.enabled &&
      azurerm_cdn_frontdoor_route.platform.forwarding_protocol == "HttpsOnly" &&
      azurerm_cdn_frontdoor_firewall_policy.platform.mode == "Prevention" &&
      length(azurerm_cdn_frontdoor_firewall_policy.platform.managed_rule) == 2
    )
    error_message = "The Phase 4 edge must enforce HTTPS and WAF prevention before accepting traffic."
  }
}
