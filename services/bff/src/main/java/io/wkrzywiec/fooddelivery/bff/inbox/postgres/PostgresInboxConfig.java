package io.wkrzywiec.fooddelivery.bff.inbox.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import io.wkrzywiec.fooddelivery.bff.inbox.InboxMessageProcessor;
import io.wkrzywiec.fooddelivery.bff.inbox.InboxPublisher;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@Profile("postgres-inbox")
@EnableScheduling
@Import({DataSourceAutoConfiguration.class})
public class PostgresInboxConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public InboxPublisher postgresInboxPublisher(JdbcTemplate jdbcTemplate, Clock clock, ObjectMapper objectMapper) {
        return new PostgresInboxPublisher(jdbcTemplate, clock, objectMapper);
    }

    @Bean
    public PostgresInboxListener postgresInboxListener(JdbcTemplate jdbcTemplate, InboxMessageProcessor inboxMessageProcessor, ObjectMapper objectMapper) {
        return new PostgresInboxListener(jdbcTemplate, inboxMessageProcessor, objectMapper);
    }
}
