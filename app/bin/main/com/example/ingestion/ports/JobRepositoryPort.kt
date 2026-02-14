package com.example.ingestion.ports

import com.example.ingestion.domain.JobKey
import reactor.core.publisher.Mono

interface JobRepositoryPort {
  /** returns true if claimed (new job inserted), false if already exists */
  fun tryClaim(key: JobKey): Mono<Boolean>
  fun markDone(key: JobKey): Mono<Void>
  fun markFailed(key: JobKey, error: String): Mono<Void>
}
