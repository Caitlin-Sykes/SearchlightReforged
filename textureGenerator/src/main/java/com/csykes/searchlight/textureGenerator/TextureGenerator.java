package com.csykes.searchlight.textureGenerator;

import com.csykes.searchlight.textureGenerator.services.GeneratorService;
import cy.jdkdigital.dyenamics.core.util.DyenamicDyeColor;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TextureGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("Texture path must be provided as an argument");
            return;
        }
        GeneratorService generatorService = new GeneratorService(args[0]);
        log.info("Generating textures for: {}", generatorService.getTexturePath());
        Long numberGenerated = Arrays.stream(DyenamicDyeColor.values()).toList().parallelStream()
                .map(generatorService::generateWallLightTextures).filter(result -> result).count();
        log.info("Generated {} textures out of {} dye colours.", numberGenerated, DyenamicDyeColor.values().length);
    }
}