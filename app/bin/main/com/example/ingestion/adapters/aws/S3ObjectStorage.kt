package com.example.ingestion.adapters.aws

import com.example.ingestion.ports.ObjectMetadata
import com.example.ingestion.ports.ObjectStoragePort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import java.nio.ByteBuffer
import java.nio.charset.Charset

@Component
class S3ObjectStorage(
  private val s3: S3AsyncClient,
  @Value("\${ingestion.parser.charset}") private val charsetName: String,
) : ObjectStoragePort {

  private val charset: Charset = Charset.forName(charsetName)

  override fun head(bucket: String, key: String): Mono<ObjectMetadata> {
    val req = HeadObjectRequest.builder().bucket(bucket).key(key).build()
    return Mono.fromFuture(s3.headObject(req))
      .map { ObjectMetadata(it.eTag(), it.versionId(), it.contentLength()) }
  }

  override fun readLines(bucket: String, key: String): Flux<String> {
    val req = GetObjectRequest.builder().bucket(bucket).key(key).build()

    return Mono.fromFuture(s3.getObject(req, AsyncResponseTransformer.toPublisher()))
      .flatMapMany { publisherResp ->
        Flux.from(publisherResp)
          .transform { byteBuffersToLines(it, charset) }
      }
  }

  /**
   * Chunk-safe line splitting. Handles \n across bytebuffer boundaries.
   */
  private fun byteBuffersToLines(buffers: Flux<ByteBuffer>, charset: Charset): Flux<String> {
    return Flux.create { sink ->
      val sb = StringBuilder()
      buffers.subscribe(
        { bb ->
          val text = charset.decode(bb).toString()
          sb.append(text)
          var idx: Int
          while (true) {
            idx = sb.indexOf("\n")
            if (idx < 0) break
            val line = sb.substring(0, idx).trimEnd('\r')
            sb.delete(0, idx + 1)
            if (line.isNotBlank()) sink.next(line)
          }
        },
        { err -> sink.error(err) },
        {
          val last = sb.toString().trimEnd('\r')
          if (last.isNotBlank()) sink.next(last)
          sink.complete()
        }
      )
    }
  }
}
