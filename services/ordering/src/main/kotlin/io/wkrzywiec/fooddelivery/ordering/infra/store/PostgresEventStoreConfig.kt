package io.wkrzywiec.fooddelivery.ordering.infra.store

import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore
import io.wkrzywiec.fooddelivery.commons.infra.store.postgres.PostgresEventStore
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

@Configuration
@Profile("postgres-event-store")
@Import(
    DataSourceAutoConfiguration::class
)
class PostgresEventStoreConfig {
    @Bean
    fun eventStore(jdbcTemplate: NamedParameterJdbcTemplate?, objectMapper: ObjectMapper?): EventStore {
        return PostgresEventStore(jdbcTemplate, objectMapper, OrderingEventClassTypeProvider())
    }
}
