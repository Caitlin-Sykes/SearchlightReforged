package com.csykes.searchlight.features.edge_light;

import com.csykes.searchlight.utils.lighting.ColorAveragingHelper;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates pixel mapping state for an {@link EdgeLightBlock}.
 * Handles 4 primary edges (NORTH, EAST, SOUTH, WEST), each prepared
 * for up to 8 sub-pixel segments while treating overlapping corners cleanly.
 */
public class EdgeLightData {

    public static final Direction[] HORIZONTALS = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    public static final int SUB_PIXELS_PER_EDGE = 8;

    public static class EdgeState {
        @Getter
        private String color = "white";
        @Getter
        private boolean lit = true;
        private final String[] subColors = new String[SUB_PIXELS_PER_EDGE];
        private final boolean[] subLit = new boolean[SUB_PIXELS_PER_EDGE];

        public EdgeState() {
            Arrays.fill(subColors, "white");
            Arrays.fill(subLit, true);
        }

        public EdgeState(EdgeState other) {
            this.color = other.color;
            this.lit = other.lit;
            System.arraycopy(other.subColors, 0, this.subColors, 0, SUB_PIXELS_PER_EDGE);
            System.arraycopy(other.subLit, 0, this.subLit, 0, SUB_PIXELS_PER_EDGE);
        }

        public boolean hasAnyLitSubPixel() {
            for (boolean b : subLit) {
                if (b) return true;
            }
            return false;
        }

        public boolean isUniform() {
            String firstColor = subColors[0];
            boolean firstLit = subLit[0];
            for (int i = 1; i < SUB_PIXELS_PER_EDGE; i++) {
                if (subLit[i] != firstLit || !Objects.equals(subColors[i], firstColor)) {
                    return false;
                }
            }
            return true;
        }

        public void set(String color, boolean lit) {
            this.color = (color != null && !color.isBlank()) ? color.toLowerCase(Locale.ROOT).trim() : "white";
            this.lit = lit;
            Arrays.fill(this.subColors, this.color);
            Arrays.fill(this.subLit, this.lit);
        }

        public void setSubPixel(int index, String color, boolean lit) {
            if (index >= 0 && index < SUB_PIXELS_PER_EDGE) {
                String c = (color != null && !color.isBlank()) ? color.toLowerCase(Locale.ROOT).trim() : "white";
                this.subColors[index] = c;
                this.subLit[index] = lit;
                this.lit = hasAnyLitSubPixel();
                this.color = c;
            }
        }

        public String getSubColor(int index) {
            return (index >= 0 && index < SUB_PIXELS_PER_EDGE) ? subColors[index] : color;
        }

        public boolean isSubLit(int index) {
            return (index >= 0 && index < SUB_PIXELS_PER_EDGE) ? subLit[index] : lit;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("color", color);
            tag.putBoolean("lit", lit);

            ListTag subColorList = new ListTag();
            for (String sc : subColors) {
                subColorList.add(StringTag.valueOf(sc != null ? sc : "white"));
            }
            tag.put("sub_colors", subColorList);

            byte[] litBytes = new byte[SUB_PIXELS_PER_EDGE];
            for (int i = 0; i < SUB_PIXELS_PER_EDGE; i++) {
                litBytes[i] = (byte) (subLit[i] ? 1 : 0);
            }
            tag.putByteArray("sub_lit", litBytes);

            return tag;
        }

        public void load(CompoundTag tag) {
            this.color = tag.contains("color") ? tag.getString("color") : "white";
            this.lit = !tag.contains("lit") || tag.getBoolean("lit");

            if (tag.contains("sub_colors", Tag.TAG_LIST)) {
                ListTag list = tag.getList("sub_colors", Tag.TAG_STRING);
                for (int i = 0; i < Math.min(list.size(), SUB_PIXELS_PER_EDGE); i++) {
                    subColors[i] = list.getString(i);
                }
            } else {
                Arrays.fill(subColors, color);
            }

            if (tag.contains("sub_lit", Tag.TAG_BYTE_ARRAY)) {
                byte[] bytes = tag.getByteArray("sub_lit");
                for (int i = 0; i < Math.min(bytes.length, SUB_PIXELS_PER_EDGE); i++) {
                    subLit[i] = bytes[i] == 1;
                }
            } else {
                Arrays.fill(subLit, lit);
            }
        }
    }

    private final Map<Direction, EdgeState> edges = new EnumMap<>(Direction.class);

    public EdgeLightData() {
        for (Direction dir : HORIZONTALS) {
            edges.put(dir, new EdgeState());
        }
    }

    public EdgeLightData(EdgeLightData other) {
        for (Direction dir : HORIZONTALS) {
            edges.put(dir, new EdgeState(other.getEdge(dir)));
        }
    }

    public @NotNull EdgeState getEdge(Direction dir) {
        return edges.computeIfAbsent(dir, d -> new EdgeState());
    }

