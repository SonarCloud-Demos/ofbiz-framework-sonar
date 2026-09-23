output "backend" {
  description = "Non-sensitive coordinates used to configure environment backends."
  value = {
    resource_group_name  = azurerm_resource_group.state.name
    storage_account_name = azurerm_storage_account.state.name
    container_name       = azurerm_storage_container.state.name
  }
}

output "private_endpoint_id" {
  description = "Private endpoint used for Terraform state blob access."
  value       = azurerm_private_endpoint.state.id
}
