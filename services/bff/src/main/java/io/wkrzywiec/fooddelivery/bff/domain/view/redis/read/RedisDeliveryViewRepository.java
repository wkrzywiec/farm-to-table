package io.wkrzywiec.fooddelivery.bff.domain.view.redis.read;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.bff.domain.view.DeliveryViewRepository;
import io.wkrzywiec.fooddelivery.bff.domain.view.DeliveryView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
@Slf4j
public class RedisDeliveryViewRepository implements DeliveryViewRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<DeliveryView> getAllDeliveryViews() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("delivery-view");

        return entries.values().stream()
                .map(this::mapJsonStringToDeliveryView)
                .toList();
    }

    @Override
    public Optional<DeliveryView> getDeliveryViewById(String orderId) {
        var deliveryViewOptional = ofNullable(redisTemplate.opsForHash().get("delivery-view", orderId));
        return deliveryViewOptional.map(this::mapJsonStringToDeliveryView);
    }

    private DeliveryView mapJsonStringToDeliveryView(Object deliveryView) {
        try {
            return objectMapper.readValue((String) deliveryView, DeliveryView.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
