variable "subscription_id" { type = string }
variable "location" { type = string }
variable "environment_name" {
  type = string
  validation {
    condition     = can(regex("^eph-[0-9]+$", var.environment_name))
    error_message = "The ephemeral environment name must use the eph-<run-id> format."
  }
}
variable "expiry" {
  type = string
  validation {
    condition     = can(regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$", var.expiry))
    error_message = "Expiry must be an ISO date in YYYY-MM-DD format."
  }
}
variable "owner" { type = string }
variable "cost_center" { type = string }
variable "budget_amount" { type = number }
variable "budget_contact_emails" { type = list(string) }
variable "apim_publisher_name" { type = string }
variable "apim_publisher_email" { type = string }
variable "postgres_administrator_login" { type = string }
variable "postgres_administrator_object_id" { type = string }
variable "entra_tenant_id" { type = string }
variable "entra_client_id" { type = string }
variable "entra_api_audience" { type = string }
variable "modern_shell_origin_url" { type = string }
variable "identity_origin_url" { type = string }
variable "catalog_api_origin_url" { type = string }
variable "legacy_ofbiz_origin_url" { type = string }
variable "edge_enabled" {
  type    = bool
  default = false
}
variable "catalog_pilot_enabled" {
  type    = bool
  default = false
}
variable "catalog_pilot_subject_ids" {
  type    = set(string)
  default = []
}

module "platform" {
  source = "../../modules/platform"

  name                                  = var.environment_name
  location                              = var.location
  allowed_locations                     = [var.location, "global"]
  log_retention_days                    = 30
  budget_amount                         = var.budget_amount
  budget_contact_emails                 = var.budget_contact_emails
  apim_publisher_name                   = var.apim_publisher_name
  apim_publisher_email                  = var.apim_publisher_email
  postgres_administrator_login          = var.postgres_administrator_login
  postgres_administrator_object_id      = var.postgres_administrator_object_id
  entra_tenant_id                       = var.entra_tenant_id
  entra_client_id                       = var.entra_client_id
  entra_api_audience                    = var.entra_api_audience
  modern_shell_origin_url               = var.modern_shell_origin_url
  identity_origin_url                   = var.identity_origin_url
  catalog_api_origin_url                = var.catalog_api_origin_url
  legacy_ofbiz_origin_url               = var.legacy_ofbiz_origin_url
  edge_enabled                          = var.edge_enabled
  catalog_pilot_enabled                 = var.catalog_pilot_enabled
  catalog_pilot_subject_ids             = var.catalog_pilot_subject_ids
  apim_sku_name                         = "Developer_1"
  postgres_sku_name                     = "B_Standard_B1ms"
  postgres_backup_retention_days        = 7
  postgres_geo_redundant_backup_enabled = false
  postgres_high_availability_enabled    = false
  tags = {
    application         = "ofbiz-modernization"
    environment         = "ephemeral"
    service             = "platform"
    owner               = var.owner
    cost-center         = var.cost_center
    data-classification = "internal"
    managed-by          = "terraform"
    criticality         = "tier-4"
    expiry              = var.expiry
  }
}

output "resource_group_id" {
  description = "Resource group used by the ephemeral smoke and destroy checks."
  value       = module.platform.resource_group_id
}
