mock_provider "azurerm" {
  mock_data "azurerm_client_config" {
    defaults = { tenant_id = "00000000-0000-0000-0000-000000000001" }
  }
  mock_data "azurerm_resource_group" {
    defaults = { id = "/subscriptions/test/resourceGroups/ofbiz-test-rg" }
  }
}

variables {
  subscription_id    = "00000000-0000-0000-0000-000000000001"
  tenant_id          = "00000000-0000-0000-0000-000000000001"
  environment        = "test"
  vnet_cidr          = "10.20.0.0/20"
  alert_email        = "platform@example.invalid"
  monthly_budget     = 500
  sample_image       = "example.invalid/platform-sample@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  shell_image        = "example.invalid/web-shell@sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
  legacy_origin_host = "legacy.example.invalid"
  extra_tags = {
    owner               = "platform-team"
    cost-center         = "replace-before-apply"
    data-classification = "internal"
    criticality         = "medium"
  }
}

run "secure_platform_plan" {
  command = plan

  assert {
    condition     = module.data_platform.security_controls.key_vault_public_access == false
    error_message = "Key Vault must not expose its data plane publicly."
  }
  assert {
    condition     = module.data_platform.security_controls.servicebus_local_auth == false
    error_message = "Service Bus local authentication must remain disabled."
  }
  assert {
    condition     = module.data_platform.security_controls.storage_public_access == false
    error_message = "Blob Storage must not expose its data plane publicly."
  }
  assert {
    condition     = module.data_platform.security_controls.postgres_public_access == false
    error_message = "PostgreSQL must not expose its data plane publicly."
  }
  assert {
    condition     = module.data_platform.security_controls.postgres_password_auth == false
    error_message = "PostgreSQL must use Entra authentication instead of administrator passwords."
  }
  assert {
    condition     = module.data_platform.security_controls.appconfig_public_access == "Disabled" && module.data_platform.security_controls.appconfig_local_auth == false
    error_message = "App Configuration must be private and reject local access keys."
  }
  assert {
    condition     = module.data_platform.security_controls.redis_minimum_tls == "1.2"
    error_message = "Azure Managed Redis must require TLS 1.2."
  }
  assert {
    condition     = module.container_platform.security_controls.registry_admin_enabled == false
    error_message = "ACR administrator credentials must remain disabled."
  }
  assert {
    condition     = module.edge.security_controls.waf_mode == "Prevention"
    error_message = "The edge WAF must operate in prevention mode."
  }
  assert {
    condition     = module.edge.security_controls.apim_public_access == false
    error_message = "API Management must remain private."
  }
  assert {
    condition     = module.diagnostics.target_count == 14
    error_message = "Every supported platform resource must export diagnostics."
  }
  assert {
    condition     = length(azurerm_management_lock.production_stateful) == 0
    error_message = "Non-production environments must remain disposable."
  }
}

run "production_stateful_resources_are_locked" {
  command = plan

  variables { environment = "prod" }

  assert {
    condition     = length(azurerm_management_lock.production_stateful) == 6
    error_message = "Every production stateful resource must have deletion protection."
  }
}
