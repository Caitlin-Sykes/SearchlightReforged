package com.csykes.searchlight.textureGenerator.services;

import cy.jdkdigital.dyenamics.core.util.DyenamicDyeColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorServiceTest {

    @Test
    @DisplayName("generateWallLightTextures writes a 16x16 PNG with the expected zones and colors")
    void testGenerateWallLightTextures(@TempDir Path tempDir) throws IOException {
        GeneratorService service = new GeneratorService(tempDir.toString());

        DyenamicDyeColor testDye = DyenamicDyeColor.PEACH;
        boolean result = service.generateWallLightTextures(testDye);

        assertTrue(result);

        File expectedFile = tempDir.resolve("block").resolve("wall_light_" + testDye.getSerializedName() + ".png").toFile();
        assertTrue(expectedFile.exists());
        assertTrue(expectedFile.length() > 0);

        BufferedImage image = ImageIO.read(expectedFile);
        assertNotNull(image);
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());

        // Verify white lamp zone at (1, 1) has 0xFFFFFFFF
        assertEquals(0xFFFFFFFF, image.getRGB(1, 1));
        assertEquals(0xFFFFFFFF, image.getRGB(7, 1));

        // Verify coloured lamp zone at (0, 0) has alpha 255
        int rgb = image.getRGB(0, 0);
        int alpha = (rgb >> 24) & 0xFF;
        assertEquals(255, alpha);
    }

    @Test
    @DisplayName("generateWallLightTextures handles all available Dyenamics colours successfully")
    void testGenerateAllDyeColors(@TempDir Path tempDir) {
        GeneratorService service = new GeneratorService(tempDir.toString());

        for (DyenamicDyeColor dye : DyenamicDyeColor.values()) {
            boolean success = service.generateWallLightTextures(dye);
            assertTrue(success, "Generating texture for " + dye.getSerializedName() + " should succeed");

            File generated = tempDir.resolve("block").resolve("wall_light_" + dye.getSerializedName() + ".png").toFile();
            assertTrue(generated.exists());
        }
    }
}
