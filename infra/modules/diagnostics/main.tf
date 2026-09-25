variable "name" { type = string }
variable "workspace_id" { type = string }
variable "target_resource_ids" { type = map(string) }

resource "azurerm_monitor_diagnostic_setting" "this" {
  for_each                   = var.target_resource_ids
  name                       = "${var.name}-${each.key}-diagnostics"
  target_resource_id         = each.value
  log_analytics_workspace_id = var.workspace_id

  enabled_log { category_group = "allLogs" }
  enabled_metric { category = "AllMetrics" }
}

output "target_count" { value = length(azurerm_monitor_diagnostic_setting.this) }
