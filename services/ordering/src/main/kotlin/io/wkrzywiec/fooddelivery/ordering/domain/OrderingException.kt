package io.wkrzywiec.fooddelivery.ordering.domain

class OrderingException : RuntimeException {
    internal constructor(message: String?) : super(message)

    internal constructor(message: String?, cause: Exception?) : super(message, cause)
}
