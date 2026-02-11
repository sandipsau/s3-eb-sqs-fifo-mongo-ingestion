package com.example.ingestion.ports

import com.example.ingestion.domain.MaterialTaxNode
import reactor.core.publisher.Mono

interface MaterialTaxNodeRepositoryPort {
  fun bulkUpsert(
    bucket: String,
    key: String,
    batch: List<MaterialTaxNode>,
  ): Mono<Void>
}
