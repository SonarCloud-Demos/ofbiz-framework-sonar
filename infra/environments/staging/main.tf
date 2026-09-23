variable "subscription_id" { type = string }
variable "location" { type = string }
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

  name                                  = "ofbiz-staging"
  location                              = var.location
  allowed_locations                     = [var.location, "global"]
  log_retention_days                    = 90
  budget_amount                         = var.budget_amount
  budget_contact_emails                 = var.budget_contact_emails
  apim_publisher_name                   = var.apim_publisher_name
  apim_publisher_email                  = var.apim_publisher_email
  apim_sku_name                         = "Premium_1"
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
  postgres_sku_name                     = "GP_Standard_D2s_v3"
  postgres_backup_retention_days        = 14
  postgres_geo_redundant_backup_enabled = false
  postgres_high_availability_enabled    = true
  tags = {
    application         = "ofbiz-modernization"
    environment         = "staging"
    service             = "platform"
    owner               = var.owner
    cost-center         = var.cost_center
    data-classification = "confidential"
    managed-by          = "terraform"
    criticality         = "tier-2"
  }
}
