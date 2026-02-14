package com.example.ingestion.runtime

import com.example.ingestion.ports.EnvelopeParserPort
import com.example.ingestion.ports.QueueConsumerPort
import com.example.ingestion.usecase.IngestFileUseCase
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Component
class IngestionRunner(
  private val queue: QueueConsumerPort,
  private val envelopeParser: EnvelopeParserPort,
  private val ingestFile: IngestFileUseCase,
  @Value("\${ingestion.sqs.concurrency}") private val concurrency: Int,
) {
  private val log = LoggerFactory.getLogger(javaClass)

  @PostConstruct
  fun start() {
    // Long-running subscription
    queue.receive()
      .flatMap({ msg ->
        Mono.defer {
          val env = envelopeParser.parse(msg.body)
          val corrId = env.trace?.correlationId
          ingestFile.ingest(env.file.bucket, env.file.key, corrId)
            .then(queue.delete(msg.receiptHandle))
            .doOnSuccess { log.info("Deleted SQS messageId={} corrId={}", msg.messageId, corrId) }
        }.onErrorResume { e ->
          // DO NOT delete => retries; after maxReceiveCount goes to DLQ
          log.error("Processing failed for messageId={}, will retry", msg.messageId, e)
          Mono.empty()
        }
      }, concurrency)
      .subscribeOn(Schedulers.boundedElastic())
      .subscribe()
  }
}
