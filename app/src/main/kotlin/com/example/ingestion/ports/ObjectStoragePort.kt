package com.example.ingestion.ports

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class ObjectMetadata(
  val eTag: String?,
  val versionId: String?,
  val sizeBytes: Long?,
)

interface ObjectStoragePort {
  fun head(bucket: String, key: String): Mono<ObjectMetadata>
  fun readLines(bucket: String, key: String): Flux<String>
}
