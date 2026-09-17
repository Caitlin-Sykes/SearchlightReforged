package com.csykes.searchlight.textureGenerator.services;

import com.csykes.searchlight.textureGenerator.utils.TextureUtils;
import cy.jdkdigital.dyenamics.core.util.DyenamicDyeColor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Data
@AllArgsConstructor
public class GeneratorService {
    private String texturePath;

    private static final float NOISE_AMPLITUDE = 0.075f;

    public boolean generateWallLightTextures(DyenamicDyeColor dye) {
        log.info(String.format("%s - #%06x", dye.getSerializedName(), dye.getFireworkColor()));
        try {
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            // Coloured Lamp Zone
            TextureUtils.drawTexturedRectangle(image, 0, 0, 6, 6, 0xff000000 | dye.getFireworkColor(), NOISE_AMPLITUDE);
            TextureUtils.drawTexturedRectangle(image, 6, 0, 10, 2, 0xff000000 | dye.getFireworkColor(), NOISE_AMPLITUDE);
            TextureUtils.drawTexturedRectangle(image, 12, 2, 4, 2, 0xff000000 | dye.getFireworkColor(), NOISE_AMPLITUDE);

            // White Lamp Zone
            TextureUtils.drawRectangle(image, 1, 1, 4, 1, 0xffffffff);
            TextureUtils.drawRectangle(image, 7, 1, 4, 1, 0xffffffff);

            File targetFile = Path.of(texturePath, "block", "wall_light_" + dye.getSerializedName() + ".png").toFile();
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            ImageIO.write(image, "png", targetFile);
        } catch (IOException e) {
            log.error("Failed to generate texture for dye: {}", dye.getSerializedName(), e);
            return false;
        }
        return true;
    }

}
