terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.20"
    }
  }
}

# AWS Provider
provider "aws" {
  region = var.region
}

# Kubernetes Provider (conecta no EKS)
data "aws_eks_cluster" "cluster" {
  name = data.terraform_remote_state.eks.outputs.cluster_name
}

data "aws_eks_cluster_auth" "cluster" {
  name = data.terraform_remote_state.eks.outputs.cluster_name
}

provider "kubernetes" {
  host                   = data.aws_eks_cluster.cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.cluster.token
}

# Remote States
data "terraform_remote_state" "eks" {
  backend = "s3"
  config = {
    bucket = "tf-state-challenge-bucket"
    key    = "kubernetes/${var.environment}/terraform.tfstate"
    region = "us-east-2"
  }
}

data "terraform_remote_state" "rds_billing" {
  backend = "s3"
  config = {
    bucket = "tf-state-challenge-bucket"
    key    = "rds-billing/${var.environment}/terraform.tfstate"
    region = "us-east-2"
  }
}
