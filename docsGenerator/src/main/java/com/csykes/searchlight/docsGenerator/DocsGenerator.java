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
            peripheralBEs.forEach(peripheral -> {
                MarkdownService markdownService = new MarkdownService(peripheral, peripheralMethods.get(peripheral.getPeripheralClass()));
                try {
                    FileWriter fw = new FileWriter("docs/generated/" + peripheral.getPeripheralClass().getFileName().toString().replaceFirst("[.][^.]+$", "") + ".md");
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