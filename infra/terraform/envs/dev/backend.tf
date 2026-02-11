# Configure remote state for your environment (example: S3 backend).
# Replace placeholders to match your org standards.

# terraform {
#   backend "s3" {
#     bucket = "my-terraform-state-bucket"
#     key    = "ingestion/<env>/terraform.tfstate"
#     region = "us-east-2"
#     dynamodb_table = "my-terraform-locks"
#     encrypt = true
#   }
# }
