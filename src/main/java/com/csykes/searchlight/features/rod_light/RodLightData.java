package com.csykes.searchlight.features.rod_light;

import com.csykes.searchlight.features.edge_light.EdgeLightData;
import com.csykes.searchlight.utils.lighting.ColorAveragingHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates pixel mapping state for single rod lights (CornerLight and CentreLight).
 * Holds 8 sub-pixel segments along the rod axis.
 */
public class RodLightData {

    public static final int SUB_PIXELS = 8;

    private final EdgeLightData.EdgeState rodState = new EdgeLightData.EdgeState();

    public RodLightData() {
    }

    public RodLightData(RodLightData other) {
        this.rodState.load(other.rodState.save());
    }

    public EdgeLightData.EdgeState getRodState() {
        return rodState;
    }

    public void set(String color, boolean lit) {
        rodState.set(color, lit);
    }

    public void setSubPixel(int index, String color, boolean lit) {
        rodState.setSubPixel(index, color, lit);
    }

    public String getColor() {
        return rodState.getColor();
    }

    public boolean isLit() {
        return rodState.isLit();
    }

    public String getSubPixelColor(int index) {
        return rodState.getSubColor(index);
    }

    public boolean isSubPixelLit(int index) {
        return rodState.isSubLit(index);
    }

    public boolean isUniform() {
        return rodState.isUniform();
    }

    public boolean hasAnyLitSubPixel() {
        return rodState.hasAnyLitSubPixel();
    }

    public @Nullable String getAverageColor(BlockState state) {
        if (!hasAnyLitSubPixel()) {
            return null;
        }
        List<String> litColors = new ArrayList<>();
        for (int i = 0; i < SUB_PIXELS; i++) {
            if (rodState.isSubLit(i)) {
                litColors.add(rodState.getSubColor(i));
            }
        }
        return ColorAveragingHelper.calculateAverageColor(litColors);
    }

    public CompoundTag save() {
        return rodState.save();
    }

    public void load(CompoundTag tag) {
        rodState.load(tag);
    }
}
