package com.csykes.searchlight.docsGenerator;

import com.csykes.searchlight.docsGenerator.entities.RecipeDocRecord;
import com.csykes.searchlight.docsGenerator.services.JavaParsingService;
import com.csykes.searchlight.docsGenerator.services.MarkdownService;
import com.csykes.searchlight.docsGenerator.services.RecipeParsingService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DocsGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("Texture path must be provided as an argument");
            return;
        }
        log.info("Generating docs");

        Path targetDir = Path.of(args[0]);
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            log.error("Failed to create target directory: {}", targetDir, e);
        }

        JavaParsingService parser = new JavaParsingService();
        try {
            var blockEnitites = parser.parseBEBlocks();
            var peripheralBEs = parser.parseBEs(blockEnitites);
            var peripheralMethods = parser.parseMethods(peripheralBEs);

            // Group Block Entities and their blocks by Peripheral class
            var peripherals = peripheralBEs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            com.csykes.searchlight.docsGenerator.entities.BlockPeripheralPair::getPeripheralClass,
                            java.util.LinkedHashMap::new,
                            java.util.stream.Collectors.toMap(
                                    com.csykes.searchlight.docsGenerator.entities.BlockPeripheralPair::getBlockId,
                                    pair -> pair.getBlocks() != null ? pair.getBlocks() : java.util.List.<String>of(),
                                    (existing, replacement) -> existing,
                                    java.util.LinkedHashMap::new
                            )
                    ))
                    .entrySet().stream()
                    .map(entry -> new com.csykes.searchlight.docsGenerator.entities.PeripheralDocRecord(entry.getKey(), entry.getValue()))
                    .toList();

            peripherals.forEach(peripheral -> {
                MarkdownService markdownService = new MarkdownService(peripheral, peripheralMethods.get(peripheral.peripheralClass()));
                try {
                    Path mdPath = targetDir.resolve(peripheral.peripheralClass().getFileName().toString().replaceFirst("[.][^.]+$", "") + ".md");
                    try (FileWriter fw = new FileWriter(mdPath.toFile())) {
                        fw.write(markdownService.buildPage());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            log.error("File not found", e);
        }

        // Generate recipes
        log.info("Generating recipes");
        RecipeParsingService recipeParser = new RecipeParsingService();
        List<RecipeDocRecord> recipes = recipeParser.parseAllRecipes();

        Map<String, Object> recipesMap = new LinkedHashMap<>();
        for (RecipeDocRecord recipe : recipes) {
            recipesMap.put(recipe.getId(), recipe);
            // Also register sanitized shorthand key (e.g. centre_light_@color@ -> centre_light)
            if (recipe.getId().contains("@color@")) {
                String shorthand = recipe.getId().replace("_@color@", "");
                recipesMap.putIfAbsent(shorthand, recipe);
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Path recipesJsonPath = targetDir.resolve("recipes.json");
            try (FileWriter fw = new FileWriter(recipesJsonPath.toFile())) {
                gson.toJson(recipesMap, fw);
            }
            log.info("Generated {} recipes into {}", recipes.size(), recipesJsonPath);
        } catch (IOException e) {
            log.error("Failed to write recipes.json", e);
        }

        // Extract game textures
        extractGameTextures(targetDir);

        log.info("Done");
    }

    public static void extractGameTextures(Path targetDir) {
        log.info("Extracting game textures into {}", targetDir);
        Map<String, List<String>> resourceMappings = new LinkedHashMap<>();

        // 16 Vanilla Dyes
        for (String color : RecipeParsingService.COLORS) {
            String dyeName = color + "_dye";
            resourceMappings.put(dyeName + ".png", List.of(
                    "assets/minecraft/textures/item/" + dyeName + ".png",
                    "assets/minecraft/textures/item/" + color + "_dye.png"
            ));
        }

        // Dyenamics Dyes
        for (String dyeColor : JavaParsingService.getDynamicDyeColors()) {
            String dyeName = dyeColor + "_dye";
            resourceMappings.put(dyeName + ".png", List.of(
                    "assets/dyenamics/textures/item/" + dyeName + ".png",
                    "assets/minecraft/textures/item/" + dyeName + ".png"
            ));
        }

        // Vanilla Items & Blocks
        resourceMappings.put("torch.png", List.of(
                "assets/minecraft/textures/block/torch.png",
                "assets/minecraft/textures/item/torch.png"
        ));
        resourceMappings.put("redstone.png", List.of(
                "assets/minecraft/textures/item/redstone.png",
                "assets/minecraft/textures/item/redstone_dust.png"
        ));
        resourceMappings.put("iron_ingot.png", List.of("assets/minecraft/textures/item/iron_ingot.png"));
        resourceMappings.put("iron_nugget.png", List.of("assets/minecraft/textures/item/iron_nugget.png"));
        resourceMappings.put("copper_ingot.png", List.of("assets/minecraft/textures/item/copper_ingot.png"));
        resourceMappings.put("glass.png", List.of("assets/minecraft/textures/block/glass.png"));
        resourceMappings.put("stone.png", List.of("assets/minecraft/textures/block/stone.png"));
        resourceMappings.put("prismarine_shard.png", List.of("assets/minecraft/textures/item/prismarine_shard.png"));
        resourceMappings.put("prismarine_crystals.png", List.of("assets/minecraft/textures/item/prismarine_crystals.png"));
        resourceMappings.put("stick.png", List.of("assets/minecraft/textures/item/stick.png"));
        resourceMappings.put("glowstone_dust.png", List.of("assets/minecraft/textures/item/glowstone_dust.png"));

        // Searchlight Item Icons
        resourceMappings.put("lighting_linker_card.png", List.of(
                "assets/searchlight/textures/item/link_tool.png"
        ));
        resourceMappings.put("lighting_director.png", List.of(
                "assets/searchlight/textures/block/light_director.png"
        ));

        ClassLoader cl = DocsGenerator.class.getClassLoader();
        int extractedCount = 0;
        for (Map.Entry<String, List<String>> entry : resourceMappings.entrySet()) {
            String outFileName = entry.getKey();
            Path outPath = targetDir.resolve(outFileName);
            boolean found = false;
            for (String resPath : entry.getValue()) {
                try (var is = cl.getResourceAsStream(resPath)) {
                    if (is != null) {
                        Files.copy(is, outPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        extractedCount++;
                        found = true;
                        break;
                    }
                } catch (IOException e) {
                    log.error("Failed to extract texture {} from {}", outFileName, resPath, e);
                }

                Path localFile = Path.of("src/main/resources", resPath);
                if (Files.exists(localFile)) {
                    try {
                        Files.copy(localFile, outPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        extractedCount++;
                        found = true;
                        break;
                    } catch (IOException e) {
                        log.error("Failed to copy local texture {} from {}", outFileName, localFile, e);
                    }
                }
            }
            if (!found) {
                log.warn("Could not find texture resource for {}", outFileName);
            }
        }
        log.info("Extracted {} game textures to {}", extractedCount, targetDir);
    }
}