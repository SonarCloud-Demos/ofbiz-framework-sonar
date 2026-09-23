locals {
  compact_name = replace(lower(var.name), "-", "")
}

resource "azurerm_resource_group" "platform" {
  name     = "rg-${var.name}"
  location = var.location
  tags     = var.tags
}

resource "azurerm_virtual_network" "platform" {
  name                = "vnet-${var.name}"
  address_space       = ["10.40.0.0/16"]
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name
  tags                = var.tags
}

resource "azurerm_subnet" "container_apps" {
  name                 = "snet-container-apps"
  resource_group_name  = azurerm_resource_group.platform.name
  virtual_network_name = azurerm_virtual_network.platform.name
  address_prefixes     = ["10.40.0.0/23"]
  delegation {
    name = "container-apps"
    service_delegation {
      name = "Microsoft.App/environments"
    }
  }
}

resource "azurerm_subnet" "private_endpoints" {
  name                              = "snet-private-endpoints"
  resource_group_name               = azurerm_resource_group.platform.name
  virtual_network_name              = azurerm_virtual_network.platform.name
  address_prefixes                  = ["10.40.2.0/24"]
  private_endpoint_network_policies = "Disabled"
}

resource "azurerm_subnet" "postgres" {
  name                 = "snet-postgres"
  resource_group_name  = azurerm_resource_group.platform.name
  virtual_network_name = azurerm_virtual_network.platform.name
  address_prefixes     = ["10.40.3.0/24"]

  delegation {
    name = "postgres-flexible-server"
    service_delegation {
      name = "Microsoft.DBforPostgreSQL/flexibleServers"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/join/action",
      ]
    }
  }
}

resource "azurerm_subnet" "api_management" {
  name                 = "snet-api-management"
  resource_group_name  = azurerm_resource_group.platform.name
  virtual_network_name = azurerm_virtual_network.platform.name
  address_prefixes     = ["10.40.4.0/24"]
}

resource "azurerm_network_security_group" "api_management" {
  name                = "nsg-apim-${var.name}"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name
  tags                = var.tags

  security_rule {
    name                       = "allow-management-plane"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "3443"
    source_address_prefix      = "ApiManagement"
    destination_address_prefix = "VirtualNetwork"
  }

  security_rule {
    name                       = "allow-load-balancer"
    priority                   = 110
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "6390"
    source_address_prefix      = "AzureLoadBalancer"
    destination_address_prefix = "VirtualNetwork"
  }
}

resource "azurerm_subnet_network_security_group_association" "api_management" {
  subnet_id                 = azurerm_subnet.api_management.id
  network_security_group_id = azurerm_network_security_group.api_management.id
}

resource "azurerm_log_analytics_workspace" "platform" {
  name                = "log-${var.name}"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name
  sku                 = "PerGB2018"
  retention_in_days   = var.log_retention_days
  tags                = var.tags
}

resource "azurerm_application_insights" "platform" {
  name                       = "appi-${var.name}"
  location                   = azurerm_resource_group.platform.location
  resource_group_name        = azurerm_resource_group.platform.name
  workspace_id               = azurerm_log_analytics_workspace.platform.id
  application_type           = "web"
  internet_ingestion_enabled = false
  internet_query_enabled     = false
  tags                       = var.tags
}

resource "azurerm_container_registry" "platform" {
  name                          = "acr${local.compact_name}"
  resource_group_name           = azurerm_resource_group.platform.name
  location                      = azurerm_resource_group.platform.location
  sku                           = "Premium"
  admin_enabled                 = false
  public_network_access_enabled = false
  zone_redundancy_enabled       = true
  tags                          = var.tags

  identity {
    type = "SystemAssigned"
  }
}

resource "azurerm_key_vault" "platform" {
  name                          = "kv-${var.name}"
  location                      = azurerm_resource_group.platform.location
  resource_group_name           = azurerm_resource_group.platform.name
  tenant_id                     = data.azurerm_client_config.current.tenant_id
  sku_name                      = "premium"
  rbac_authorization_enabled    = true
  public_network_access_enabled = false
  purge_protection_enabled      = true
  soft_delete_retention_days    = 90
  tags                          = var.tags
}

