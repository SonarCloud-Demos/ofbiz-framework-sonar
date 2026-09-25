variable "environment" {
  description = "Deployment environment represented by this contract."
  type        = string

  validation {
    condition     = contains(["test", "stage", "prod"], var.environment)
    error_message = "Environment must be test, stage, or prod."
  }
}

variable "location" {
  description = "Azure region used by the reference environment."
  type        = string
  default     = "westeurope"

  validation {
    condition     = var.location == "westeurope"
    error_message = "Phase 2 permits only the approved westeurope reference region."
  }
}

variable "owner" {
  description = "Accountable team or group; no module may invent this value."
  type        = string

  validation {
    condition     = length(trimspace(var.owner)) > 0
    error_message = "Owner must be supplied."
  }
}

variable "cost_center" {
  description = "Approved cost-allocation identifier."
  type        = string

  validation {
    condition     = length(trimspace(var.cost_center)) > 0
    error_message = "Cost center must be supplied."
  }
}

variable "data_classification" {
  description = "Highest data classification allowed in the environment."
  type        = string

  validation {
    condition     = contains(["public", "internal", "confidential", "restricted"], var.data_classification)
    error_message = "Data classification must use an approved value."
  }
}

variable "criticality" {
  description = "Operational criticality used for policy and recovery controls."
  type        = string

  validation {
    condition     = contains(["low", "medium", "high", "critical"], var.criticality)
    error_message = "Criticality must use an approved value."
  }
}

variable "monthly_budget" {
  description = "Approved monthly budget in the billing currency."
  type        = number

  validation {
    condition     = var.monthly_budget > 0
    error_message = "Monthly budget must be greater than zero."
  }
}

variable "budget_alert_recipients" {
  description = "Recipients for actual and forecast budget alerts."
  type        = set(string)

  validation {
    condition     = length(var.budget_alert_recipients) > 0
    error_message = "At least one budget alert recipient is required."
  }
}

variable "rpo_minutes" {
  description = "Approved recovery point objective in minutes."
  type        = number

  validation {
    condition     = var.rpo_minutes > 0
    error_message = "RPO must be greater than zero."
  }
}

variable "rto_minutes" {
  description = "Approved recovery time objective in minutes."
  type        = number

  validation {
    condition     = var.rto_minutes > 0
    error_message = "RTO must be greater than zero."
  }
}
