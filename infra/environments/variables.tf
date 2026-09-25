variable "subscription_id" { type = string }
variable "tenant_id" { type = string }
variable "environment" {
  type = string
  validation {
    condition     = contains(["test", "stage", "prod"], var.environment)
    error_message = "environment must be test, stage, or prod"
  }
}
variable "location" {
  type    = string
  default = "westeurope"
}
variable "vnet_cidr" { type = string }
variable "alert_email" { type = string }
variable "monthly_budget" { type = number }
variable "sample_image" { type = string }
variable "shell_image" { type = string }
variable "legacy_origin_host" {
  type = string
  validation {
    condition     = !can(regex("^https?://", var.legacy_origin_host)) && length(var.legacy_origin_host) > 0
    error_message = "legacy_origin_host must be a hostname without a scheme"
  }
}
variable "extra_tags" {
  type    = map(string)
  default = {}
  validation {
    condition = alltrue([
      for key in ["owner", "cost-center", "data-classification", "criticality"] :
      length(trimspace(lookup(var.extra_tags, key, ""))) > 0
    ])
    error_message = "extra_tags must define owner, cost-center, data-classification, and criticality."
  }
}
locals {
  tags = merge({
    application         = "ofbiz-modern"
    environment         = var.environment
    owner               = var.extra_tags.owner
    cost-center         = var.extra_tags.cost-center
    managed-by          = "terraform"
    data-classification = var.extra_tags.data-classification
    criticality         = var.extra_tags.criticality
    repository          = "ofbiz-framework-sonar"
    service             = "platform"
    migration-phase     = "2"
  }, var.extra_tags)
}
