package io.wkrzywiec.fooddelivery.delivery.infra.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore;
import io.wkrzywiec.fooddelivery.commons.infra.store.PostgresEventStore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("postgres-event-store")
@Import({DataSourceAutoConfiguration.class})
public class PostgresEventStoreConfig {

    @Bean
    public EventStore eventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new PostgresEventStore(jdbcTemplate, objectMapper, new DeliveryEventClassTypeProvider());
    }
}
