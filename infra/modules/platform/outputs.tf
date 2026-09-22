output "resource_group_id" {
  value       = azurerm_resource_group.platform.id
  description = "Platform resource group ID."
}

output "container_app_environment_id" {
  value       = azurerm_container_app_environment.platform.id
  description = "Private Container Apps environment ID."
}

output "container_registry_id" {
  value       = azurerm_container_registry.platform.id
  description = "Private container registry ID."
}

output "key_vault_id" {
  value       = azurerm_key_vault.platform.id
  description = "Platform Key Vault ID."
}

output "api_management_id" {
  value       = azurerm_api_management.platform.id
  description = "Internal API Management instance ID."
}

output "frontdoor_endpoint_id" {
  value       = azurerm_cdn_frontdoor_endpoint.platform.id
  description = "Disabled Front Door endpoint awaiting an approved private APIM origin."
}

output "postgres_server_id" {
  value       = azurerm_postgresql_flexible_server.platform.id
  description = "Private Microsoft Entra-only PostgreSQL server ID."
}