data "azurerm_client_config" "current" {}

resource "azurerm_app_configuration" "platform" {
  name                       = "appcs-${var.name}"
  resource_group_name        = azurerm_resource_group.platform.name
  location                   = azurerm_resource_group.platform.location
  sku                        = "standard"
  public_network_access      = "Disabled"
  purge_protection_enabled   = true
  local_auth_enabled         = false
  soft_delete_retention_days = 7
  identity { type = "SystemAssigned" }
  tags = var.tags
}

resource "azurerm_servicebus_namespace" "platform" {
  name                          = "sb-${var.name}"
  location                      = azurerm_resource_group.platform.location
  resource_group_name           = azurerm_resource_group.platform.name
  sku                           = "Premium"
  capacity                      = 1
  premium_messaging_partitions  = 1
  local_auth_enabled            = false
  public_network_access_enabled = false
  minimum_tls_version           = "1.2"
  identity { type = "SystemAssigned" }
  tags = var.tags
}

resource "azurerm_container_app_environment" "platform" {
  name                           = "cae-${var.name}"
  location                       = azurerm_resource_group.platform.location
  resource_group_name            = azurerm_resource_group.platform.name
  log_analytics_workspace_id     = azurerm_log_analytics_workspace.platform.id
  infrastructure_subnet_id       = azurerm_subnet.container_apps.id
  internal_load_balancer_enabled = true
  zone_redundancy_enabled        = true
  tags                           = var.tags
}

