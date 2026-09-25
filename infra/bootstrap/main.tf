variable "subscription_id" { type = string }
variable "tenant_id" { type = string }
variable "storage_account_name" { type = string }
variable "owner" { type = string }
variable "cost_center" { type = string }

locals {
  tags = {
    application         = "ofbiz-modern"
    environment         = "bootstrap"
    owner               = var.owner
    cost-center         = var.cost_center
    managed-by          = "terraform"
    data-classification = "confidential"
    criticality         = "high"
    repository          = "ofbiz-framework-sonar"
    service             = "terraform-state"
    migration-phase     = "2"
  }
}

resource "azurerm_resource_group" "state" {
  name     = "rg-ofbiz-modern-state-weu"
  location = "westeurope"
  tags     = local.tags
}

resource "azurerm_storage_account" "state" {
  name                              = var.storage_account_name
  resource_group_name               = azurerm_resource_group.state.name
  location                          = azurerm_resource_group.state.location
  account_tier                      = "Standard"
  account_replication_type          = "ZRS"
  min_tls_version                   = "TLS1_2"
  shared_access_key_enabled         = false
  public_network_access_enabled     = false
  infrastructure_encryption_enabled = true
  blob_properties {
    versioning_enabled = true
    delete_retention_policy { days = 30 }
    container_delete_retention_policy { days = 30 }
  }
  identity { type = "SystemAssigned" }
  tags = local.tags
}

resource "azurerm_storage_container" "state" {
  name                  = "terraform-state"
  storage_account_id    = azurerm_storage_account.state.id
  container_access_type = "private"
}

resource "azurerm_management_lock" "state" {
  name       = "protect-terraform-state"
  scope      = azurerm_storage_account.state.id
  lock_level = "CanNotDelete"
}
