package com.csykes.searchlight.docsGenerator;

import com.csykes.searchlight.docsGenerator.services.JavaParsingService;
import com.csykes.searchlight.docsGenerator.services.MarkdownService;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class DocsGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("Texture path must be provided as an argument");
            return;
        }
        log.info("Generating docs");
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
                    FileWriter fw = new FileWriter("docs/generated/" + peripheral.peripheralClass().getFileName().toString().replaceFirst("[.][^.]+$", "") + ".md");
                    fw.write(markdownService.buildPage());
                    fw.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            log.error("File not found", e);
        }
        log.info("Done");
    }
}