package com.example.ingestion

import com.example.ingestion.usecase.DelimitedFileParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux

class DelimitedFileParserTest {
  @Test
  fun `parses pipe delimited rows skipping header`() {
    val parser = DelimitedFileParser("|", true)
    val rows = parser.parse(Flux.just(
      "material_no|workstation_tax_node_id",
      "31KZ71|2356",
      "1A377|9999"
    )).collectList().block()!!

    assertEquals(2, rows.size)
    assertEquals("31KZ71", rows[0].materialNo)
    assertEquals("2356", rows[0].taxNodeId)
  }
}
