package io.wkrzywiec.fooddelivery.delivery.domain;

class DeliveryException extends RuntimeException {

    DeliveryException(String message) {
        super(message);
    }

    DeliveryException(String message, Exception cause) {
        super(message, cause);
    }
}
