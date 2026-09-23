variable "name" {
  description = "Short platform and environment name used in resource names."
  type        = string
}

variable "location" {
  description = "Approved primary Azure region."
  type        = string
}

variable "log_retention_days" {
  description = "Log Analytics retention required by the environment policy."
  type        = number
  validation {
    condition     = var.log_retention_days >= 30 && var.log_retention_days <= 730
    error_message = "Log retention must be between 30 and 730 days."
  }
}

variable "allowed_locations" {
  description = "Azure regions permitted by policy, including global when required by a selected service."
  type        = set(string)
  validation {
    condition     = length(var.allowed_locations) > 0
    error_message = "At least one allowed Azure location is required."
  }
}

variable "tags" {
  description = "Required governance tags."
  type        = map(string)
  validation {
    condition = alltrue([
      for key in ["application", "environment", "service", "owner", "cost-center", "data-classification", "managed-by", "criticality"] :
      contains(keys(var.tags), key)
    ])
    error_message = "All required governance tags must be supplied."
  }
}

variable "budget_amount" {
  description = "Monthly environment budget in the billing currency."
  type        = number
  validation {
    condition     = var.budget_amount > 0
    error_message = "Budget amount must be greater than zero."
  }
}

variable "budget_contact_emails" {
  description = "Approved distribution addresses for budget notifications."
  type        = list(string)
  validation {
    condition     = length(var.budget_contact_emails) > 0
    error_message = "At least one budget notification address is required."
  }
}

variable "apim_publisher_name" {
  description = "Organization name shown in API Management administrative messages."
  type        = string
}

variable "apim_publisher_email" {
  description = "Approved distribution address for API Management notifications."
  type        = string
  validation {
    condition     = can(regex("^[^@[:space:]]+@[^@[:space:]]+\\.[^@[:space:]]+$", var.apim_publisher_email))
    error_message = "The API Management publisher email must be a valid address."
  }
}

variable "postgres_administrator_login" {
  description = "Microsoft Entra login name for the PostgreSQL administrator."
  type        = string
}

variable "postgres_administrator_object_id" {
  description = "Microsoft Entra object ID for the PostgreSQL administrator."
  type        = string
}

variable "apim_sku_name" {
  description = "Approved API Management SKU and capacity."
  type        = string
  validation {
    condition     = contains(["Developer_1", "Premium_1"], var.apim_sku_name)
    error_message = "APIM must use Developer_1 or Premium_1."
  }
}

variable "postgres_sku_name" {
  description = "Approved PostgreSQL Flexible Server SKU."
  type        = string
}

variable "postgres_backup_retention_days" {
  description = "PostgreSQL point-in-time backup retention."
  type        = number
  validation {
    condition     = var.postgres_backup_retention_days >= 7 && var.postgres_backup_retention_days <= 35
    error_message = "PostgreSQL backup retention must be between 7 and 35 days."
  }
}

variable "postgres_geo_redundant_backup_enabled" {
  description = "Whether PostgreSQL backups are geo-redundant."
  type        = bool
}

variable "postgres_high_availability_enabled" {
  description = "Whether PostgreSQL uses zone-redundant high availability."
  type        = bool
}

variable "edge_enabled" {
  description = "Whether the approved Phase 4 Front Door endpoint and routes accept traffic."
  type        = bool
  default     = false
}

variable "entra_tenant_id" {
  description = "Microsoft Entra tenant that issues workforce access tokens."
  type        = string
  validation {
    condition     = can(regex("^[0-9a-fA-F-]{36}$", var.entra_tenant_id))
    error_message = "The Entra tenant ID must be a UUID."
  }
}

variable "entra_api_audience" {
  description = "Application ID URI expected in access-token audiences."
  type        = string
  validation {
    condition     = startswith(var.entra_api_audience, "api://")
    error_message = "The Entra API audience must be an api:// application ID URI."
  }
}

variable "entra_client_id" {
  description = "Public client identifier used by the modern browser application."
  type        = string
  validation {
    condition     = can(regex("^[0-9a-fA-F-]{36}$", var.entra_client_id))
    error_message = "The Entra client ID must be a UUID."
  }
}

variable "modern_shell_origin_url" {
  description = "Private HTTPS origin including the /modern base path for the identity-aware modern shell."
  type        = string
  validation {
    condition     = startswith(var.modern_shell_origin_url, "https://")
    error_message = "The modern shell origin must use HTTPS."
  }
}

variable "identity_origin_url" {
  description = "Private HTTPS origin including the /auth base path for the shell OIDC session handler."
  type        = string
  validation {
    condition     = startswith(var.identity_origin_url, "https://")
    error_message = "The identity origin must use HTTPS."
  }
}

variable "catalog_api_origin_url" {
  description = "Private HTTPS origin including the /api/catalog base path for the product catalog API."
  type        = string
  validation {
    condition     = startswith(var.catalog_api_origin_url, "https://")
    error_message = "The catalog API origin must use HTTPS."
  }
}

variable "legacy_ofbiz_origin_url" {
  description = "Private HTTPS origin including the /catalog base path for retained OFBiz routes."
  type        = string
  validation {
    condition     = startswith(var.legacy_ofbiz_origin_url, "https://")
    error_message = "The legacy OFBiz origin must use HTTPS."
  }
}
