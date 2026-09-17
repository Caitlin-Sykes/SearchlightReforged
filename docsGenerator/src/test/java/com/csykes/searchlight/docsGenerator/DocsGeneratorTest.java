package com.csykes.searchlight.docsGenerator;

import com.csykes.searchlight.docsGenerator.entities.BlockPeripheralPair;
import com.csykes.searchlight.docsGenerator.entities.LuaMethod;
import com.csykes.searchlight.docsGenerator.entities.PeripheralDocRecord;
import com.csykes.searchlight.docsGenerator.entities.PeripheralDocumentation;
import com.csykes.searchlight.docsGenerator.services.JavaParsingService;
import com.csykes.searchlight.docsGenerator.services.MarkdownService;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
}
