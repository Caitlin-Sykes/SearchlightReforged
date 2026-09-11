package com.csykes.searchlight.docsGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DocsGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("Texture path must be provided as an argument");
            return;
        }
        log.info("Generating docs");
        log.info("Done");
    }
}