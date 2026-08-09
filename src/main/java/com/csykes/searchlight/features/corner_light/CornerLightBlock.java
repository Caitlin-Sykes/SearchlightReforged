package com.csykes.searchlight.features.corner_light;

import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.csykes.searchlight.utils.lighting.CornerLightStage;
import com.csykes.searchlight.utils.lighting.LightRodConnection;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class CornerLightBlock extends AbstractLightBlock implements EntityBlock {
    private final DyeColor blockColor;
    private final String dyenamicColor;

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WallLightBlockEntity(pos, state);
    }

    public CornerLightBlock(Properties properties, DyeColor blockColor) {
        this(properties, blockColor, null);
    }

    public CornerLightBlock(Properties properties, String dyenamicColor) {
        this(properties, null, dyenamicColor);
    }

    private CornerLightBlock(Properties properties, DyeColor blockColor, String dyenamicColor) {
        super(properties);
        this.blockColor = blockColor;
        this.dyenamicColor = dyenamicColor;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, true)
                .setValue(BRIGHTNESS, BrightnessStage.MEDIUM)
                .setValue(CONNECTION, LightRodConnection.SINGLE)
                .setValue(CORNER, CornerLightStage.BOTTOM_LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECTION, CORNER);
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState baseState = super.getStateForPlacement(context);
        if (baseState == null) return null;

        Vec3 hit = context.getClickLocation();
        BlockPos pos = context.getClickedPos();

        CornerLightStage corner = getCorner(hit, pos);
        BlockState withCorner = baseState.setValue(CORNER, corner);
        return withCorner.setValue(CONNECTION, this.getConnectionState(context.getLevel(), pos, withCorner, Direction.Axis.Y));
    }

    private static @NotNull CornerLightStage getCorner(Vec3 hit, BlockPos pos) {
        double localX = hit.x - pos.getX();
        double localZ = hit.z - pos.getZ();

        CornerLightStage corner;

        if (localX < 0.5) {
            if (localZ < 0.5) {
                corner = CornerLightStage.BOTTOM_RIGHT;
            } else {
                corner = CornerLightStage.BOTTOM_LEFT;
            }
        } else {
            if (localZ < 0.5) {
                corner = CornerLightStage.TOP_RIGHT;
            } else {
                corner = CornerLightStage.TOP_LEFT;
            }
        }
        return corner;
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        return state.setValue(CONNECTION, getConnectionState(level, pos, state, Direction.Axis.Y));
    }

    @Override
    protected boolean isMatchingConnection(LevelAccessor level, BlockPos pos, BlockState state, BlockState neighborState) {
        return neighborState.getBlock() instanceof CornerLightBlock && neighborState.getValue(CORNER) == state.getValue(CORNER);
    }

    public static final MapCodec<CornerLightBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec(), DyeColor.CODEC.fieldOf("color").forGetter(CornerLightBlock::getBlockColor)).apply(instance, CornerLightBlock::new));

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    private static final VoxelShape SHAPE_BL = Block.box(0, 0, 14, 2, 16, 16);

    private static final VoxelShape SHAPE_BR = Block.box(0, 0, 0, 2, 16, 2);

    private static final VoxelShape SHAPE_TR = Block.box(14, 0, 0, 16, 16, 2);

    private static final VoxelShape SHAPE_TL = Block.box(14, 0, 14, 16, 16, 16);


    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return switch (state.getValue(CORNER)) {
            case BOTTOM_LEFT -> SHAPE_BL;
            case BOTTOM_RIGHT -> SHAPE_BR;
            case TOP_RIGHT -> SHAPE_TR;
            case TOP_LEFT -> SHAPE_TL;
        };
    }

}