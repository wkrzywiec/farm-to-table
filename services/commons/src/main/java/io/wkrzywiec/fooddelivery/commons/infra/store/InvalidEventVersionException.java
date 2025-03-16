package io.wkrzywiec.fooddelivery.commons.infra.store;

public class InvalidEventVersionException extends RuntimeException {

    public InvalidEventVersionException() {
        super("Failed to store an event. Event version is invalid.");
    }
}
