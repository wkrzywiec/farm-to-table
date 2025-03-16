package io.wkrzywiec.fooddelivery.commons.infra.store;

public interface EventClassTypeProvider {

    Class<? extends DomainEvent> getClassType(String type);
}
