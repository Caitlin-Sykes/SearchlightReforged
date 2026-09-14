package com.csykes.searchlight.docsGenerator.services;

import com.csykes.searchlight.docsGenerator.entities.BlockPeripheralPair;
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
                """.formatted(this.peripheral.getPeripheralClass().getFileName(), buildItemBadges(), this.peripheralDocumentation.description());
    }

    private String buildItemBadges() {
        return """
                @[%s]
                """.formatted(this.peripheral.getBlocks().stream().map(block -> String.format("%s", block)).collect(Collectors.joining(",")));
    }

    private String buildMethods() {
        return """
                ## Methods
                | Method | Parameters | Description |
                | --- | --- | --- |
                %s
                """.formatted(this.peripheralDocumentation.methods().stream().map(method -> String.format("| %s | %s | %s |", method.getName(), method.getParameters(), method.getDescription())).collect(Collectors.joining("\n")));
    }
}
