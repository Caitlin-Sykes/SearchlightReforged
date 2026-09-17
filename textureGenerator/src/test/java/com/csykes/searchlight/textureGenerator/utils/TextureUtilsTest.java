package com.csykes.searchlight.textureGenerator.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class TextureUtilsTest {

    @Test
    @DisplayName("drawRectangle fills the specified bounds with the given color")
    void testDrawRectangle() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int expectedColor = 0xFF123456;

        TextureUtils.drawRectangle(image, 2, 3, 4, 5, expectedColor);

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                if (x >= 2 && x < 2 + 4 && y >= 3 && y < 3 + 5) {
                    assertEquals(expectedColor, image.getRGB(x, y),
                            "Pixel at (" + x + ", " + y + ") should be filled with expected color");
                } else {
                    assertEquals(0, image.getRGB(x, y),
                            "Pixel at (" + x + ", " + y + ") outside bounds should remain transparent");
                }
            }
        }
    }

    @Test
    @DisplayName("drawTexturedRectangle applies bounded noise to RGB channels while preserving alpha")
    void testDrawTexturedRectangleNoiseAndAlpha() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int baseColor = 0xFF808080;
        float noiseAmplitude = 0.075f;

        TextureUtils.drawTexturedRectangle(image, 1, 1, 6, 6, baseColor, noiseAmplitude);

        int minExpected = (int) (128 * (1.0f - noiseAmplitude));
        int maxExpected = (int) (128 * (1.0f + noiseAmplitude));

        for (int x = 1; x < 7; x++) {
            for (int y = 1; y < 7; y++) {
                int rgb = image.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                assertEquals(255, alpha, "Alpha should remain 0xFF");
                assertTrue(red >= minExpected && red <= maxExpected, "Red channel with noise");
                assertTrue(green >= minExpected && green <= maxExpected, "Green channel with noise");
                assertTrue(blue >= minExpected && blue <= maxExpected, "Blue channel with noise");
            }
        }
    }

    @Test
    @DisplayName("drawTexturedRectangle produces deterministic output with seeded random")
    void testDrawTexturedRectangleDeterminism() {
        BufferedImage image1 = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage image2 = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        int testColor = 0xFFFFAA33;
        TextureUtils.drawTexturedRectangle(image1, 0, 0, 8, 8, testColor, 0.075f);
        TextureUtils.drawTexturedRectangle(image2, 0, 0, 8, 8, testColor, 0.075f);

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                assertEquals(image1.getRGB(x, y), image2.getRGB(x, y));
            }
        }
    }
}
