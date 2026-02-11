package com.example.ingestion.usecase

import com.example.ingestion.domain.MaterialTaxNode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class DelimitedFileParser(
  @Value("\${ingestion.parser.delimiter}") private val delimiter: String,
  @Value("\${ingestion.parser.hasHeader}") private val hasHeader: Boolean,
) {
  fun parse(lines: Flux<String>): Flux<MaterialTaxNode> {
    val dataLines = if (hasHeader) lines.skip(1) else lines
    return dataLines.map { line ->
      val parts = line.split(delimiter)
      require(parts.size == 2) { "Invalid line (expected 2 columns): $line" }
      MaterialTaxNode(
        materialNo = parts[0].trim(),
        workstationTaxNodeId = parts[1].trim(),
      )
    }
  }
}
