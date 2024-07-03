package io.wkrzywiec.fooddelivery.commons.infra.store;

import lombok.Data;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class EventEntity {

    protected UUID id;
    protected UUID streamId;
    protected int version;
    protected String channel;
    protected String type;
    protected DomainEvent data;
    protected Instant addedAt;

    private EventEntity() {}

    public EventEntity(UUID id, UUID streamId, int version, String channel, String type, DomainEvent data, Instant addedAt) {
        this.id = id;
        this.streamId = streamId;
        this.version = version;
        this.channel = channel;
        this.type = type;
        this.data = data;
        this.addedAt = addedAt;
    }

    public static List<EventEntity> newEventEntities(List<DomainEvent> domainEvents , String channel, Clock clock) {
        return domainEvents.stream()
                .map(domainEvent -> newEventEntity(domainEvent, channel, clock))
                .toList();
    }

    public static EventEntity newEventEntity(DomainEvent domainEvent, String channel, Clock clock) {
        return new EventEntity(
                UUID.randomUUID(), domainEvent.streamId(),
                domainEvent.version(), channel,
                domainEvent.getClass().getSimpleName(), domainEvent,
                clock.instant());
    }

    public UUID id() {
        return id;
    }

    public UUID streamId() {
        return streamId;
    }

    public int version() {
        return version;
    }

    public String channel() {
        return channel;
    }

    public String type() {
        return type;
    }

    public DomainEvent data() {
        return data;
    }

    public Instant addedAt() {
        return addedAt;
    }
}
