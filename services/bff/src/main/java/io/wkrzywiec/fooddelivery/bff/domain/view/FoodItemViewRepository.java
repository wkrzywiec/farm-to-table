package io.wkrzywiec.fooddelivery.bff.domain.view;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface FoodItemViewRepository {
    List<JsonNode> findByQuery(String query);
}
