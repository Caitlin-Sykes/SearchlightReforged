package com.csykes.searchlight.integration.jade;

import com.csykes.searchlight.Searchlight;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import static com.csykes.searchlight.utils.lighting.AbstractLightBlock.*;

public enum LightDataComponent implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    public static final String SERVERDATA_COLOR = "color";
    public static final String SERVERDATA_BRIGHTNESS = "brightness";
    public static final String SERVERDATA_MODE = "mode";

    @Override
    public void appendTooltip(
            ITooltip tooltip,
            BlockAccessor accessor,
            IPluginConfig config
    ) {
        if (accessor.getServerData().contains(SERVERDATA_COLOR))
            tooltip.add(Component.translatable("searchlight.jade.color", formatName(accessor.getServerData().getString(SERVERDATA_COLOR))));
        if (accessor.getServerData().contains(SERVERDATA_BRIGHTNESS))
            tooltip.add(Component.translatable("searchlight.jade.brightness", formatName(accessor.getServerData().getString(SERVERDATA_BRIGHTNESS))));
        if (accessor.getServerData().contains(SERVERDATA_MODE))
            tooltip.add(Component.translatable("searchlight.jade.mode", formatName(accessor.getServerData().getString(SERVERDATA_MODE))));
    }

    private static String formatName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockState light = accessor.getBlockEntity().getBlockState();
        if (light.hasProperty(COLOR))
            data.putString(SERVERDATA_COLOR, light.getValue(COLOR).getName());
        if (light.hasProperty(BRIGHTNESS))
            data.putString(SERVERDATA_BRIGHTNESS, light.getValue(BRIGHTNESS).getName());
        if (light.hasProperty(LIGHT_REQUEST))
            data.putString(SERVERDATA_MODE, light.getValue(LIGHT_REQUEST).getName());
    }

    @Override
    public ResourceLocation getUid() {
        return Searchlight.LIGHT_DATA_COMPONENT;
    }

}
