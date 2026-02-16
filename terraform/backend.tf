terraform {
  backend "s3" {
    bucket         = "tf-state-challenge-bucket"
    key            = "v4/service-billing/homologation/terraform.tfstate"
    region         = "us-east-2"
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}
