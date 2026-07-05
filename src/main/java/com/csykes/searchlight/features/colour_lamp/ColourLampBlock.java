package com.csykes.searchlight.features.colour_lamp;

import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class ColourLampBlock extends AbstractLightBlock implements EntityBlock {
    private final DyeColor blockColor;
    public static final IntegerProperty CONNECT_POINTS = IntegerProperty.create("connect_points", 0, 63);
    public static final BooleanProperty WIDE_CLUSTER = BooleanProperty.create("wide_cluster");

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
                .setValue(CONNECT_POINTS, 0)
                .setValue(WIDE_CLUSTER, false)); // Default to false, needed as a way to differentiate between wide and narrow clusters
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECT_POINTS, WIDE_CLUSTER);
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

    private boolean checkStructureContext(LevelAccessor level, BlockPos pos, int mask) {
        System.out.println("MASKKKKK " + mask);
        // Top-Left Corner (Mask = 1 | 2, i.e., 36)
        if (mask == 36) {
            System.out.println("Top-Left Corner");
            System.out.println(level.getBlockState(pos.south().east()).getBlock());
            return level.getBlockState(pos.south().east()).getBlock() instanceof ColourLampBlock;
        }

        // Top-Right Corner (Mask = 1 | 8, i.e., 9)
        if (mask == 33) {
            return level.getBlockState(pos.south().west()).getBlock() instanceof ColourLampBlock;
        }

        // Bottom-Left Corner (Mask = 4 | 2, i.e., 6)
        if (mask == 20) {
            return level.getBlockState(pos.above().east()).getBlock() instanceof ColourLampBlock;
        }

        // Bottom-Right Corner (Mask = 4 | 8, i.e., 12)
        if (mask == 17) {
            return level.getBlockState(pos.above().west()).getBlock() instanceof ColourLampBlock;
        }
        return false;
    }


    @Override
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
                                           @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {

        int mask = calculateConnectionMask(level, pos, state);
        if (state.getValue(CONNECT_POINTS) == mask) return state;

        boolean wideCluster = checkStructureContext(level, pos, mask);

        return state.setValue(CONNECT_POINTS, mask)
                .setValue(WIDE_CLUSTER, wideCluster);
    }

    @Override
    protected boolean isMatchingConnection(LevelAccessor level, BlockPos pos, BlockState state, BlockState neighborState) {
        return neighborState.getBlock() instanceof ColourLampBlock;
    }

    public static final MapCodec<ColourLampBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec(), DyeColor.CODEC.fieldOf("color").forGetter(ColourLampBlock::getBlockColor)).apply(instance, ColourLampBlock::new));

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}