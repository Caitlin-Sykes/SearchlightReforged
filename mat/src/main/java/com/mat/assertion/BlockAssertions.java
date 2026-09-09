package com.mat.assertion;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Objects;
import java.util.Optional;

/**
 * Inspection and assertion utilities for validating BlockStates and properties.
 */
@UtilityClass
public class BlockAssertions {

    /**
     * Checks if the given {@link BlockState} matches the specified namespaced block ID.
     *
     * @param state           the block state to check
     * @param expectedBlockId the expected block ID
     * @return true if the block state matches the expected block ID, false otherwise
     */
    public static boolean matchesBlockId(BlockState state, String expectedBlockId) {
        Objects.requireNonNull(state, "BlockState must not be null");
        ResourceLocation expected = ResourceLocation.parse(expectedBlockId);
        ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return expected.equals(actual);
    }

    /**
     * Asserts that the block state matches the expected block ID.
     *
     * @param pos             the position of the block
     * @param state           the block state to check
     * @param expectedBlockId the expected block ID
     */
    public static void assertBlockId(BlockPos pos, BlockState state, String expectedBlockId) {
        if (!matchesBlockId(state, expectedBlockId)) {
            ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            throw new GameTestAssertException("Expected block at " + pos + " to have ID '" + expectedBlockId + "', but was '" + actual + "'");
        }
    }

    /**
     * Checks if any property on the block state (such as EnumProperty for colour) matches the expected string representation.
     *
     * @param state          the block state to check
     * @param expectedColour the expected colour string representation
     * @return true if any property matches the expected colour, false otherwise
     */
    public static boolean matchesColour(BlockState state, String expectedColour) {
        Objects.requireNonNull(state, "BlockState must not be null");
        for (Property<?> property : state.getProperties()) {
            Comparable<?> value = state.getValue(property);
            if (value instanceof StringRepresentable stringRepresentable) {
                if (stringRepresentable.getSerializedName().equalsIgnoreCase(expectedColour)) {
                    return true;
                }
            } else if (value.toString().equalsIgnoreCase(expectedColour)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Asserts that a colour property on the block state matches the expected colour.
     *
     * @param pos            the position of the block
     * @param state          the block state to check
     * @param expectedcolour the expected colour string representation
     */
    public static void assertColour(BlockPos pos, BlockState state, String expectedColour) {
        if (!matchesColour(state, expectedColour)) {
            throw new GameTestAssertException("Expected block at " + pos + " to have colour '" + expectedColour + "', but no matching property found on state: " + state);
        }
    }

    /**
     * Retrieves the optional value of a specific property from the block state.
     *
     * @param state    the block state to retrieve the property value from
     * @param property the property to retrieve the value for
     *
     */
    public static <T extends Comparable<T>> Optional<T> getPropertyValue(BlockState state, Property<T> property) {
        Objects.requireNonNull(state, "BlockState must not be null");
        Objects.requireNonNull(property, "Property must not be null");
        if (state.hasProperty(property)) {
            return Optional.of(state.getValue(property));
        }
        return Optional.empty();
    }

    /**
     * Asserts that a specific property on the block state equals the expected value.
     *
     * @param pos           the position of the block
     * @param state         the block state to check
     * @param property      the property to retrieve the value for
     * @param expectedValue the expected value of the property
     */
    public static <T extends Comparable<T>> void assertProperty(BlockPos pos, BlockState state, Property<T> property, T expectedValue) {
        Optional<T> actual = getPropertyValue(state, property);
        if (actual.isEmpty()) {
            throw new GameTestAssertException("Expected block at " + pos + " to have property '" + property.getName() + "', but property was missing on: " + state);
        }
        if (!Objects.equals(actual.get(), expectedValue)) {
            throw new GameTestAssertException("Expected block at " + pos + " property '" + property.getName() + "' to be '" + expectedValue + "', but was '" + actual.get() + "'");
        }
    }
}
