variable "subscription_id" {
  description = "Azure subscription that owns the state resources."
  type        = string
}

variable "location" {
  description = "Approved Azure region for the state resources."
  type        = string
}

variable "resource_group_name" {
  description = "Resource group for the state resources."
  type        = string
}

variable "storage_account_name" {
  description = "Globally unique lowercase storage account name."
  type        = string
  validation {
    condition     = can(regex("^[a-z0-9]{3,24}$", var.storage_account_name))
    error_message = "The storage account name must contain 3-24 lowercase letters or digits."
  }
}

variable "ci_principal_id" {
  description = "Object ID of the OIDC-federated CI identity allowed to access state."
  type        = string
}

variable "private_endpoint_subnet_id" {
  description = "Landing-zone subnet dedicated to the state storage private endpoint."
  type        = string
}

variable "blob_private_dns_zone_id" {
  description = "Landing-zone privatelink.blob.core.windows.net private DNS zone ID."
  type        = string
}

variable "diagnostic_workspace_id" {
  description = "Existing protected Log Analytics workspace that receives state-storage diagnostics."
  type        = string
}

variable "tags" {
  description = "Governance tags required on state resources."
  type        = map(string)
}
