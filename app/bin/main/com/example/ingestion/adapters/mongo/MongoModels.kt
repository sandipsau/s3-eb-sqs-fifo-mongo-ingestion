package com.example.ingestion.adapters.mongo

import com.example.ingestion.domain.JobStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("ingestion_jobs")
data class IngestionJobDoc(
  @Id val id: String? = null,
  val bucket: String,
  val key: String,
  val eTag: String?,
  val status: JobStatus,
  val startedAt: Instant,
  val finishedAt: Instant? = null,
  val error: String? = null
)

@Document("material_tax_nodes")
data class MaterialTaxNodeDoc(
  @Id val id: String? = null,
  val materialNo: String,
  val taxNodeId: String,
  val sourceBucket: String,
  val sourceKey: String,
  val ingestedAt: Instant
)
