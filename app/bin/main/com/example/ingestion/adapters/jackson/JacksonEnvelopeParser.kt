package com.example.ingestion.adapters.jackson

import com.example.ingestion.domain.IngestionEnvelope
import com.example.ingestion.ports.EnvelopeParserPort
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class JacksonEnvelopeParser(
  private val mapper: ObjectMapper,
) : EnvelopeParserPort {
  override fun parse(rawJson: String): IngestionEnvelope =
    mapper.readValue(rawJson, IngestionEnvelope::class.java)
}
