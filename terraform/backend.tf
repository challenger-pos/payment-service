terraform {
  backend "s3" {
    bucket = "tf-state-challenge-bucket"
    region = "us-east-2"
  }
}

