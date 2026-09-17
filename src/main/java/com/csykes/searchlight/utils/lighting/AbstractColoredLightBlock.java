package com.csykes.searchlight.utils.lighting;

import lombok.Getter;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class AbstractColoredLightBlock extends AbstractLightBlock implements ColoredLight {
    protected final DyeColor blockColor;
    protected final String dyenamicColor;

    public AbstractColoredLightBlock(@NotNull Properties properties, @Nullable DyeColor blockColor) {
        this(properties, blockColor, null);
    }

    public AbstractColoredLightBlock(@NotNull Properties properties, @Nullable String dyenamicColor) {
        this(properties, null, dyenamicColor);
    }

    public AbstractColoredLightBlock(@NotNull Properties properties, @Nullable DyeColor blockColor, @Nullable String dyenamicColor) {
        super(properties);
        this.blockColor = blockColor;
        this.dyenamicColor = dyenamicColor;
    }

    @Override
    public @Nullable Block getBlockForColor(String colorKey) {
        return null;
    }
}
