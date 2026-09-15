package com.csykes.searchlight.docsGenerator;

import com.csykes.searchlight.docsGenerator.entities.LuaMethod;
import com.csykes.searchlight.docsGenerator.entities.PeripheralDocRecord;
import com.csykes.searchlight.docsGenerator.entities.PeripheralDocumentation;
import com.csykes.searchlight.docsGenerator.services.MarkdownService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
}
