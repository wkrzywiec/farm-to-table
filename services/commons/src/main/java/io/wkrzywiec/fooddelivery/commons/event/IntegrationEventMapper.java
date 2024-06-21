package io.wkrzywiec.fooddelivery.commons.event;

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;

public interface IntegrationEventMapper {
    IntegrationMessageBody map(DomainEvent domainEvent);
}
