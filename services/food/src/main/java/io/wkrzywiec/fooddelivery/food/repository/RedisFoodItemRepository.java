package io.wkrzywiec.fooddelivery.food.repository;

import com.redislabs.modules.rejson.JReJSON;
import io.wkrzywiec.fooddelivery.food.controller.FoodItemDTO;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class RedisFoodItemRepository implements FoodItemRepository {

    private final JReJSON redisJson;
    public List<FoodItemDTO> saveAll(List<FoodItemDTO> foodItemDTOs) {
        List<FoodItemDTO> result = new ArrayList<>();

        for (FoodItemDTO food: foodItemDTOs) {
            if (food.getId() == null) {
                food.setId(UUID.randomUUID().toString());
            }

            String key = getKey(food);
            redisJson.set(key, food);
            result.add(food);
        }

        return result;
    }

    private String getKey(FoodItemDTO foodItemDTO) {
        String id = foodItemDTO.getId() == null ? UUID.randomUUID().toString() : foodItemDTO.getId();
        return String.format("%s:%s", "food", id);
    }
}
