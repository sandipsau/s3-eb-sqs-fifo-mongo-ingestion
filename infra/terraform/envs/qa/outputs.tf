output "sqs_fifo_url" {
  value = module.pipeline.fifo_queue_url
}

output "event_rule_arn" {
  value = module.pipeline.event_rule_arn
}

output "bucket_name" {
  value = module.pipeline.bucket_name
}
output "bucket_name" { value = module.pipeline.bucket_name }
