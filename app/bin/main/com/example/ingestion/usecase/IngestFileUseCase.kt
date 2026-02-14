package com.example.ingestion.usecase

import com.example.ingestion.domain.JobKey
import com.example.ingestion.ports.JobRepositoryPort
import com.example.ingestion.ports.MaterialTaxNodeRepositoryPort
import com.example.ingestion.ports.ObjectStoragePort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class IngestFileUseCase(
  private val storage: ObjectStoragePort,
  private val parser: DelimitedFileParser,
  private val materialRepo: MaterialTaxNodeRepositoryPort,
  private val jobs: JobRepositoryPort,
  @Value("\${ingestion.parser.batchSize}") private val batchSize: Int,
) {
  private val log = LoggerFactory.getLogger(javaClass)

  fun ingest(bucket: String, key: String, correlationId: String?): Mono<Void> {
    return storage.head(bucket, key)
      .flatMap { meta ->
        val jobKey = JobKey(bucket, key, meta.eTag)
        jobs.tryClaim(jobKey).flatMap { claimed ->
          if (!claimed) {
            log.info("Skip already-processed file bucket={} key={} eTag={} corrId={}", bucket, key, meta.eTag, correlationId)
            Mono.empty()
          } else {
            log.info("Start ingest bucket={} key={} eTag={} size={} corrId={}", bucket, key, meta.eTag, meta.sizeBytes, correlationId)
            parser.parse(storage.readLines(bucket, key))
              .buffer(batchSize)
              .concatMap { batch -> materialRepo.bulkUpsert(bucket, key, batch) }
              .then(jobs.markDone(jobKey))
              .doOnSuccess { log.info("Done ingest bucket={} key={} eTag={} corrId={}", bucket, key, meta.eTag, correlationId) }
              .onErrorResume { e ->
                log.error("Failed ingest bucket={} key={} eTag={} corrId={}", bucket, key, meta.eTag, correlationId, e)
                jobs.markFailed(jobKey, e.message ?: "unknown error")
                  .then(Mono.error(e))
              }
          }
        }
      }
  }
}
