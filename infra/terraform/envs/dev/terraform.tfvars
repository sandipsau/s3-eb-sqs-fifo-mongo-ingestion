region = "us-east-2"
name_prefix = "dev"
bucket_name = "REPLACE_ME_BUCKET"
key_prefix = "incoming/"
source_system = "order-calc-services"
message_group_id = "order-calc-services"
tags = {
  environment = "dev"
  service     = "ingestion"
}
