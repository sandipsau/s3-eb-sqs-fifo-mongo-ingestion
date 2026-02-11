output "sqs_fifo_url" { value = aws_sqs_queue.fifo.id }
output "sqs_fifo_arn" { value = aws_sqs_queue.fifo.arn }
output "sqs_dlq_arn"  { value = aws_sqs_queue.dlq.arn }
output "event_rule_arn" { value = aws_cloudwatch_event_rule.s3_object_created.arn }
