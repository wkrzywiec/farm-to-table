package io.wkrzywiec.fooddelivery.commons.infra.store;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ToString
@EqualsAndHashCode
public class EventEntity {

    protected String id;
    protected String streamId;
    protected int version;
    protected String channel;
    protected String type;
    protected DomainEvent data;
    protected Instant addedAt;

    private EventEntity() {}

    public EventEntity(String id, String streamId, int version, String channel, String type, DomainEvent data, Instant addedAt) {
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
                UUID.randomUUID().toString(), domainEvent.streamId(),
                domainEvent.version(), channel,
                domainEvent.getClass().getSimpleName(), domainEvent,
                clock.instant());
    }

    public String id() {
        return id;
    }

    public String streamId() {
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
