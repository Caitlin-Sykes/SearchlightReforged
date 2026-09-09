package com.csykes.searchlight.utils.lighting;

import com.csykes.searchlight.integration.dyenamics.DyenamicHelper;
import net.minecraft.world.item.DyeColor;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility for color averaging and nearest-palette matching across vanilla dye colors
 * and Dyenamics colors (if present).
 */
public class ColorAveragingHelper {

    /**
     * Resolves the packed ARGB integer for a given color name.
     */
    public static int getRgbForColor(@Nullable String colorName) {
        if (colorName == null || colorName.isBlank()) {
            return 0xFF000000 | DyeColor.WHITE.getTextureDiffuseColor();
        }
        String normalized = colorName.toLowerCase(Locale.ROOT).trim();

        DyeColor standard = DyeColor.byName(normalized, null);
        if (standard != null) {
            return 0xFF000000 | standard.getTextureDiffuseColor();
        }

        if (ModList.get().isLoaded("dyenamics")) {
            int dyenamic = DyenamicHelper.getDyenamicColor(normalized);
            if (dyenamic != -1) {
                return dyenamic;
            }
        }

        return 0xFF000000 | DyeColor.WHITE.getTextureDiffuseColor();
    }

    /**
     * Retrieves all available color names and their ARGB values.
     * Respects whether Dyenamics is loaded.
     */
    public static Map<String, Integer> getAllAvailableColors() {
        Map<String, Integer> colors = new LinkedHashMap<>();

        // Add 16 vanilla dye colors
        for (DyeColor color : DyeColor.values()) {
            colors.put(color.getName(), 0xFF000000 | color.getTextureDiffuseColor());
        }

        // Add Dyenamics colors if loaded
        if (ModList.get().isLoaded("dyenamics")) {
            colors.putAll(DyenamicHelper.getAllDyenamicColors());
        }

        return colors;
    }

    /**
     * Finds the closest registered color name for the given RGB components
     * using weighted Euclidean distance in RGB color space.
     */
    public static @NotNull String findClosestColor(int r, int g, int b) {
        Map<String, Integer> palette = getAllAvailableColors();

        String bestColor = "white";
        long minDistanceSq = Long.MAX_VALUE;

        for (Map.Entry<String, Integer> entry : palette.entrySet()) {
            int rgb = entry.getValue();
            int cr = (rgb >> 16) & 0xFF;
            int cg = (rgb >> 8) & 0xFF;
            int cb = rgb & 0xFF;

            long dr = r - cr;
            long dg = g - cg;
            long db = b - cb;

            // Perceptually weighted Euclidean distance (accounting for eye sensitivity)
            long distSq = 2 * dr * dr + 4 * dg * dg + 3 * db * db;
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                bestColor = entry.getKey();
            }
        }

        return bestColor;
    }

    /**
     * Calculates the average color from a collection of color names and returns
     * the closest registered color variant name.
     *
     * @param colorNames Collection of color name strings (e.g. "red", "yellow").
     * @return Closest color name, or null if colorNames is empty.
     */
    public static @Nullable String calculateAverageColor(@Nullable Collection<String> colorNames) {
        if (colorNames == null || colorNames.isEmpty()) {
            return null;
        }

        long sumR = 0;
        long sumG = 0;
        long sumB = 0;
        int count = 0;

        for (String name : colorNames) {
            if (name == null || name.isBlank()) continue;
            int rgb = getRgbForColor(name);
            sumR += (rgb >> 16) & 0xFF;
            sumG += (rgb >> 8) & 0xFF;
            sumB += rgb & 0xFF;
            count++;
        }

        if (count == 0) {
            return null;
        }

        int avgR = (int) (sumR / count);
        int avgG = (int) (sumG / count);
        int avgB = (int) (sumB / count);

        return findClosestColor(avgR, avgG, avgB);
    }
}
