package com.taskflow.common.time

import jakarta.validation.ClockProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

/** One clock decides what "today" means everywhere, and tests can replace it with a fixed one. */
@Configuration
class TimeConfig {

    @Bean
    fun clock(@Value("\${taskflow.time-zone:}") timeZone: String): Clock =
        if (timeZone.isBlank()) Clock.systemDefaultZone() else Clock.system(ZoneId.of(timeZone))

    /** Makes annotations such as @FutureOrPresent use the same clock as the services. */
    @Bean
    fun validationClock(clock: Clock) = ValidationConfigurationCustomizer { configuration ->
        configuration.clockProvider(ClockProvider { clock })
    }
}
