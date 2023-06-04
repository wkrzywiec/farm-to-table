package io.wkrzywiec.fooddelivery.food.repository;

import io.wkrzywiec.fooddelivery.food.controller.FoodItemDTO;

import java.util.List;

public interface FoodItemRepository {

    List<FoodItemDTO> saveAll(List<FoodItemDTO> foodItemDTOs);
}
