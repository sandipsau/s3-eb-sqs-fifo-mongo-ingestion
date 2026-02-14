package com.example.ingestion.adapters.mongo

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class MongoIndexes(private val template: ReactiveMongoTemplate) {
  private val log = LoggerFactory.getLogger(javaClass)

  @PostConstruct
  fun ensureIndexes() {
    // Unique index for idempotency
    val jobIndex = Index()
      .on("bucket", Sort.Direction.ASC)
      .on("key", Sort.Direction.ASC)
      .on("eTag", Sort.Direction.ASC)
      .unique()

    // Optional index for querying ingested docs
    val dataIndex = Index()
      .on("materialNo", Sort.Direction.ASC)
      .on("sourceBucket", Sort.Direction.ASC)
      .on("sourceKey", Sort.Direction.ASC)

    template.indexOps(IngestionJobDoc::class.java).ensureIndex(jobIndex)
      .then(template.indexOps(MaterialTaxNodeDoc::class.java).ensureIndex(dataIndex))
      .doOnSuccess { log.info("Mongo indexes ensured") }
      .doOnError { log.warn("Failed ensuring indexes", it) }
      .subscribe()
  }
}
