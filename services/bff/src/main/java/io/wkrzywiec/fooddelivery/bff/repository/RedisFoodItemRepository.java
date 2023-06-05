package io.wkrzywiec.fooddelivery.bff.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redislabs.lettusearch.RediSearchCommands;
import com.redislabs.lettusearch.SearchResults;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class RedisFoodItemRepository {

    private final StatefulRediSearchConnection<String, String> searchConnection;
    private final ObjectMapper objectMapper;

    public List<JsonNode> findByQuery(String query) {
        RediSearchCommands<String, String> commands = searchConnection.sync();
        SearchResults<String, String> results = commands.search("food-idx", query);
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
