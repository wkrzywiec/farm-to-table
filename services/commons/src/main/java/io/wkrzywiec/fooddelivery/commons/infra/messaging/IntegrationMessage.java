package io.wkrzywiec.fooddelivery.commons.infra.messaging;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationEventMapper;
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public record IntegrationMessage(Header header, IntegrationMessageBody body) {

    public static IntegrationMessage firstMessage(String channel, Clock clock, IntegrationMessageBody body) {
        return message(channel, clock, body, 1);
    }

    public static IntegrationMessage message(String channel, Clock clock, IntegrationMessageBody body, int version) {
        return new IntegrationMessage(
                new Header(UUID.randomUUID().toString(), version, channel, body.getClass().getSimpleName(), body.orderId(), clock.instant()),
                body
        );
    }

    public static List<IntegrationMessage> integrationEvents(List<EventEntity> eventEntities, IntegrationEventMapper mapper) {
        return eventEntities.stream().map(eventEntity -> integrationEvent(eventEntity, mapper)).toList();
    }

    public static IntegrationMessage integrationEvent(EventEntity eventEntity, IntegrationEventMapper mapper) {
        return new IntegrationMessage(
                new Header(
                        eventEntity.id(), eventEntity.version(), eventEntity.channel(),
                        eventEntity.type(), eventEntity.streamId(), eventEntity.addedAt()),
                mapper.map(eventEntity.data())
        );
    }
}
