run "accepted_stage_contract" {
  command = plan

  variables {
    environment             = "stage"
    owner                   = "platform-team"
    cost_center             = "replace-before-apply"
    data_classification     = "internal"
    criticality             = "medium"
    monthly_budget          = 1
    budget_alert_recipients = ["replace-before-apply@example.invalid"]
    rpo_minutes             = 60
    rto_minutes             = 120
  }

  assert {
    condition     = output.contract.location == "westeurope"
    error_message = "The reference contract must default to westeurope."
  }

  assert {
    condition     = output.contract.required_tags["managed-by"] == "terraform"
    error_message = "The environment contract must identify Terraform ownership."
  }

  assert {
    condition     = output.contract.budget_thresholds == [50, 80, 100]
    error_message = "Actual budget thresholds must match the approved model."
  }
}

run "rejects_unknown_environment" {
  command = plan

  variables {
    environment             = "development"
    owner                   = "platform-team"
    cost_center             = "replace-before-apply"
    data_classification     = "internal"
    criticality             = "medium"
    monthly_budget          = 1
    budget_alert_recipients = ["replace-before-apply@example.invalid"]
    rpo_minutes             = 60
    rto_minutes             = 120
  }

  expect_failures = [var.environment]
}
