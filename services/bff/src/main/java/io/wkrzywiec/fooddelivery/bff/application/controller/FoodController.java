package io.wkrzywiec.fooddelivery.bff.application.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.wkrzywiec.fooddelivery.bff.domain.view.redis.read.RedisFoodItemViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FoodController {

    private final RedisFoodItemViewRepository repository;

    @GetMapping("/foods")
    List<JsonNode> findFoodItems(@RequestParam(name="q") String query) {
        log.info("Received request to find food items by query: {}", query);
        return repository.findByQuery(query);
    }
}
