package com.csykes.searchlight.features.colour_lamp;

import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class ColourLampBlock extends AbstractLightBlock implements EntityBlock {
    private final DyeColor blockColor;
    public static final IntegerProperty CONNECT_POINTS = IntegerProperty.create("connect_points", 0, 63);

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WallLightBlockEntity(pos, state);
    }

    public ColourLampBlock(Properties properties, DyeColor blockColor) {
        super(properties);
        this.blockColor = blockColor;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, true)
                .setValue(BRIGHTNESS, BrightnessStage.MEDIUM)
                .setValue(CONNECT_POINTS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECT_POINTS);
    }

    private int calculateConnectionMask(LevelAccessor level, BlockPos pos, BlockState state) {
        int mask = 0;
        // Standard NESW (1, 2, 4, 8)
        if (isMatchingConnection(level, pos.north(), state, level.getBlockState(pos.north()))) mask |= 1;
        if (isMatchingConnection(level, pos.east(), state, level.getBlockState(pos.east()))) mask |= 2;
        if (isMatchingConnection(level, pos.south(), state, level.getBlockState(pos.south()))) mask |= 4;
        if (isMatchingConnection(level, pos.west(), state, level.getBlockState(pos.west()))) mask |= 8;

        // Expanded UD (16, 32)
        if (isMatchingConnection(level, pos.above(), state, level.getBlockState(pos.above()))) mask |= 16;
        if (isMatchingConnection(level, pos.below(), state, level.getBlockState(pos.below()))) mask |= 32;

        return mask;
    }


    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState baseState = super.getStateForPlacement(context);
        if (baseState == null) return null;

        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        // Calculate initial mask based on current surroundings
        int mask = calculateConnectionMask(level, pos, baseState);

        return baseState.setValue(CONNECT_POINTS, mask);
    }


    @Override
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
                                           @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        return state.setValue(CONNECT_POINTS, calculateConnectionMask(level, pos, state));
    }

    @Override
    protected boolean isMatchingConnection(LevelAccessor level, BlockPos pos, BlockState state, BlockState neighborState) {
        return neighborState.getBlock() instanceof ColourLampBlock;
    }

    public static final com.mojang.serialization.MapCodec<ColourLampBlock> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec(), DyeColor.CODEC.fieldOf("color").forGetter(ColourLampBlock::getBlockColor)).apply(instance, ColourLampBlock::new));

    @Override
    protected com.mojang.serialization.@NotNull MapCodec<? extends net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}