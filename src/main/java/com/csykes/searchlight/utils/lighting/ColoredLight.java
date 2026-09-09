package com.csykes.searchlight.utils.lighting;

import com.csykes.searchlight.integration.dyenamics.DyenamicHelper;
import net.minecraft.world.item.DyeColor;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public interface ColoredLight {
    @Nullable
    DyeColor getBlockColor();

    @Nullable
    String getDyenamicColor();

    @Nullable
    default String getColorKey() {
        if (getBlockColor() != null) {
            return getBlockColor().getName();
        }
        return getDyenamicColor();
    }

    default int getDiffuseColor() {
        if (getBlockColor() != null) {
            return getBlockColor().getTextureDiffuseColor();
        }
        if (ModList.get().isLoaded("dyenamics") && getDyenamicColor() != null) {
            return DyenamicHelper.getDyenamicColor(getDyenamicColor());
        }
        return -1;
    }
}
