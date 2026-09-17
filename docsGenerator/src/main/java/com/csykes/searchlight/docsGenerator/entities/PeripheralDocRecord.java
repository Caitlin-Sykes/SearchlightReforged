package com.csykes.searchlight.docsGenerator.entities;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record PeripheralDocRecord(
        Path peripheralClass,
        Map<String, List<String>> blockEntitiesToBlocks
) {
}
