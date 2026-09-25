module "resource_group" {
  source   = "../modules/resource-group"
  name     = "ofbiz-${var.environment}-rg"
  location = var.location
  tags     = local.tags
}
module "network" {
  source              = "../modules/network"
  name                = "ofbiz-${var.environment}-vnet"
  resource_group_name = module.resource_group.name
  location            = module.resource_group.location
  address_space       = [var.vnet_cidr]
  tags                = local.tags
}
module "observability" {
  source              = "../modules/observability"
  name                = "ofbiz-${var.environment}"
  resource_group_name = module.resource_group.name
  location            = module.resource_group.location
  alert_email         = var.alert_email
  tags                = local.tags
}
module "data_platform" {
  source                     = "../modules/data-platform"
  name                       = "ofbiz-${var.environment}"
  resource_group_name        = module.resource_group.name
  location                   = module.resource_group.location
  private_endpoint_subnet_id = module.network.private_endpoint_subnet_id
  private_dns_zone_ids       = module.network.private_dns_zone_ids
  tenant_id                  = var.tenant_id
  tags                       = local.tags
}
module "container_platform" {
  source                     = "../modules/container-platform"
  name                       = "ofbiz-${var.environment}"
  resource_group_name        = module.resource_group.name
  location                   = module.resource_group.location
  apps_subnet_id             = module.network.apps_subnet_id
  private_endpoint_subnet_id = module.network.private_endpoint_subnet_id
  acr_private_dns_zone_id    = module.network.private_dns_zone_ids["privatelink.azurecr.io"]
  workspace_id               = module.observability.workspace_id
  sample_image               = var.sample_image
  shell_image                = var.shell_image
  tags                       = local.tags
}
module "edge" {
  source                   = "../modules/edge"
  name                     = "ofbiz-${var.environment}"
  resource_group_name      = module.resource_group.name
  location                 = module.resource_group.location
  monthly_budget           = var.monthly_budget
  alert_email              = var.alert_email
  apim_subnet_id           = module.network.apim_subnet_id
  container_environment_id = module.container_platform.environment_id
  shell_origin_host        = module.container_platform.shell_fqdn
  legacy_origin_host       = var.legacy_origin_host
  tags                     = local.tags
}
module "platform_alerts" {
  source                  = "../modules/platform-alerts"
  name                    = "ofbiz-${var.environment}"
  resource_group_name     = module.resource_group.name
  sample_app_id           = module.container_platform.sample_app_id
  shell_app_id            = module.container_platform.shell_app_id
  postgres_server_id      = module.data_platform.postgres_server_id
  servicebus_namespace_id = module.data_platform.servicebus_namespace_id
  action_group_id         = module.observability.action_group_id
  tags                    = local.tags
}

locals {
  diagnostic_targets = merge(module.data_platform.stateful_resource_ids, {
    api_management        = module.edge.apim_id
    apim_nsg              = module.network.apim_nsg_id
    container_environment = module.container_platform.environment_id
    container_registry    = module.container_platform.registry_id
    front_door            = module.edge.frontdoor_profile_id
    platform_sample       = module.container_platform.sample_app_id
    shell_bff             = module.container_platform.shell_app_id
    virtual_network       = module.network.vnet_id
  })
}

module "diagnostics" {
  source              = "../modules/diagnostics"
  name                = "ofbiz-${var.environment}"
  workspace_id        = module.observability.workspace_id
  target_resource_ids = local.diagnostic_targets
}

resource "azurerm_management_lock" "production_stateful" {
  for_each   = var.environment == "prod" ? module.data_platform.stateful_resource_ids : {}
  name       = "protect-${replace(each.key, "_", "-")}"
  scope      = each.value
  lock_level = "CanNotDelete"
}
