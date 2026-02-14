package com.example.ingestion.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.time.Duration

@Configuration
class AwsClientsConfig(
  @Value("\${ingestion.aws.region}") private val region: String,
) {
  @Bean
  fun sqsAsyncClient(): SqsAsyncClient =
    SqsAsyncClient.builder()
      .region(Region.of(region))
      .credentialsProvider(DefaultCredentialsProvider.create())
      .httpClientBuilder(
        NettyNioAsyncHttpClient.builder()
          .maxConcurrency(200)
          .readTimeout(Duration.ofSeconds(60))
      )
      .build()

  @Bean
  fun s3AsyncClient(): S3AsyncClient =
    S3AsyncClient.builder()
      .region(Region.of(region))
      .credentialsProvider(DefaultCredentialsProvider.create())
      .httpClientBuilder(
        NettyNioAsyncHttpClient.builder()
          .maxConcurrency(200)
          .readTimeout(Duration.ofSeconds(60))
      )
      .build()
}
