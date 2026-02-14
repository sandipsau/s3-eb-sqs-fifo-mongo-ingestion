package com.example.ingestion.adapters.mongo

import com.example.ingestion.domain.MaterialTaxNode
import com.example.ingestion.ports.MaterialTaxNodeRepositoryPort
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Instant

@Component
class MongoMaterialTaxNodeRepository(
  private val template: ReactiveMongoTemplate
) : MaterialTaxNodeRepositoryPort {

  override fun bulkUpsert(bucket: String, key: String, batch: List<MaterialTaxNode>): Mono<Void> {
    if (batch.isEmpty()) return Mono.empty()

    val ops = template.bulkOps(BulkOperations.BulkMode.UNORDERED, MaterialTaxNodeDoc::class.java)
    val now = Instant.now()

    batch.forEach { row ->
      val q = Query.query(
        Criteria.where("materialNo").`is`(row.materialNo)
          .and("sourceBucket").`is`(bucket)
          .and("sourceKey").`is`(key)
      )
      val u = Update()
        .set("workstationTaxNodeId", row.taxNodeId)
        .set("ingestedAt", now)
        .setOnInsert("materialNo", row.materialNo)
        .setOnInsert("sourceBucket", bucket)
        .setOnInsert("sourceKey", key)

      ops.upsert(q, u)
    }

    return ops.execute().then()
  }
}
