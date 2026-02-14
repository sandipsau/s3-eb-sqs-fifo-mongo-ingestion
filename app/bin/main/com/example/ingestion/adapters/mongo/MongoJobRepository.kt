package com.example.ingestion.adapters.mongo

import com.example.ingestion.domain.JobKey
import com.example.ingestion.domain.JobStatus
import com.example.ingestion.ports.JobRepositoryPort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Instant

@Component
class MongoJobRepository(
  private val template: ReactiveMongoTemplate
) : JobRepositoryPort {

  override fun tryClaim(key: JobKey): Mono<Boolean> {
    val q = Query.query(
      Criteria.where("bucket").`is`(key.bucket)
        .and("key").`is`(key.key)
        .and("eTag").`is`(key.eTag)
    )

    val u = Update()
      .setOnInsert("bucket", key.bucket)
      .setOnInsert("key", key.key)
      .setOnInsert("eTag", key.eTag)
      .setOnInsert("status", JobStatus.PROCESSING)
      .setOnInsert("startedAt", Instant.now())

    return template.upsert(q, u, IngestionJobDoc::class.java)
      .map { it.upsertedId != null } // true if inserted (claimed)
  }

  override fun markDone(key: JobKey): Mono<Void> {
    val q = Query.query(
      Criteria.where("bucket").`is`(key.bucket)
        .and("key").`is`(key.key)
        .and("eTag").`is`(key.eTag)
    )
    val u = Update()
      .set("status", JobStatus.DONE)
      .set("finishedAt", Instant.now())
      .unset("error")
    return template.updateFirst(q, u, IngestionJobDoc::class.java).then()
  }

  override fun markFailed(key: JobKey, error: String): Mono<Void> {
    val q = Query.query(
      Criteria.where("bucket").`is`(key.bucket)
        .and("key").`is`(key.key)
        .and("eTag").`is`(key.eTag)
    )
    val u = Update()
      .set("status", JobStatus.FAILED)
      .set("finishedAt", Instant.now())
      .set("error", error.take(2000))
    return template.updateFirst(q, u, IngestionJobDoc::class.java).then()
  }
}
