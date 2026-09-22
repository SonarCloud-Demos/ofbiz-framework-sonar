terraform {
  required_version = "= 1.13.3"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "= 4.46.0"
    }
  }
}

provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
}
