package com.example.ingestion.domain

data class IngestionEnvelope(
  val schemaVersion: String,
  val eventType: String,
  val sourceSystem: String,
  val producedAt: String,
  val region: String,
  val file: S3FilePointer,
  val trace: TraceInfo? = null,
)

data class S3FilePointer(
  val bucket: String,
  val key: String,
)

data class TraceInfo(
  val correlationId: String? = null,
  val account: String? = null,
)
