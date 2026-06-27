package com.csykes.searchlight.utils.lighting;

import lombok.AllArgsConstructor;
import net.minecraft.util.StringRepresentable;

@AllArgsConstructor
public enum LightRodConnection implements StringRepresentable {
    SINGLE("single"),
    BOTTOM("bottom"),
    MIDDLE("middle"),
    TOP("top");

    private final String name;

    @Override
    public String getSerializedName() {
        return name;
    }
}