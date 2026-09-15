package com.csykes.searchlight.docsGenerator;

import com.csykes.searchlight.docsGenerator.entities.BlockPeripheralPair;
import com.csykes.searchlight.docsGenerator.entities.LuaMethod;
import com.csykes.searchlight.docsGenerator.entities.PeripheralDocRecord;
import com.csykes.searchlight.docsGenerator.entities.PeripheralDocumentation;
import com.csykes.searchlight.docsGenerator.entities.RecipeDocRecord;
import com.csykes.searchlight.docsGenerator.services.JavaParsingService;
import com.csykes.searchlight.docsGenerator.services.MarkdownService;
import com.csykes.searchlight.docsGenerator.services.RecipeParsingService;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocsGeneratorTest {

    @Test
    @DisplayName("main with no arguments handles gracefully without throwing exceptions")
    void testMainWithoutArguments() {
        assertDoesNotThrow(() -> com.csykes.searchlight.docsGenerator.DocsGenerator.main(new String[0]));
    }

    @Test
    @DisplayName("MarkdownService preserves specials inside multi-line code blocks while escaping outside")
    void testCodeBlocksPreservedInMarkdown() {
        String description = """
                Sets the light color (e.g. {red}, [blue]).
                ```lua
                local light = peripheral.find("light")
                light.setPixels({ [1] = "red", [2] = { color = "blue", lit = true } })
                ```
                Check if <value> is: true?
                """;

        LuaMethod method = LuaMethod.builder()
                .name("testMethod")
                .description(description)
                .parameters(Collections.emptyList())
                .build();

        PeripheralDocumentation doc = new PeripheralDocumentation("Test Peripheral", List.of(method));
        PeripheralDocRecord record = new PeripheralDocRecord(Path.of("TestPeripheral.java"), Map.of());
        MarkdownService service = new MarkdownService(record, doc);

        String page = service.buildPage();

        // Specials outside code blocks must be escaped
        assertTrue(page.contains("&#40;e.g. &#123;red&#125;, &#91;blue&#93;&#41;"));
        assertTrue(page.contains("&lt;value&gt; is&#58; true&#63;"));

        // Specials inside code blocks must NOT be escaped
        assertTrue(page.contains("""
                ```lua
                local light = peripheral.find("light")
                light.setPixels({ [1] = "red", [2] = { color = "blue", lit = true } })
                ```"""));
    }

    @Test
    @DisplayName("Validates registered peripherals and their exposed Lua method counts against peripheral source classes")
    void testPeripheralAndMethodCountsMatchClasses() throws IOException {
        JavaParsingService parser = new JavaParsingService();
        Map<String, List<String>> blockEntities = parser.parseBEBlocks();
        List<BlockPeripheralPair> peripheralBEs = parser.parseBEs(blockEntities);

        assertFalse(peripheralBEs.isEmpty(), "Expected registered peripheral block entities to be found");

        // 1. Discover all unique peripheral classes registered in CCIntegration
        List<Path> uniquePeripheralPaths = peripheralBEs.stream()
                .map(BlockPeripheralPair::getPeripheralClass)
                .distinct()
                .filter(Objects::nonNull)
                .toList();

        assertFalse(uniquePeripheralPaths.isEmpty(), "Expected at least one peripheral class to be registered");

        // 2. Parse methods and documentation through the generator service
        Map<Path, PeripheralDocumentation> parsedDocs = parser.parseMethods(peripheralBEs);
        assertEquals(uniquePeripheralPaths.size(), parsedDocs.size(), "Expected parsed docs for every unique peripheral");

        // 3. For each registered peripheral, independently parse the class file and verify that the
        // generator's detected method count and exposed names match the class definitions dynamically
        for (Path path : uniquePeripheralPaths) {
            CompilationUnit cu = StaticJavaParser.parse(path);
            Set<String> expectedLuaMethodNames = cu.findAll(MethodDeclaration.class).stream()
                    .filter(m -> m.getAnnotationByName("LuaFunction").isPresent())
                    .map(MethodDeclaration::getNameAsString)
                    .collect(Collectors.toSet());

            assertFalse(expectedLuaMethodNames.isEmpty(),
                    () -> "Expected peripheral class " + path.getFileName() + " to expose at least one @LuaFunction");

            PeripheralDocumentation doc = parsedDocs.get(path);
            assertNotNull(doc, "Expected peripheral doc for " + path.getFileName());

            Set<String> actualLuaMethodNames = doc.methods().stream()
                    .map(LuaMethod::getName)
                    .collect(Collectors.toSet());

            // Validate that detected method counts and names match the class exactly
            assertEquals(expectedLuaMethodNames.size(), doc.methods().size(),
                    () -> "Method count mismatch for " + path.getFileName() + ". Class has " + expectedLuaMethodNames.size() + " (" + expectedLuaMethodNames + "), but generator found " + doc.methods().size() + " (" + actualLuaMethodNames + ")");
            assertEquals(expectedLuaMethodNames, actualLuaMethodNames,
                    () -> "Method name mismatch for " + path.getFileName());
        }
    }

    @Test
    @DisplayName("RecipeParsingService parses all recipes and formats alternating dye slots with @[] syntax")
    void testRecipeParsingService() {
        RecipeParsingService parser = new RecipeParsingService();
        List<RecipeDocRecord> recipes = parser.parseAllRecipes();

        assertFalse(recipes.isEmpty(), "Expected recipes to be parsed");

        // Find centre_light_@color@ recipe
        RecipeDocRecord centreLight = recipes.stream()
                .filter(r -> "centre_light_@color@".equals(r.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(centreLight, "Expected centre_light_@color@ recipe to be parsed");
        assertEquals("minecraft:crafting_shapeless", centreLight.getType());

        // Slot 1 should alternate dyes (starting with white_dye)
        String slot1 = centreLight.getSlot(1);
        assertTrue(slot1.startsWith("@[") && slot1.endsWith("]"), "Slot 1 must use @[...] syntax for dyes");
        assertTrue(slot1.contains("white_dye") && slot1.contains("red_dye") && slot1.contains("blue_dye"));
        assertTrue(slot1.startsWith("@[white_dye,"), "Slot 1 first item should be white_dye");

        // Slot 2 should alternate centre_lights rotated/out of sync (starting with centre_light_orange)
        String slot2 = centreLight.getSlot(2);
        assertTrue(slot2.startsWith("@[") && slot2.endsWith("]"), "Slot 2 must use @[...] syntax for centre_lights tag");
        assertTrue(slot2.startsWith("@[centre_light_orange,"), "Slot 2 first item should be rotated to centre_light_orange so it is out of sync with white_dye");
        assertTrue(slot2.contains("centre_light_white") && slot2.contains("centre_light_red"));

        // Slots 3 to 9 must be blank
        for (int i = 3; i <= 9; i++) {
            assertEquals("", centreLight.getSlot(i), "Slot " + i + " should be empty");
        }

        // Shaped recipe test: searchlight_white_base
        RecipeDocRecord searchlightBase = recipes.stream()
                .filter(r -> "searchlight_white_base".equals(r.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(searchlightBase, "Expected searchlight_white_base recipe");
        assertEquals("iron_nugget", searchlightBase.getSlot(1));
        assertEquals("iron_ingot", searchlightBase.getSlot(2));
        assertEquals("iron_nugget", searchlightBase.getSlot(3));
        assertEquals("torch", searchlightBase.getSlot(5));
        assertEquals("glass", searchlightBase.getSlot(7));
        assertEquals("glass", searchlightBase.getSlot(8));
        assertEquals("glass", searchlightBase.getSlot(9));
    }

    @Test
    @DisplayName("DocsGenerator generates recipes.json and extracts game textures into target directory")
    void testDocsGeneratorGeneratesRecipesJson(@TempDir Path tempDir) throws IOException {
        DocsGenerator.main(new String[]{tempDir.toString()});

        Path recipesJson = tempDir.resolve("recipes.json");
        assertTrue(Files.exists(recipesJson), "recipes.json should be created in generated docs directory");

        String content = Files.readString(recipesJson);
        assertTrue(content.contains("centre_light"), "recipes.json must contain centre_light");
        assertTrue(content.contains("colour_lamp"), "recipes.json must contain colour_lamp");
        assertTrue(content.contains("wall_light"), "recipes.json must contain wall_light");

        // Verify extracted game textures
        assertTrue(Files.exists(tempDir.resolve("white_dye.png")), "white_dye.png should be extracted");
        assertTrue(Files.exists(tempDir.resolve("red_dye.png")), "red_dye.png should be extracted");
        assertTrue(Files.exists(tempDir.resolve("torch.png")), "torch.png should be extracted");
        assertTrue(Files.exists(tempDir.resolve("redstone.png")), "redstone.png should be extracted");
        assertTrue(Files.exists(tempDir.resolve("iron_ingot.png")), "iron_ingot.png should be extracted");
        assertTrue(Files.exists(tempDir.resolve("iron_nugget.png")), "iron_nugget.png should be extracted");
        assertTrue(Files.exists(tempDir.resolve("copper_ingot.png")), "copper_ingot.png should be extracted");
        assertTrue(Files.exists(tempDir.resolve("glass.png")), "glass.png should be extracted");
        assertTrue(Files.exists(tempDir.resolve("stone.png")), "stone.png should be extracted");
        assertTrue(Files.exists(tempDir.resolve("prismarine_shard.png")), "prismarine_shard.png should be extracted");
        assertTrue(Files.exists(tempDir.resolve("prismarine_crystals.png")), "prismarine_crystals.png should be extracted");
    }

    @Test
    @DisplayName("Generate docs into project docs/generated folder")
    void testGenerateDocsDirectory() {
        DocsGenerator.main(new String[]{"docs/generated"});
        Path docsGenerated = Path.of("docs/generated");
        assertTrue(Files.exists(docsGenerated.resolve("recipes.json")));
        assertTrue(Files.exists(docsGenerated.resolve("white_dye.png")));
        assertTrue(Files.exists(docsGenerated.resolve("red_dye.png")));
        assertTrue(Files.exists(docsGenerated.resolve("torch.png")));
    }
}
