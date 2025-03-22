package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.event.IntegrationEventMapper
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*
import org.mapstruct.Mapper

@Mapper
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

    fun map(created: OrderingEvent.OrderCreated): OrderCreated
    fun map(canceled: OrderingEvent.OrderCanceled): OrderCanceled
    fun map(inProgress: OrderingEvent.OrderInProgress): OrderInProgress
    fun map(tipAdded: OrderingEvent.TipAddedToOrder): TipAddedToOrder
    fun map(completed: OrderingEvent.OrderCompleted): OrderCompleted
}