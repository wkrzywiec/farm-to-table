package io.wkrzywiec.fooddelivery.commons.infra.messaging;

import java.time.Instant;

public record Header(String id, int version, String channel, String type, String streamId, Instant createdAt) {
}
