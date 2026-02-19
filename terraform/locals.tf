locals {
  eks_state_key      = "v4/kubernetes/${var.environment}/terraform.tfstate"
  dynamodb_state_key = "v4/dynamodb-billing/${var.environment}/terraform.tfstate"
}
