package io.wkrzywiec.fooddelivery.bff.domain.view.redis.read;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RediSearchCommands;
import com.redis.lettucemod.search.SearchResults;
import io.wkrzywiec.fooddelivery.bff.domain.view.FoodItemViewRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class RedisFoodItemViewRepository implements FoodItemViewRepository {

    private final StatefulRedisModulesConnection<String, String> searchConnection;
    private final ObjectMapper objectMapper;

    public List<JsonNode> findByQuery(String query) {
        RediSearchCommands<String, String> commands = searchConnection.sync();
        SearchResults<String, String> results = commands.ftSearch("food-idx", query);
        return results.stream()
                .map(d -> d.get("$"))
                .map(this::mapToJson)
                .toList();
    }

    private JsonNode mapToJson(String string) {
        try {
            return objectMapper.readTree(string);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
