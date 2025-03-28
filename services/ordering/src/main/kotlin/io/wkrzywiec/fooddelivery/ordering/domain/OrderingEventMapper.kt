package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.event.IntegrationEventMapper
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
@JvmDefaultWithCompatibility
interface OrderingEventMapper : IntegrationEventMapper {

    override fun map(domainEvent: DomainEvent): IntegrationMessageBody {
        return when (domainEvent) {
            is OrderingEvent.OrderCreated -> map(domainEvent)
            is OrderingEvent.OrderCanceled -> map(domainEvent)
            is OrderingEvent.OrderInProgress  -> map(domainEvent)
            is OrderingEvent.TipAddedToOrder -> map(domainEvent)
            is OrderingEvent.OrderCompleted -> map(domainEvent)

            else -> throw IllegalArgumentException("Failed to map DomainEvent to IntegrationMessageBody. Unknown DomainEvent: ${domainEvent::class.simpleName}")
        }
    }

    @Mapping(source = "orderId", target = "id")
    @Mapping(source = "version", target = "aggregateVersion")
    fun map(created: OrderingEvent.OrderCreated): OrderCreated

    @Mapping(source = "orderId", target = "id")
    @Mapping(source = "version", target = "aggregateVersion")
    fun map(canceled: OrderingEvent.OrderCanceled): OrderCanceled

    @Mapping(source = "orderId", target = "id")
    @Mapping(source = "version", target = "aggregateVersion")
    fun map(inProgress: OrderingEvent.OrderInProgress): OrderInProgress

    @Mapping(source = "orderId", target = "id")
    @Mapping(source = "version", target = "aggregateVersion")
    fun map(tipAdded: OrderingEvent.TipAddedToOrder): TipAddedToOrder

    @Mapping(source = "orderId", target = "id")
    @Mapping(source = "version", target = "aggregateVersion")
    fun map(completed: OrderingEvent.OrderCompleted): OrderCompleted
}