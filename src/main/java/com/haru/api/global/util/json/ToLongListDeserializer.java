package com.haru.api.global.util.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

public class ToLongListDeserializer extends JsonDeserializer<List<Long>> {
    @Override
    public List<Long> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<Long> result = new ArrayList<>();
        JsonNode node = p.getCodec().readTree(p);

        if (node.isArray()) {
            for (JsonNode item : node) {
                try {
                    result.add(Long.parseLong(item.asText()));
                } catch (NumberFormatException e) {
                    throw new IOException("파싱 실패: " + item.asText());
                }
            }
        }

        return result;
    }
}
