package com.csykes.searchlight.features.recipe;

import com.csykes.searchlight.Searchlight;
import com.mat.api.TestContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(Searchlight.MODID)
public class RecipeSanityGameTest {

    /**
     * Sanity test verifying that every registered Searchlight item has at least one recipe
     * that produces it as an output result.
     */
    @GameTest
    public static void testAllSearchlightItemsHaveRecipes(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        var level = helper.getLevel();
        var registryAccess = level.registryAccess();
        var recipeManager = level.getRecipeManager();

        // Collect all items produced by registered recipes
        Set<Item> craftableItems = new HashSet<>();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            try {
                ItemStack result = holder.value().getResultItem(registryAccess);
                if (result != null && !result.isEmpty() && result.getItem() != Items.AIR) {
                    craftableItems.add(result.getItem());
                }
            } catch (Exception ignored) {
                // Ignore recipes that do not support getResultItem without context
            }
        }

        // Get all items registered under searchlight namespace
        List<ResourceLocation> missingRecipeItems = new ArrayList<>();
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation itemId = entry.getKey().location();
            if (itemId.getNamespace().equals(Searchlight.MODID)) {
                Item item = entry.getValue();
                if (!craftableItems.contains(item)) {
                    missingRecipeItems.add(itemId);
                }
            }
        }

        context.assertThat(
                missingRecipeItems::isEmpty,
                "Expected all Searchlight items to have recipes, but the following items have no recipes: "
                        + missingRecipeItems.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "))
        );

        context.execute();
    }

    /**
     * Verifies that each light category contains craftable recipes across all registered color variants.
     */
    @GameTest
    public static void testCategoryRecipeCompleteness(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        var level = helper.getLevel();
        var registryAccess = level.registryAccess();
        var recipeManager = level.getRecipeManager();

        Set<Item> craftableItems = recipeManager.getRecipes().stream()
                .map(holder -> {
                    try {
                        return holder.value().getResultItem(registryAccess).getItem();
                    } catch (Exception e) {
                        return Items.AIR;
                    }
                })
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toSet());

        // Check Wall Lights
        Searchlight.WALL_LIGHT_ITEMS.forEach((variant, deferredItem) -> {
            Item item = deferredItem.get();
            if (!craftableItems.contains(item)) {
                throw new GameTestAssertException("Missing recipe for wall light variant: " + variant);
            }
        });

        // Check Corner Lights
        Searchlight.CORNER_LIGHTS_ITEMS.forEach((variant, deferredItem) -> {
            Item item = deferredItem.get();
            if (!craftableItems.contains(item)) {
                throw new GameTestAssertException("Missing recipe for corner light variant: " + variant);
            }
        });

        // Check Centre Lights
        Searchlight.CENTRE_LIGHTS_ITEMS.forEach((variant, deferredItem) -> {
            Item item = deferredItem.get();
            if (!craftableItems.contains(item)) {
                throw new GameTestAssertException("Missing recipe for centre light variant: " + variant);
            }
        });

        // Check Edge Lights
        Searchlight.EDGE_LIGHTS_ITEMS.forEach((variant, deferredItem) -> {
            Item item = deferredItem.get();
            if (!craftableItems.contains(item)) {
                throw new GameTestAssertException("Missing recipe for edge light variant: " + variant);
            }
        });

        // Check Colour Lamps
        Searchlight.COLOUR_LAMP_ITEMS.forEach((variant, deferredItem) -> {
            Item item = deferredItem.get();
            if (!craftableItems.contains(item)) {
                throw new GameTestAssertException("Missing recipe for colour lamp variant: " + variant);
            }
        });

        // Check Colour Lamp Slabs
        Searchlight.COLOUR_SLAB_ITEMS.forEach((variant, deferredItem) -> {
            Item item = deferredItem.get();
            if (!craftableItems.contains(item)) {
                throw new GameTestAssertException("Missing recipe for colour lamp slab variant: " + variant);
            }
        });

        // Check Searchlights
        Searchlight.SEARCHLIGHT_ITEMS.forEach((variant, deferredItem) -> {
            Item item = deferredItem.get();
            if (!craftableItems.contains(item)) {
                throw new GameTestAssertException("Missing recipe for searchlight variant: " + variant);
            }
        });

        // Check CC: Tweaked items if registered
        if (Searchlight.LIGHTING_DIRECTOR_ITEM != null) {
            Item item = Searchlight.LIGHTING_DIRECTOR_ITEM.get();
            if (!craftableItems.contains(item)) {
                throw new GameTestAssertException("Missing recipe for lighting director");
            }
        }
        if (Searchlight.LIGHTING_LINKER_CARD != null) {
            Item item = Searchlight.LIGHTING_LINKER_CARD.get();
            if (!craftableItems.contains(item)) {
                throw new GameTestAssertException("Missing recipe for lighting linker card");
            }
        }

        context.execute();
    }
}
