package io.wkrzywiec.fooddelivery.ordering.infra.store;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("postgres-event-store")
@EnableScheduling
@Import({DataSourceAutoConfiguration.class})
public class PostgresEventStoreConfig {

}
