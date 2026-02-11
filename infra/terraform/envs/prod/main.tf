terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

module "pipeline" {
  source = "../../modules/ingestion_pipeline"

  name_prefix      = var.name_prefix
  region           = var.region
  bucket_name      = var.bucket_name
  key_prefix       = var.key_prefix
  source_system    = var.source_system
  message_group_id = var.message_group_id
  force_destroy     = var.force_destroy
  enable_versioning = var.enable_versioning

  tags = var.tags

}
