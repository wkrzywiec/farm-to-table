package io.wkrzywiec.fooddelivery.commons.infra.store;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public interface EventClassTypeProvider {

    public Class<? extends IntegrationMessageBody> getClassType(String type);
    public Class<? extends DomainEvent> getClassType1(String type);
}
