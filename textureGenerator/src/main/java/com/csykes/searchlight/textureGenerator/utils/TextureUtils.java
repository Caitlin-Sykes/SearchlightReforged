package com.csykes.searchlight.textureGenerator.utils;

import java.awt.image.BufferedImage;
import java.util.Random;

public class TextureUtils {
    private static final long NOISE_SEED = 0xCAFEBABEL;

    public static void drawRectangle(BufferedImage image, int x, int y, int width, int height, int colour) {
        for (int _x = x; _x < x + width; _x++) {
            for (int _y = y; _y < y + height; _y++) {
                image.setRGB(_x, _y, colour);
            }
        }
    }

    public static void drawTexturedRectangle(BufferedImage image, int x, int y, int width, int height, int colour, float noiseAmplitude) {
        Random random = new Random(NOISE_SEED);
        for (int _x = x; _x < x + width; _x++) {
            for (int _y = y; _y < y + height; _y++) {

                float noiseValue = (1 - noiseAmplitude) + random.nextFloat() * noiseAmplitude * 2;

                int alpha = (colour >> 24) & 0xFF;
                int red = Math.clamp((int) (((colour >> 16) & 0xFF) * noiseValue), 0, 255);
                int green = Math.clamp((int) (((colour >> 8) & 0xFF) * noiseValue), 0, 255);
                int blue = Math.clamp((int) ((colour & 0xFF) * noiseValue), 0, 255);

                int mixedColour = (alpha << 24) | (red << 16) | (green << 8) | blue;

                image.setRGB(_x, _y, mixedColour);
            }
        }
    }
}
