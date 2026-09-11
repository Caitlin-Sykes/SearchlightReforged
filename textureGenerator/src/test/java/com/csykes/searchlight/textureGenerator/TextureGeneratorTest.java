package com.csykes.searchlight.textureGenerator;

import cy.jdkdigital.dyenamics.core.util.DyenamicDyeColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TextureGeneratorTest {

    @Test
    @DisplayName("main with no arguments handles gracefully without throwing exceptions")
    void testMainWithoutArguments() {
        assertDoesNotThrow(() -> TextureGenerator.main(new String[0]));
    }

    @Test
    @DisplayName("main generates wall light textures for all colours into target directory")
    void testMainGeneratesAllTextures(@TempDir Path tempDir) {
        TextureGenerator.main(new String[]{tempDir.toString()});

        File blockDir = tempDir.resolve("block").toFile();
        assertTrue(blockDir.exists());
        assertTrue(blockDir.isDirectory());

        for (DyenamicDyeColor dye : DyenamicDyeColor.values()) {
            File textureFile = new File(blockDir, "wall_light_" + dye.getSerializedName() + ".png");
            assertTrue(textureFile.exists(), "Expected " + textureFile.getName() + " to be generated");
            assertTrue(textureFile.length() > 0, "Expected " + textureFile.getName() + " not to be empty");
        }
    }
}
