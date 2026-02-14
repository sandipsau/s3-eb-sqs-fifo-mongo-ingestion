package com.example.ingestion.ports

import com.example.ingestion.domain.IngestionEnvelope

interface EnvelopeParserPort {
  fun parse(rawJson: String): IngestionEnvelope
}
