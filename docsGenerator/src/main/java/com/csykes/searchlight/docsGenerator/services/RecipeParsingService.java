package com.csykes.searchlight.docsGenerator.services;

import com.csykes.searchlight.docsGenerator.entities.RecipeDocRecord;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class RecipeParsingService {

    public static final List<String> COLORS = List.of(
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    );

    private static List<String> rotatedColors(String prefix) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < COLORS.size(); i++) {
            list.add(prefix + COLORS.get((i + 1) % COLORS.size()));
        }
        return list;
    }

    private static final Map<String, List<String>> TAG_RESOLVER = Map.ofEntries(
            Map.entry("searchlight:centre_lights", rotatedColors("centre_light_")),
            Map.entry("searchlight:colour_lamps", rotatedColors("colour_lamp_")),
            Map.entry("searchlight:colour_lamp_slab", rotatedColors("colour_lamp_slab_")),
            Map.entry("searchlight:corner_lights", rotatedColors("corner_light_")),
            Map.entry("searchlight:edge_lights", rotatedColors("edge_light_")),
            Map.entry("searchlight:searchlights", rotatedColors("searchlight_")),
            Map.entry("searchlight:wall_lights", rotatedColors("wall_light_")),
            Map.entry("c:stones", List.of("stone")),
            Map.entry("c:glass_blocks", List.of("glass")),
            Map.entry("c:ingots/copper", List.of("copper_ingot")),
            Map.entry("c:ingots/iron", List.of("iron_ingot"))
    );

    public List<RecipeDocRecord> parseAllRecipes() {
        List<Path> directories = List.of(
                Path.of("src/main/resources/data/searchlight/recipe"),
                Path.of("src/main/templates/color_resources/data/searchlight/recipe")
        );

        List<RecipeDocRecord> records = new ArrayList<>();
        for (Path dir : directories) {
            if (!Files.exists(dir)) {
                log.warn("Directory not found: {}", dir);
                continue;
            }
            try (Stream<Path> stream = Files.walk(dir)) {
                stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json"))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(p -> {
                            try {
                                Optional<RecipeDocRecord> record = parseRecipeFile(p);
                                record.ifPresent(records::add);
                            } catch (Exception e) {
                                log.error("Failed to parse recipe file: {}", p, e);
                            }
                        });
            } catch (IOException e) {
                log.error("Failed to read directory: {}", dir, e);
            }
        }
        return records;
    }

    public Optional<RecipeDocRecord> parseRecipeFile(Path path) throws IOException {
        String content = Files.readString(path);
        String fileName = path.getFileName().toString().replaceFirst("[.][^.]+$", "");
        return parseRecipe(fileName, content);
    }

    public Optional<RecipeDocRecord> parseRecipe(String recipeId, String jsonContent) {
        JsonObject json = JsonParser.parseString(jsonContent).getAsJsonObject();
        String type = json.has("type") ? json.get("type").getAsString() : "minecraft:crafting_shaped";
        String group = json.has("group") ? json.get("group").getAsString() : "";

        List<String> slots = new ArrayList<>(Collections.nCopies(9, ""));
        List<String> result = new ArrayList<>();
        int resultCount = 1;

        if (json.has("result")) {
            JsonObject resultObj = json.getAsJsonObject("result");
            String rawResultId = resultObj.has("id") ? resultObj.get("id").getAsString()
                    : (resultObj.has("item") ? resultObj.get("item").getAsString() : "");
            result = resolveItemString(rawResultId);
            if (resultObj.has("count")) {
                resultCount = resultObj.get("count").getAsInt();
            }
        }

        if ("minecraft:crafting_shaped".equals(type) || json.has("pattern")) {
            if (!json.has("pattern") || !json.has("key")) {
                return Optional.empty();
            }
            JsonArray patternArray = json.getAsJsonArray("pattern");
            JsonObject keyObj = json.getAsJsonObject("key");

            Map<Character, String> keyMap = new HashMap<>();
            for (String keyChar : keyObj.keySet()) {
                JsonElement elem = keyObj.get(keyChar);
                String formatted = resolveIngredient(elem);
                keyMap.put(keyChar.charAt(0), formatted);
            }

            for (int r = 0; r < patternArray.size() && r < 3; r++) {
                String row = patternArray.get(r).getAsString();
                for (int c = 0; c < row.length() && c < 3; c++) {
                    char ch = row.charAt(c);
                    if (ch != ' ' && keyMap.containsKey(ch)) {
                        slots.set(r * 3 + c, keyMap.get(ch));
                    }
                }
            }
        } else if ("minecraft:crafting_shapeless".equals(type) || json.has("ingredients")) {
            if (!json.has("ingredients")) {
                return Optional.empty();
            }
            JsonArray ingredientsArray = json.getAsJsonArray("ingredients");
            for (int i = 0; i < ingredientsArray.size() && i < 9; i++) {
                JsonElement elem = ingredientsArray.get(i);
                String formatted = resolveIngredient(elem);
                slots.set(i, formatted);
            }
        } else {
            // Unknown or dynamic recipe type (e.g. searchlight:lamp_brightness)
            return Optional.empty();
        }

        return Optional.of(RecipeDocRecord.builder()
                .id(recipeId)
                .type(type)
                .group(group)
                .slots(slots)
                .result(result)
                .resultCount(resultCount)
                .build());
    }

    private String resolveIngredient(JsonElement elem) {
        if (elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            if (obj.has("item")) {
                List<String> items = resolveItemString(obj.get("item").getAsString());
                return formatItemList(items);
            } else if (obj.has("tag")) {
                List<String> items = resolveTagString(obj.get("tag").getAsString());
                return formatItemList(items);
            }
        } else if (elem.isJsonPrimitive()) {
            List<String> items = resolveItemString(elem.getAsString());
            return formatItemList(items);
        }
        return "";
    }

    private List<String> resolveItemString(String rawItem) {
        String clean = rawItem.replace("@namespace@:", "").replace("minecraft:", "").replace("searchlight:", "");
        if (clean.contains("@color@")) {
            return COLORS.stream().map(c -> clean.replace("@color@", c)).toList();
        }
        return List.of(clean);
    }

    private List<String> resolveTagString(String rawTag) {
        if (TAG_RESOLVER.containsKey(rawTag)) {
            return TAG_RESOLVER.get(rawTag);
        }
        String clean = rawTag.replace("minecraft:", "").replace("searchlight:", "");
        if (TAG_RESOLVER.containsKey(clean)) {
            return TAG_RESOLVER.get(clean);
        }
        return List.of(clean);
    }

    private String formatItemList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        return "@[" + String.join(", ", items) + "]";
    }
}
