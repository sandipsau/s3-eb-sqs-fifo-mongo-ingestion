#############################################
# MODULE: ingestion_pipeline (main.tf)
# PURPOSE:
#   S3 (Object Created) -> EventBridge Rule -> SQS FIFO (+ DLQ)
#   EventBridge uses InputTransformer to create a stable JSON envelope.
#
# NOTES (Production-grade defaults):
# - Creates the S3 bucket (encryption, public access block, ownership controls, optional versioning)
# - Creates FIFO queue + FIFO DLQ with redrive policy
# - Enables S3 -> EventBridge notifications
# - Creates EventBridge Rule + Target (SQS FIFO) with Input Transformer envelope
# - Adds least-privilege SQS Queue Policy for EventBridge SendMessage
#############################################

#############################################
# SECTION A — S3 BUCKET (CREATE + HARDEN)
#############################################

resource "aws_s3_bucket" "source" {
  bucket        = var.bucket_name
  force_destroy = var.force_destroy
  tags          = var.tags
}

# Block all public access (recommended)
resource "aws_s3_bucket_public_access_block" "source" {
  bucket = aws_s3_bucket.source.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Enforce bucket-owner ownership (prevents ACL-related issues)
resource "aws_s3_bucket_ownership_controls" "source" {
  bucket = aws_s3_bucket.source.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

# Default encryption (SSE-S3); replace with SSE-KMS if your org requires KMS keys.
resource "aws_s3_bucket_server_side_encryption_configuration" "source" {
  bucket = aws_s3_bucket.source.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Optional but strongly recommended for ingestion idempotency (bucket+key+versionId)
resource "aws_s3_bucket_versioning" "source" {
  bucket = aws_s3_bucket.source.id

  versioning_configuration {
    status = var.enable_versioning ? "Enabled" : "Suspended"
  }
}

#############################################
# SECTION B — S3 -> EVENTBRIDGE ENABLEMENT
#############################################

# This turns on S3 events to EventBridge for this bucket.
resource "aws_s3_bucket_notification" "eventbridge" {
  bucket      = aws_s3_bucket.source.id
  eventbridge = true

  # Helps avoid ordering issues on initial create
  depends_on = [
    aws_s3_bucket_public_access_block.source,
    aws_s3_bucket_ownership_controls.source,
    aws_s3_bucket_server_side_encryption_configuration.source
  ]
}

#############################################
# SECTION C — SQS FIFO + DLQ (RELIABLE DELIVERY)
#############################################

resource "aws_sqs_queue" "dlq" {
  name                        = "${var.name_prefix}-ingestion-dlq.fifo"
  fifo_queue                  = true
  content_based_deduplication = true

  # 14 days retention for investigation/replay
  message_retention_seconds = 1209600

  tags = var.tags
}

resource "aws_sqs_queue" "fifo" {
  name                        = "${var.name_prefix}-ingestion.fifo"
  fifo_queue                  = true
  content_based_deduplication = true

  # Long polling reduces empty receives/cost
  receive_wait_time_seconds = 20

  # 4 days retention
  message_retention_seconds = 345600

  # Must exceed max expected processing time in ingestion service
  visibility_timeout_seconds = var.visibility_timeout_seconds

  # Redrive to DLQ after N failed receives
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = var.max_receive_count
  })

  tags = var.tags
}

#############################################
# SECTION D — EVENTBRIDGE RULE (MATCH FILE ARRIVAL)
#############################################

resource "aws_cloudwatch_event_rule" "s3_object_created" {
  name        = "${var.name_prefix}-s3-object-created"
  description = "Route S3 Object Created to SQS FIFO with custom envelope"

  event_pattern = jsonencode({
    "source"      : ["aws.s3"],
    "detail-type" : ["Object Created"],
    "detail" : {
      "bucket" : { "name" : [aws_s3_bucket.source.bucket] },
      "object" : { "key" : [{ "prefix" : var.key_prefix }] }
    }
  })

  tags = var.tags
}

#############################################
# SECTION E — EVENTBRIDGE TARGET (SQS FIFO + ENVELOPE)
#############################################

resource "aws_cloudwatch_event_target" "to_sqs" {
  rule = aws_cloudwatch_event_rule.s3_object_created.name
  arn  = aws_sqs_queue.fifo.arn

  # FIFO ordering lane (OK to be static for 1–2 files/day)
  sqs_target {
    message_group_id = var.message_group_id
  }

  # EventBridge InputTransformer: creates the SQS message body envelope
  input_transformer {
    input_paths = {
      eventId    = "$.id"
      time       = "$.time"
      region     = "$.region"
      account    = "$.account"
      bucket     = "$.detail.bucket.name"
      key        = "$.detail.object.key"
      detailType = "$.detail-type"
    }

    # Important note:
    # - Using producedAt/time in the body makes content-based dedup less effective.
    # - That's OK because the ingestion service should be idempotent (bucket+key+etag or versionId).
    input_template = jsonencode({
      schemaVersion = "1.0"
      eventType     = "<detailType>"
      sourceSystem  = var.source_system
      producedAt    = "<time>"
      region        = "<region>"
      file = {
        bucket = "<bucket>"
        key    = "<key>"
      }
      trace = {
        correlationId = "<eventId>"
        account       = "<account>"
      }
    })
  }
}

#############################################
# SECTION F — PERMISSIONS (ALLOW EVENTBRIDGE -> SQS)
#############################################

data "aws_iam_policy_document" "allow_eventbridge_send" {
  statement {
    sid    = "AllowEventBridgeSendMessage"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }

    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.fifo.arn]

    # Limit permission to only this rule as the source.
    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_cloudwatch_event_rule.s3_object_created.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "fifo_policy" {
  queue_url = aws_sqs_queue.fifo.id
  policy    = data.aws_iam_policy_document.allow_eventbridge_send.json
}

#############################################
# SECTION G — OPTIONAL OUTPUTS (HANDY)
#############################################

output "bucket_name" {
  value = aws_s3_bucket.source.bucket
}

output "fifo_queue_url" {
  value = aws_sqs_queue.fifo.id
}

output "fifo_queue_arn" {
  value = aws_sqs_queue.fifo.arn
}

output "dlq_queue_arn" {
  value = aws_sqs_queue.dlq.arn
}

output "event_rule_arn" {
  value = aws_cloudwatch_event_rule.s3_object_created.arn
}
