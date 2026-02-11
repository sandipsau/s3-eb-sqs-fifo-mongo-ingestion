package com.example.ingestion.domain

import java.time.Instant

enum class JobStatus { PROCESSING, DONE, FAILED }

data class JobKey(
  val bucket: String,
  val key: String,
  val eTag: String?,
)

data class IngestionJobRecord(
  val id: String? = null,
  val bucket: String,
  val key: String,
  val eTag: String?,
  val status: JobStatus,
  val startedAt: Instant,
  val finishedAt: Instant? = null,
  val error: String? = null,
)
