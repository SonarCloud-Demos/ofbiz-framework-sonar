package ofbiz.terraform.phase2

# CI evaluates Terraform and scanner output against this deny contract. Keep
# production disabled until the reliability/security review is recorded.
deny contains message if {
  input.environment == "prod"
  not input.production_gate_approved
  message := "production apply is gated"
}

deny contains message if {
  input.long_lived_client_secret
  message := "long-lived CI client secrets are forbidden"
}

deny contains message if {
  input.location != "westeurope"
  message := "only the approved westeurope region is allowed"
}

deny contains message if {
  input.public_stateful_endpoint
  message := "stateful data planes must not be public"
}

deny contains message if {
  not input.managed_identity_enabled
  message := "workloads must use managed identity"
}

deny contains message if {
  not input.diagnostics_complete
  message := "supported resources must export diagnostics"
}

deny contains message if {
  input.environment == "prod"
  not input.stateful_deletion_locks
  message := "production stateful resources require deletion locks"
}

deny contains message if {
  count(input.missing_required_tags) > 0
  message := "all resources must carry the required governance tags"
}
