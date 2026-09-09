package com.csykes.searchlight.utils.lighting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;

@Getter
@AllArgsConstructor
public enum BrightnessStage implements StringRepresentable {
    OFF(0, "off", 0),
    LOW(1, "low", 4),
    MEDIUM(2, "medium", 8),
    HIGH(3, "high", 12),
    ULTRA(4, "ultra", 15);

    private final int id;
    private final String name;
    private final int lightLevel;

    @Override
    public String getSerializedName() {
        return name;
    }

    public static BrightnessStage fromId(int id) {
        for (BrightnessStage stage : values()) {
            if (stage.id == id) {
                return stage;
            }
        }
        return MEDIUM;
    }

    public BrightnessStage next() {
        return fromId(Math.min(4, id + 1));
    }

    public BrightnessStage previous() {
        return fromId(Math.max(0, id - 1));
    }
}