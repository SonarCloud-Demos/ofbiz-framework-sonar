locals {
  application         = "ofbiz-modern"
  region_abbreviation = "weu"
  required_tags = {
    application         = local.application
    environment         = var.environment
    owner               = var.owner
    cost-center         = var.cost_center
    managed-by          = "terraform"
    data-classification = var.data_classification
    criticality         = var.criticality
    repository          = "ofbiz-framework-sonar"
    service             = "platform"
    migration-phase     = "2"
  }
  budget_thresholds = [50, 80, 100]
}

output "contract" {
  description = "Validated inputs consumed by future provider-backed environment roots."
  value = {
    application             = local.application
    environment             = var.environment
    location                = var.location
    region_abbreviation     = local.region_abbreviation
    required_tags           = local.required_tags
    monthly_budget          = var.monthly_budget
    budget_alert_recipients = var.budget_alert_recipients
    budget_thresholds       = local.budget_thresholds
    forecast_threshold      = 100
    rpo_minutes             = var.rpo_minutes
    rto_minutes             = var.rto_minutes
  }
}
