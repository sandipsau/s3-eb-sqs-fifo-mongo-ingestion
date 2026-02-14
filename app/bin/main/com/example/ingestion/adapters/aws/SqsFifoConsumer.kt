package com.example.ingestion.adapters.aws

import com.example.ingestion.ports.QueueConsumerPort
import com.example.ingestion.ports.QueueMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.time.Duration

@Component
class SqsFifoConsumer(
  private val sqs: SqsAsyncClient,
  @Value("\${ingestion.sqs.queueUrl}") private val queueUrl: String,
  @Value("\${ingestion.sqs.maxMessages}") private val maxMessages: Int,
  @Value("\${ingestion.sqs.waitTimeSeconds}") private val waitTimeSeconds: Int,
  @Value("\${ingestion.sqs.visibilityTimeoutSeconds}") private val visibilityTimeoutSeconds: Int,
) : QueueConsumerPort {

  override fun receive(): Flux<QueueMessage> {
    require(queueUrl.isNotBlank()) { "SQS_QUEUE_URL (ingestion.sqs.queueUrl) must be set" }

    val req = ReceiveMessageRequest.builder()
      .queueUrl(queueUrl)
      .maxNumberOfMessages(maxMessages)
      .waitTimeSeconds(waitTimeSeconds)
      .visibilityTimeout(visibilityTimeoutSeconds)
      .build()

    return Flux.defer { Mono.fromFuture(sqs.receiveMessage(req)) }
      .repeat()
      .flatMapIterable { it.messages() }
      .map { m -> QueueMessage(m.messageId(), m.receiptHandle(), m.body()) }
      // small pace to avoid hot loop when empty
      .timeout(Duration.ofSeconds((waitTimeSeconds + 30).toLong()))
      .retry()
  }

  override fun delete(receiptHandle: String): Mono<Void> {
    val req = DeleteMessageRequest.builder()
      .queueUrl(queueUrl)
      .receiptHandle(receiptHandle)
      .build()
    return Mono.fromFuture(sqs.deleteMessage(req)).then()
  }
}
