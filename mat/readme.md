# MAT (Minecraft Automated Testing)

A fluent, Playwright-inspired end-to-end integration testing library for Minecraft NeoForge 1.21+ (Java 21).

## Architecture & Packages

### 1. API (`com.mat.api`)

- `TestContext`: Fluent test execution runner wrapping NeoForge's `GameTestHelper`. Handles relative placement, action
  pipelines, assertions, and tick scheduling.
- `BlockHandle`: Pointer/locator targeting relative `BlockPos` with chainable action methods (`rightClick()`,
  `rightClickWithItem()`, `waitTicks()`), block entity queries (`getBlockEntity(Class<T>)`, `verifyBlockEntity(...)`),
  block capability inspection (`getCapability(...)`, `verifyCapability(...)`), and property assertions.

### 2. Programmatic Structure Generation (`com.mat.structure`)

- `StructureBuilder`: Programmatic builder for creating Minecraft structure `.nbt` templates without needing in-game
  structure blocks or external NBT editors. Supports custom palettes, dimensions, block properties, and block entity
  NBT:
  ```java
  // Generate empty structure
  StructureBuilder.empty("empty3x3x3", 3, 3, 3)
      .writeToResourceDirectory(outputDir, "searchlight");

  // Generate structure with initial blocks
  StructureBuilder.create("custom_room", 5, 5, 5)
      .setBlock(0, 0, 0, "minecraft:stone")
      .writeTo(filePath);
  ```
- `MatStructureGenerator`: Automated scanner and CLI tool that discovers `@GameTest` annotations and generates all
  required `.nbt` templates programmatically during build time.

### 3. Execution & Simulation Engines

- `com.mat.engine.SimulatedPlayerEngine`: Uses NeoForge's `FakePlayerFactory` to simulate player block interactions with
  proper raytracing and hand handling.
- `com.mat.placement.BlockPlacer`: Manages non-overlapping relative coordinate allocation and block placement.
- `com.mat.scheduler.TickScheduler`: Manages the server tick timeline and sequential action dispatch.
- `com.mat.assertion.BlockAssertions`: Assertion validation for block IDs, properties, and colors.

## Example Integration Test

```java

@GameTestHolder("searchlight")
public class CCIntegrationTest {

    private static final BlockCapability<IPeripheral, Direction> PERIPHERAL_CAPABILITY =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("computercraft", "peripheral"), IPeripheral.class);

    @GameTest
    public static void testLightPeripheral(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockHandle handle = context.placeBlock("searchlight:searchlight_white");

        // Verify capability attachment
        handle.verifyCapability(PERIPHERAL_CAPABILITY, Direction.NORTH, peripheral -> {
            context.assertThat(() -> "search_light".equals(peripheral.getType()), "Expected 'search_light' peripheral");
        });

        // Test typed BlockEntity interactions
        handle.verifyBlockEntity(SearchlightBlockEntity.class, be -> {
            LightPeripheral peripheral = new LightPeripheral(be, "search_light");
            peripheral.setBrightness(4);
            context.assertThat(() -> peripheral.getBrightness() == 4, "Expected brightness 4");
        });

        context.execute();
    }
}
```

### To run:

Options:

- Launch your minecraft instance
- run /test [runAll, run]

Non:CI:
.\gradlew runGameTestServer

!!!! IF ran in a world, it does span structures!!!!