    public void setEdge(Direction dir, String color, boolean lit) {
        getEdge(dir).set(color, lit);
        syncCornerSubPixel(dir, 0, color, lit);
        syncCornerSubPixel(dir, 7, color, lit);
    }

    public void setSubPixel(Direction dir, int index, String color, boolean lit) {
        getEdge(dir).setSubPixel(index, color, lit);
        syncCornerSubPixel(dir, index, color, lit);
    }

    public String getSubPixelColor(Direction dir, int index) {
        return getEdge(dir).getSubColor(index);
    }

    public boolean isSubPixelLit(Direction dir, int index) {
        return getEdge(dir).isSubLit(index);
    }

    public void syncCornerSubPixel(Direction dir, int index, String color, boolean lit) {
        switch (dir) {
            case NORTH -> {
                if (index == 0) getEdge(Direction.WEST).setSubPixel(0, color, lit);
                else if (index == 7) getEdge(Direction.EAST).setSubPixel(0, color, lit);
            }
            case SOUTH -> {
                if (index == 0) getEdge(Direction.WEST).setSubPixel(7, color, lit);
                else if (index == 7) getEdge(Direction.EAST).setSubPixel(7, color, lit);
            }
            case WEST -> {
                if (index == 0) getEdge(Direction.NORTH).setSubPixel(0, color, lit);
                else if (index == 7) getEdge(Direction.SOUTH).setSubPixel(0, color, lit);
            }
            case EAST -> {
                if (index == 0) getEdge(Direction.NORTH).setSubPixel(7, color, lit);
                else if (index == 7) getEdge(Direction.SOUTH).setSubPixel(7, color, lit);
            }
        }
    }

    public String getEdgeColor(Direction dir) {
        return getEdge(dir).getColor();
    }

    public boolean isEdgeLit(Direction dir) {
        return getEdge(dir).isLit();
    }

    public static BooleanProperty getPropertyForDirection(Direction dir) {
        return switch (dir) {
            case NORTH -> EdgeLightBlock.NORTH;
            case SOUTH -> EdgeLightBlock.SOUTH;
            case EAST -> EdgeLightBlock.EAST;
            case WEST -> EdgeLightBlock.WEST;
            default -> null;
        };
    }

    /**
     * Checks if any active, enabled edge on this blockstate is currently lit.
     */
    public boolean hasAnyLitEdge(BlockState state) {
        for (Direction dir : HORIZONTALS) {
            BooleanProperty prop = getPropertyForDirection(dir);
            if (prop != null && state.hasProperty(prop) && state.getValue(prop)) {
                if (getEdge(dir).hasAnyLitSubPixel()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if all active edges and their sub-pixels have the same color and lit state.
     */
    public boolean isUniform(BlockState state) {
        String firstColor = null;
        Boolean firstLit = null;

        for (Direction dir : HORIZONTALS) {
            BooleanProperty prop = getPropertyForDirection(dir);
            if (prop != null && state.hasProperty(prop) && !state.getValue(prop)) {
                continue;
            }
            EdgeState es = getEdge(dir);
            if (!es.isUniform()) {
                return false;
            }
            if (firstColor == null) {
                firstColor = es.getColor();
                firstLit = es.isLit();
            } else {
                if (es.isLit() != firstLit || !Objects.equals(es.getColor(), firstColor)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if all edges and their sub-pixels have the same color and lit state.
     */
    public boolean isUniform() {
        String firstColor = null;
        Boolean firstLit = null;

        for (Direction dir : HORIZONTALS) {
            EdgeState es = getEdge(dir);
            if (!es.isUniform()) {
                return false;
            }
            if (firstColor == null) {
                firstColor = es.getColor();
                firstLit = es.isLit();
            } else {
                if (es.isLit() != firstLit || !Objects.equals(es.getColor(), firstColor)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Calculates the average color among all active, lit edges on this block.
     * Returns null if no active edges are lit.
     */
    public @Nullable String getAverageColor(BlockState state) {
        List<String> activeColors = new ArrayList<>();

        for (Direction dir : HORIZONTALS) {
            BooleanProperty prop = getPropertyForDirection(dir);
            if (prop != null && state.hasProperty(prop) && state.getValue(prop)) {
                EdgeState es = getEdge(dir);
                for (int i = 0; i < SUB_PIXELS_PER_EDGE; i++) {
                    if (es.isSubLit(i)) {
                        activeColors.add(es.getSubColor(i));
                    }
                }
            }
        }

        return ColorAveragingHelper.calculateAverageColor(activeColors);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Direction dir : HORIZONTALS) {
            tag.put(dir.getName(), getEdge(dir).save());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        for (Direction dir : HORIZONTALS) {
            if (tag.contains(dir.getName(), Tag.TAG_COMPOUND)) {
                getEdge(dir).load(tag.getCompound(dir.getName()));
            }
        }
    }
}
