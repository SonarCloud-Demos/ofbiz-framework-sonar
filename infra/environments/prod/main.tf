terraform {
  required_version = "= 1.13.3"
  backend "azurerm" {
    key              = "prod/platform.tfstate"
    use_azuread_auth = true
  }
}

module "platform" {
  source             = "./.."
  environment        = "prod"
  subscription_id    = var.subscription_id
  tenant_id          = var.tenant_id
  vnet_cidr          = "10.40.0.0/20"
  alert_email        = var.alert_email
  monthly_budget     = var.monthly_budget
  sample_image       = var.sample_image
  shell_image        = var.shell_image
  legacy_origin_host = var.legacy_origin_host
  extra_tags         = var.required_tags
}

variable "subscription_id" { type = string }
variable "tenant_id" { type = string }
variable "alert_email" { type = string }
variable "monthly_budget" { type = number }
variable "sample_image" { type = string }
variable "shell_image" { type = string }
variable "legacy_origin_host" { type = string }
variable "required_tags" { type = map(string) }
