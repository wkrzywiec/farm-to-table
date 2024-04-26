package io.wkrzywiec.fooddelivery.bff.domain.inbox.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.bff.domain.inbox.Inbox;
import io.wkrzywiec.fooddelivery.bff.domain.inbox.InboxMessageProcessor;
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
    public Inbox postgresInboxPublisher(JdbcTemplate jdbcTemplate, Clock clock, ObjectMapper objectMapper) {
        return new PostgresInbox(jdbcTemplate, clock, objectMapper);
    }

    @Bean
    public PostgresInboxListener postgresInboxListener(JdbcTemplate jdbcTemplate, InboxMessageProcessor inboxMessageProcessor, ObjectMapper objectMapper) {
        return new PostgresInboxListener(jdbcTemplate, inboxMessageProcessor, objectMapper);
    }
}
