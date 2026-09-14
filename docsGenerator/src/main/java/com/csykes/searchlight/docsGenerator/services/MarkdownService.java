package com.csykes.searchlight.docsGenerator.services;

import com.csykes.searchlight.docsGenerator.entities.BlockPeripheralPair;
import com.csykes.searchlight.docsGenerator.entities.LuaMethod;
import com.csykes.searchlight.docsGenerator.entities.PeripheralDocumentation;
import lombok.AllArgsConstructor;

import java.util.stream.Collectors;

@AllArgsConstructor
public class MarkdownService {
    private BlockPeripheralPair peripheral;
    private PeripheralDocumentation peripheralDocumentation;

    public String buildPage() {
        StringBuilder markdown = new StringBuilder();
        markdown.append(buildHeader());
        markdown.append(buildMethods());
        return markdown.toString();
    }

    private String buildHeader() {
        return """
                # %s - CC:Tweaked API
                %s
                ---
                %s
                """.formatted(this.peripheral.getPeripheralClass().getFileName().toString().replaceFirst("[.][^.]+$", ""), buildItemBadges(), this.peripheralDocumentation.description());
    }

    private String buildItemBadges() {
        return """
                @[%s]
                """.formatted(this.peripheral.getBlocks().stream().map(block -> String.format("%s", block)).collect(Collectors.joining(",")));
    }

    private String buildMethods() {
        return """
                ## Methods
                %s
                """.formatted(this.peripheralDocumentation.methods().stream().map(this::buildMethod).collect(Collectors.joining("\n")));
    }

    private String buildMethod(LuaMethod method) {
        String parameters = method.getParameters().stream().map(parameter -> "| %s | %s | %s |".formatted(escapeMarkdown(parameter.getName()), escapeMarkdown(parameter.getType()), escapeMarkdown(parameter.getDescription()).replace("\n", " "))).collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder("""
                ### %s
                %s
                """.formatted(escapeMarkdown(method.getName()), escapeMarkdown(method.getDescription())));
        if (!method.getParameters().isEmpty()) {
            sb.append("""
                    
                    | Parameter | Type | Description |
                    |-----------|------|-------------|
                    %s
                    """.formatted(parameters));
        }
        return sb.toString();
    }

    private static String escapeMarkdown(String value) {
        return value.replace("|", "\\|").replaceAll("\\R\\s+", "\n").trim().replaceAll("\\{@code\\s+([^}]+)\\}", "`$1`").replace("<", "&lt;").replace(">", "&gt;").replace("(", "&#40;").replace(")", "&#41;").replace("[", "&#91;").replace("]", "&#93;").replace("{", "&#123;").replace("}", "&#125;").replace(":", "&#58;").replace("?", "&#63;");
    }
}
