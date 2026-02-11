resource "aws_sqs_queue" "dlq" {
  name                         = "${var.name_prefix}-ingestion-dlq.fifo"
  fifo_queue                   = true
  content_based_deduplication  = true
  message_retention_seconds    = 1209600
  tags                         = var.tags
}

resource "aws_sqs_queue" "fifo" {
  name                        = "${var.name_prefix}-ingestion.fifo"
  fifo_queue                  = true
  content_based_deduplication = true

  receive_wait_time_seconds   = 20
  message_retention_seconds   = 345600
  visibility_timeout_seconds  = var.visibility_timeout_seconds

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = var.max_receive_count
  })

  tags = var.tags
}

resource "aws_s3_bucket_notification" "eventbridge" {
  bucket      = var.bucket_name
  eventbridge = true
}

resource "aws_cloudwatch_event_rule" "s3_object_created" {
  name        = "${var.name_prefix}-s3-object-created"
  description = "Route S3 Object Created to SQS FIFO with custom envelope"

  event_pattern = jsonencode({
    "source": ["aws.s3"],
    "detail-type": ["Object Created"],
    "detail": {
      "bucket": { "name": [var.bucket_name] },
      "object": { "key": [{ "prefix": var.key_prefix }] }
    }
  })

  tags = var.tags
}

resource "aws_cloudwatch_event_target" "to_sqs" {
  rule = aws_cloudwatch_event_rule.s3_object_created.name
  arn  = aws_sqs_queue.fifo.arn

  sqs_target {
    message_group_id = var.message_group_id
  }

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
