package com.csykes.searchlight.docsGenerator;

import cy.jdkdigital.dyenamics.core.util.DyenamicDyeColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DocsGeneratorTest {

    @Test
    @DisplayName("main with no arguments handles gracefully without throwing exceptions")
    void testMainWithoutArguments() {
        assertDoesNotThrow(() -> com.csykes.searchlight.textureGenerator.DocsGenerator.main(new String[0]));
    }
}
