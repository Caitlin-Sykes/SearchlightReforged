package com.csykes.searchlight.recipe;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.utils.SearchlightUtil;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class LampBrightnessRecipe extends CustomRecipe {
    public LampBrightnessRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack lampStack = ItemStack.EMPTY;
        int lampCount = 0;
        int glowstoneCount = 0;
        int redstoneCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack slot = input.getItem(i);
            if (slot.isEmpty()) {
                continue;
            }

            if (isLamp(slot)) {
                lampCount++;
                lampStack = slot;
            } else if (slot.is(Items.GLOWSTONE_DUST)) {
                glowstoneCount++;
            } else if (slot.is(Items.REDSTONE)) {
                redstoneCount++;
            } else {
                return false;
            }
        }

        if (lampCount != 1) {
            return false;
        }

        if (glowstoneCount == 0 && redstoneCount == 0) {
            return false;
        }

        if (glowstoneCount > 0 && redstoneCount > 0) {
            return false;
        }

        BrightnessStage currentStage = SearchlightUtil.getBrightness(lampStack);
        if (glowstoneCount > 0) {
            return currentStage.getId() < BrightnessStage.ULTRA.getId();
        } else {
            return currentStage.getId() > BrightnessStage.OFF.getId();
        }
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack lampStack = ItemStack.EMPTY;
        int glowstoneCount = 0;
        int redstoneCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack slot = input.getItem(i);
            if (slot.isEmpty()) {
                continue;
            }

            if (isLamp(slot)) {
                lampStack = slot;
            } else if (slot.is(Items.GLOWSTONE_DUST)) {
                glowstoneCount++;
            } else if (slot.is(Items.REDSTONE)) {
                redstoneCount++;
            }
        }

        if (lampStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        BrightnessStage currentStage = SearchlightUtil.getBrightness(lampStack);
        BrightnessStage targetStage;

        if (glowstoneCount > 0) {
            int newId = Math.min(BrightnessStage.ULTRA.getId(), currentStage.getId() + glowstoneCount);
            targetStage = BrightnessStage.fromId(newId);
        } else if (redstoneCount > 0) {
            int newId = Math.max(BrightnessStage.OFF.getId(), currentStage.getId() - redstoneCount);
            targetStage = BrightnessStage.fromId(newId);
        } else {
            return ItemStack.EMPTY;
        }

        ItemStack result = lampStack.copyWithCount(1);
        SearchlightUtil.setBrightness(result, targetStage);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Searchlight.LAMP_BRIGHTNESS_RECIPE_SERIALIZER.get();
    }

    private boolean isLamp(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof AbstractLightBlock;
    }
}
