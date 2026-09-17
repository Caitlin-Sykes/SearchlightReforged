package com.csykes.searchlight.features.colour_lamp_slab;

import com.csykes.searchlight.Searchlight;
import com.mat.api.BlockHandle;
import com.mat.api.TestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.List;

@GameTestHolder(Searchlight.MODID)
public class ColourLampSlabGameTest {

    /**
     * Tests that a newly placed slab defaults to a bottom slab (TOP_HALF = false).
     */
    @GameTest
    public static void testBottomSlabPlacement(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockPos pos = new BlockPos(1, 1, 1);
        BlockHandle slab = context.placeBlock(pos, "searchlight:colour_lamp_slab_white");

        context.assertThat(
                () -> !slab.getBlockState().getValue(ColourLampSlabBlock.TOP_HALF),
                "Expected slab placed on the floor to be a bottom half (TOP_HALF = false)"
        );

        context.execute();
    }

    /**
     * Tests that placing a matching slab into an existing bottom slab swaps it to a full ColourLampBlock.
     */
    @GameTest
    public static void testDoubleSlabPlacementSwapsToFullBlock(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockPos pos = new BlockPos(1, 1, 1);
        BlockHandle slab = context.placeBlock(pos, "searchlight:colour_lamp_slab_white");

        slab.assertBlockId("searchlight:colour_lamp_slab_white");

        context.assertThat(
                () -> !slab.getBlockState().getValue(ColourLampSlabBlock.TOP_HALF),
                "Expected slab to initially be a bottom slab"
        );

        // Right-clicking top of the bottom slab with matching slab item should trigger double-slab swap
        slab.rightClickWithItem("searchlight:colour_lamp_slab_white", Direction.UP)
                .waitTicks(1)
                .assertBlockId("searchlight:colour_lamp_white");

        context.execute();
    }

    /**
     * Tests placing adjacent slabs of the same half and verifying connection properties.
     */
    @GameTest
    public static void testSlabAdjacencyConnectionsDifferentColour(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockPos pos1 = new BlockPos(1, 1, 1);
        BlockPos pos2 = new BlockPos(1, 1, 2);

        BlockHandle slab1 = context.placeBlock(pos1, "searchlight:colour_lamp_slab_white");
        BlockHandle slab2 = context.placeBlock(pos2, "searchlight:colour_lamp_slab_red");

        context.assertThat(
                () -> slab1.getBlockState().getValue(ColourLampSlabBlock.SOUTH),
                "Expected first slab to be connected SOUTH to adjacent slab of same half"
        );

        context.assertThat(
                () -> slab2.getBlockState().getValue(ColourLampSlabBlock.NORTH),
                "Expected second slab to be connected NORTH to adjacent slab of same half"
        );

        context.execute();
    }

    /**
     * Tests placing adjacent slabs of the same half and verifying connection properties.
     */
    @GameTest
    public static void testSlabAdjacencyConnections(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockPos pos1 = new BlockPos(1, 1, 1);
        BlockPos pos2 = new BlockPos(1, 1, 2);

        BlockHandle slab1 = context.placeBlock(pos1, "searchlight:colour_lamp_slab_white");
        BlockHandle slab2 = context.placeBlock(pos2, "searchlight:colour_lamp_slab_white");

        context.assertThat(
                () -> slab1.getBlockState().getValue(ColourLampSlabBlock.SOUTH),
                "Expected first slab to be connected SOUTH to adjacent slab of same half"
        );

        context.assertThat(
                () -> slab2.getBlockState().getValue(ColourLampSlabBlock.NORTH),
                "Expected second slab to be connected NORTH to adjacent slab of same half"
        );

        context.execute();
    }

    /**
     * Tests crafting a full ColourLampBlock from two vertical ColourLampSlab items.
     */
    @GameTest
    public static void testColourLampFromSlabsRecipe(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        var slabItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("searchlight", "colour_lamp_slab_white"));
        var expectedLampItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("searchlight", "colour_lamp_white"));

        CraftingInput input = CraftingInput.of(1, 2, List.of(new ItemStack(slabItem), new ItemStack(slabItem)));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());

        context.assertThat(
                recipe::isPresent,
                "Expected a crafting recipe to match 2 vertical white colour lamp slabs"
        );

        ItemStack result = recipe.get().value().assemble(input, helper.getLevel().registryAccess());
        context.assertThat(
                () -> result.is(expectedLampItem) && result.getCount() == 1,
                "Expected crafting result to be 1 white colour lamp"
        );

        context.execute();
    }
}
