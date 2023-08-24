package io.wkrzywiec.fooddelivery.commons.infra.store;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public interface EventClassTypeProvider {

    public Class<? extends DomainMessageBody> getClassType(String type);
}
