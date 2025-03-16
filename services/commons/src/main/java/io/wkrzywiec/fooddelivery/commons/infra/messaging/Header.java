package io.wkrzywiec.fooddelivery.commons.infra.messaging;

import java.time.Instant;
import java.util.UUID;

public record Header(UUID id, int version, String channel, String type, UUID streamId, Instant createdAt) {
}
