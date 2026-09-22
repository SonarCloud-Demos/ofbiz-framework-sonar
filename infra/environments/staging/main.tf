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
