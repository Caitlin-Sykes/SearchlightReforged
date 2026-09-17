package com.csykes.searchlight.utils.lighting;

import lombok.AllArgsConstructor;
import net.minecraft.util.StringRepresentable;

@AllArgsConstructor
public enum CornerLightStage implements StringRepresentable {
    BOTTOM_LEFT("bottom_left"),
    BOTTOM_RIGHT("bottom_right"),
    TOP_RIGHT("top_right"),
    TOP_LEFT("top_left");

    private final String name;

    @Override
    public String getSerializedName() {
        return this.name;
    }
}