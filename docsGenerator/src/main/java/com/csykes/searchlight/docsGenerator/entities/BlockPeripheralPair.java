package com.csykes.searchlight.docsGenerator.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;
import java.util.List;

@Data
@AllArgsConstructor
public class BlockPeripheralPair {
    private String blockId;
    private Path peripheralClass;
    private List<String> blocks;
}