resource "azurerm_private_dns_zone" "services" {
  for_each = toset([
    "privatelink.azurecr.io",
    "privatelink.azconfig.io",
    "privatelink.servicebus.windows.net",
    "privatelink.vaultcore.azure.net",
  ])

  name                = each.value
  resource_group_name = azurerm_resource_group.platform.name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "services" {
  for_each = azurerm_private_dns_zone.services

  name                  = "link-${local.compact_name}"
  resource_group_name   = azurerm_resource_group.platform.name
  private_dns_zone_name = each.value.name
  virtual_network_id    = azurerm_virtual_network.platform.id
  registration_enabled  = false
  tags                  = var.tags
}

locals {
  private_endpoints = {
    acr = {
      resource_id    = azurerm_container_registry.platform.id
      subresource    = "registry"
      private_dns_id = azurerm_private_dns_zone.services["privatelink.azurecr.io"].id
    }
    app_configuration = {
      resource_id    = azurerm_app_configuration.platform.id
      subresource    = "configurationStores"
      private_dns_id = azurerm_private_dns_zone.services["privatelink.azconfig.io"].id
    }
    key_vault = {
      resource_id    = azurerm_key_vault.platform.id
      subresource    = "vault"
      private_dns_id = azurerm_private_dns_zone.services["privatelink.vaultcore.azure.net"].id
    }
    service_bus = {
      resource_id    = azurerm_servicebus_namespace.platform.id
      subresource    = "namespace"
      private_dns_id = azurerm_private_dns_zone.services["privatelink.servicebus.windows.net"].id
    }
  }
}

resource "azurerm_private_endpoint" "services" {
  for_each = local.private_endpoints

  name                = "pe-${each.key}-${var.name}"
  location            = azurerm_resource_group.platform.location
  resource_group_name = azurerm_resource_group.platform.name
  subnet_id           = azurerm_subnet.private_endpoints.id
  tags                = var.tags

  private_service_connection {
    name                           = "psc-${each.key}-${var.name}"
    private_connection_resource_id = each.value.resource_id
    subresource_names              = [each.value.subresource]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "default"
    private_dns_zone_ids = [each.value.private_dns_id]
  }
}

resource "azurerm_private_dns_zone" "postgres" {
  name                = "privatelink.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.platform.name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "postgres" {
  name                  = "link-${local.compact_name}"
  resource_group_name   = azurerm_resource_group.platform.name
  private_dns_zone_name = azurerm_private_dns_zone.postgres.name
  virtual_network_id    = azurerm_virtual_network.platform.id
  registration_enabled  = false
  tags                  = var.tags
}

resource "azurerm_postgresql_flexible_server" "platform" {
  name                          = "psql-${var.name}"
  resource_group_name           = azurerm_resource_group.platform.name
  location                      = azurerm_resource_group.platform.location
  version                       = "16"
  delegated_subnet_id           = azurerm_subnet.postgres.id
  private_dns_zone_id           = azurerm_private_dns_zone.postgres.id
  public_network_access_enabled = false
  sku_name                      = var.postgres_sku_name
  storage_mb                    = 32768
  backup_retention_days         = var.postgres_backup_retention_days
  geo_redundant_backup_enabled  = var.postgres_geo_redundant_backup_enabled
  auto_grow_enabled             = true
  tags                          = var.tags

  authentication {
    active_directory_auth_enabled = true
    password_auth_enabled         = false
    tenant_id                     = data.azurerm_client_config.current.tenant_id
  }

  dynamic "high_availability" {
    for_each = var.postgres_high_availability_enabled ? [1] : []
    content {
      mode = "ZoneRedundant"
    }
  }

  depends_on = [azurerm_private_dns_zone_virtual_network_link.postgres]
}

resource "azurerm_postgresql_flexible_server_active_directory_administrator" "platform" {
  server_name         = azurerm_postgresql_flexible_server.platform.name
  resource_group_name = azurerm_resource_group.platform.name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  object_id           = var.postgres_administrator_object_id
  principal_name      = var.postgres_administrator_login
  principal_type      = "Group"
}

resource "azurerm_api_management" "platform" {
  name                          = "apim-${var.name}"
  location                      = azurerm_resource_group.platform.location
  resource_group_name           = azurerm_resource_group.platform.name
  publisher_name                = var.apim_publisher_name
  publisher_email               = var.apim_publisher_email
  sku_name                      = var.apim_sku_name
  virtual_network_type          = "Internal"
  public_network_access_enabled = true
  client_certificate_enabled    = true
  min_api_version               = "2022-08-01"
  tags                          = var.tags

  identity {
    type = "SystemAssigned"
  }

  virtual_network_configuration {
    subnet_id = azurerm_subnet.api_management.id
  }

  protocols {
    http2_enabled = true
  }

  sign_in {
    enabled = true
  }

  security {
    backend_ssl30_enabled  = false
    backend_tls10_enabled  = false
    backend_tls11_enabled  = false
    frontend_ssl30_enabled = false
    frontend_tls10_enabled = false
    frontend_tls11_enabled = false
  }

  depends_on = [azurerm_subnet_network_security_group_association.api_management]

  lifecycle {
    ignore_changes = [public_network_access_enabled]
  }
}

# Azure rejects creating an API Management service with public_network_access_enabled = false
# directly (ActivateServiceWithPrivateEndpointAccessNotAllowed). Create with public access
# enabled, then disable it via azapi once the service exists.
# https://github.com/hashicorp/terraform-provider-azurerm/issues/31788
resource "azapi_update_resource" "apim_disable_public_network_access" {
  type        = "Microsoft.ApiManagement/service@2023-09-01-preview"
  resource_id = azurerm_api_management.platform.id

  body = {
    properties = {
      publicNetworkAccess = "Disabled"
    }
  }
}

resource "azurerm_cdn_frontdoor_profile" "platform" {
  name                = "afd-${var.name}"
  resource_group_name = azurerm_resource_group.platform.name
  sku_name            = "Premium_AzureFrontDoor"
  tags                = var.tags

  identity {
    type = "SystemAssigned"
  }
}

resource "azurerm_cdn_frontdoor_endpoint" "platform" {
  name                     = "fde-${var.name}"
  cdn_frontdoor_profile_id = azurerm_cdn_frontdoor_profile.platform.id
  enabled                  = var.edge_enabled
  tags                     = var.tags
}

resource "azurerm_cdn_frontdoor_firewall_policy" "platform" {
  name                = "waf${local.compact_name}"
  resource_group_name = azurerm_resource_group.platform.name
  sku_name            = azurerm_cdn_frontdoor_profile.platform.sku_name
  enabled             = true
  mode                = "Prevention"
  tags                = var.tags

  managed_rule {
    type    = "DefaultRuleSet"
    version = "2.1"
    action  = "Block"
  }

  managed_rule {
    type    = "Microsoft_BotManagerRuleSet"
    version = "1.1"
    action  = "Block"
  }
}

resource "azurerm_cdn_frontdoor_security_policy" "platform" {
  name                     = "security-${var.name}"
  cdn_frontdoor_profile_id = azurerm_cdn_frontdoor_profile.platform.id

  security_policies {
    firewall {
      cdn_frontdoor_firewall_policy_id = azurerm_cdn_frontdoor_firewall_policy.platform.id
      association {
        domain {
          cdn_frontdoor_domain_id = azurerm_cdn_frontdoor_endpoint.platform.id
        }
        patterns_to_match = ["/*"]
      }
    }
  }
}

resource "azurerm_cdn_frontdoor_origin_group" "apim" {
  name                     = "apim-${var.name}"
  cdn_frontdoor_profile_id = azurerm_cdn_frontdoor_profile.platform.id
  session_affinity_enabled = false

  health_probe {
    interval_in_seconds = 30
    path                = "/status-0123456789abcdef"
    protocol            = "Https"
    request_type        = "HEAD"
  }

  load_balancing {
    sample_size                        = 4
    successful_samples_required        = 3
    additional_latency_in_milliseconds = 50
  }
}

resource "azurerm_cdn_frontdoor_origin" "apim" {
  name                           = "apim-${var.name}"
  cdn_frontdoor_origin_group_id  = azurerm_cdn_frontdoor_origin_group.apim.id
  enabled                        = true
  certificate_name_check_enabled = true
  host_name                      = trimprefix(azurerm_api_management.platform.gateway_url, "https://")
  origin_host_header             = trimprefix(azurerm_api_management.platform.gateway_url, "https://")
  http_port                      = 80
  https_port                     = 443
  priority                       = 1
  weight                         = 1000

  private_link {
    location               = var.location
    private_link_target_id = azurerm_api_management.platform.id
    request_message        = "Front Door private origin for ${var.name}"
    target_type            = "managedApis"
  }
}

# APIM requires the client_secret field even for a public PKCE client. Repeating the public
# client ID satisfies the provider schema without creating or storing a confidential credential;
# token validation is enforced by policy rather than this developer-portal metadata.
resource "azurerm_api_management_openid_connect_provider" "entra" {
  name                = "entra-workforce-oidc"
  api_management_name = azurerm_api_management.platform.name
  resource_group_name = azurerm_resource_group.platform.name
  display_name        = "Microsoft Entra workforce OpenID Connect"
  client_id           = var.entra_client_id
  client_secret       = var.entra_client_id
  metadata_endpoint   = "https://login.microsoftonline.com/${var.entra_tenant_id}/v2.0/.well-known/openid-configuration"
}

resource "azurerm_api_management_api" "shell" {
  name                  = "modern-shell"
  resource_group_name   = azurerm_resource_group.platform.name
  api_management_name   = azurerm_api_management.platform.name
  revision              = "1"
  display_name          = "Modern web shell"
  path                  = "modern"
  protocols             = ["https"]
  service_url           = var.modern_shell_origin_url
  subscription_required = false

  openid_authentication {
    openid_provider_name         = azurerm_api_management_openid_connect_provider.entra.name
    bearer_token_sending_methods = ["authorizationHeader"]
  }
}

resource "azurerm_api_management_api_operation" "shell" {
  operation_id        = "modern-shell"
  api_name            = azurerm_api_management_api.shell.name
  api_management_name = azurerm_api_management.platform.name
  resource_group_name = azurerm_resource_group.platform.name
  display_name        = "Modern shell routes"
  method              = "GET"
  url_template        = "/*"
}

resource "azurerm_api_management_api" "identity" {
  name                  = "shell-identity"
  resource_group_name   = azurerm_resource_group.platform.name
  api_management_name   = azurerm_api_management.platform.name
  revision              = "1"
  display_name          = "Shell identity session"
  path                  = "auth"
  protocols             = ["https"]
  service_url           = var.identity_origin_url
  subscription_required = false

  openid_authentication {
    openid_provider_name         = azurerm_api_management_openid_connect_provider.entra.name
    bearer_token_sending_methods = ["authorizationHeader"]
  }
}

resource "azurerm_api_management_api_operation" "identity_session" {
  operation_id        = "identity-session"
  api_name            = azurerm_api_management_api.identity.name
  api_management_name = azurerm_api_management.platform.name
  resource_group_name = azurerm_resource_group.platform.name
  display_name        = "Read browser session"
  method              = "GET"
  url_template        = "/session"
}

resource "azurerm_api_management_api_operation" "identity_login" {
  operation_id        = "identity-login"
  api_name            = azurerm_api_management_api.identity.name
  api_management_name = azurerm_api_management.platform.name
  resource_group_name = azurerm_resource_group.platform.name
  display_name        = "Start browser login"
  method              = "GET"
  url_template        = "/login"
}

resource "azurerm_api_management_api_operation" "identity_logout" {
  operation_id        = "identity-logout"
  api_name            = azurerm_api_management_api.identity.name
  api_management_name = azurerm_api_management.platform.name
  resource_group_name = azurerm_resource_group.platform.name
  display_name        = "End browser session"
  method              = "POST"
  url_template        = "/logout"
}

resource "azurerm_cdn_frontdoor_route" "platform" {
  name                          = "strangler-${var.name}"
  cdn_frontdoor_endpoint_id     = azurerm_cdn_frontdoor_endpoint.platform.id
  cdn_frontdoor_origin_group_id = azurerm_cdn_frontdoor_origin_group.apim.id
  cdn_frontdoor_origin_ids      = [azurerm_cdn_frontdoor_origin.apim.id]
  enabled                       = var.edge_enabled
  forwarding_protocol           = "HttpsOnly"
  https_redirect_enabled        = true
  patterns_to_match             = ["/*"]
  supported_protocols           = ["Http", "Https"]
  link_to_default_domain        = true
}

resource "azurerm_api_management_api" "catalog" {
  name                  = "product-catalog-v1"
  resource_group_name   = azurerm_resource_group.platform.name
  api_management_name   = azurerm_api_management.platform.name
  revision              = "1"
  display_name          = "Product catalog"
  path                  = "api/catalog"
  protocols             = ["https"]
  service_url           = var.catalog_api_origin_url
  subscription_required = false

  openid_authentication {
    openid_provider_name         = azurerm_api_management_openid_connect_provider.entra.name
    bearer_token_sending_methods = ["authorizationHeader"]
  }
}

resource "azurerm_api_management_api_operation" "catalog_search" {
  operation_id        = "search-products"
  api_name            = azurerm_api_management_api.catalog.name
  api_management_name = azurerm_api_management.platform.name
  resource_group_name = azurerm_resource_group.platform.name
  display_name        = "Search products"
  method              = "GET"
  url_template        = "/v1/products"
}

resource "azurerm_api_management_api_policy" "catalog" {
  api_name            = azurerm_api_management_api.catalog.name
  api_management_name = azurerm_api_management.platform.name
  resource_group_name = azurerm_resource_group.platform.name
  xml_content = templatefile("${path.module}/policies/catalog-api.xml", {
    api_audience = var.entra_api_audience
    tenant_id    = var.entra_tenant_id
  })
}

resource "azurerm_api_management_api" "legacy" {
  name                  = "legacy-ofbiz"
  resource_group_name   = azurerm_resource_group.platform.name
  api_management_name   = azurerm_api_management.platform.name
  revision              = "1"
  display_name          = "Retained OFBiz routes"
  path                  = "catalog"
  protocols             = ["https"]
  service_url           = var.legacy_ofbiz_origin_url
  subscription_required = false

  openid_authentication {
    openid_provider_name         = azurerm_api_management_openid_connect_provider.entra.name
    bearer_token_sending_methods = ["authorizationHeader"]
  }
}

resource "azurerm_api_management_api_operation" "legacy_catalog" {
  operation_id        = "legacy-find-product"
  api_name            = azurerm_api_management_api.legacy.name
  api_management_name = azurerm_api_management.platform.name
  resource_group_name = azurerm_resource_group.platform.name
  display_name        = "Legacy product search"
  method              = "GET"
  url_template        = "/control/FindProduct"
}

resource "azurerm_api_management_api_policy" "legacy" {
  api_name            = azurerm_api_management_api.legacy.name
  api_management_name = azurerm_api_management.platform.name
  resource_group_name = azurerm_resource_group.platform.name
  xml_content = templatefile("${path.module}/policies/legacy-api.xml", {
    api_audience = var.entra_api_audience
    tenant_id    = var.entra_tenant_id
  })
}

data "azurerm_policy_definition" "allowed_locations" {
  display_name = "Allowed locations"
}

data "azurerm_policy_definition" "require_tag" {
  display_name = "Require a tag on resources"
}

resource "azurerm_resource_group_policy_assignment" "allowed_locations" {
  name                 = "allowed-locations"
  resource_group_id    = azurerm_resource_group.platform.id
  policy_definition_id = data.azurerm_policy_definition.allowed_locations.id
  description          = "Deny resources outside the regions approved for this environment."
  enforce              = true
  location             = var.location

  identity {
    type = "SystemAssigned"
  }

  parameters = jsonencode({
    listOfAllowedLocations = {
      value = sort(tolist(var.allowed_locations))
    }
  })
}

resource "azurerm_resource_group_policy_assignment" "require_tags" {
  for_each = toset([
    "application",
    "cost-center",
    "criticality",
    "data-classification",
    "environment",
    "managed-by",
    "owner",
    "service",
  ])

  name                 = "require-${each.key}"
  resource_group_id    = azurerm_resource_group.platform.id
  policy_definition_id = data.azurerm_policy_definition.require_tag.id
  description          = "Deny taggable resources that omit the ${each.key} governance tag."
  enforce              = true
  location             = var.location
  not_scopes           = [azurerm_subnet.private_endpoints.id]

  identity {
    type = "SystemAssigned"
  }

  parameters = jsonencode({
    tagName = {
      value = each.key
    }
  })
}

resource "azurerm_consumption_budget_resource_group" "platform" {
  name              = "budget-${var.name}"
  resource_group_id = azurerm_resource_group.platform.id
  amount            = var.budget_amount
  time_grain        = "Monthly"
  time_period { start_date = formatdate("YYYY-MM-01'T'00:00:00'Z'", timestamp()) }
  notification {
    enabled        = true
    threshold      = 80
    operator       = "GreaterThan"
    threshold_type = "Actual"
    contact_emails = var.budget_contact_emails
  }
  lifecycle { ignore_changes = [time_period] }
}

locals {
  diagnostic_targets = {
    api_management     = azurerm_api_management.platform.id
    app_configuration  = azurerm_app_configuration.platform.id
    container_apps     = azurerm_container_app_environment.platform.id
    container_registry = azurerm_container_registry.platform.id
    frontdoor          = azurerm_cdn_frontdoor_profile.platform.id
    frontdoor_waf      = azurerm_cdn_frontdoor_firewall_policy.platform.id
    key_vault          = azurerm_key_vault.platform.id
    network_security   = azurerm_network_security_group.api_management.id
    postgres           = azurerm_postgresql_flexible_server.platform.id
    service_bus        = azurerm_servicebus_namespace.platform.id
    virtual_network    = azurerm_virtual_network.platform.id
  }
}

resource "azurerm_monitor_diagnostic_setting" "platform" {
  for_each = local.diagnostic_targets

  name                       = "diag-${each.key}"
  target_resource_id         = each.value
  log_analytics_workspace_id = azurerm_log_analytics_workspace.platform.id

  enabled_log {
    category_group = "allLogs"
  }

  enabled_metric {
    category = "AllMetrics"
  }
}
