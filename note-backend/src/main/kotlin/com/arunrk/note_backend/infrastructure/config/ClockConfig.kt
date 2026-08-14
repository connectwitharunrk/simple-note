package com.arunrk.note_backend.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * Time is an injected dependency, not an ambient one.
 *
 * Use cases take a [Clock] instead of calling `Instant.now()`, which keeps "who decides
 * `updatedAt`" explicit and lets tests assert on exact timestamps with a fixed clock.
 */
@Configuration
class ClockConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
