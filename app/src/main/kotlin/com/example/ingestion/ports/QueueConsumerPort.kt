package com.example.ingestion.ports

import reactor.core.publisher.Flux

data class QueueMessage(
  val messageId: String,
  val receiptHandle: String,
  val body: String,
)

interface QueueConsumerPort {
  fun receive(): Flux<QueueMessage>
  fun delete(receiptHandle: String): reactor.core.publisher.Mono<Void>
}
