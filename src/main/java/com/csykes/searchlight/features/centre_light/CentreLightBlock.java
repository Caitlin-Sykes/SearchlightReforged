package com.csykes.searchlight.features.centre_light;

import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.csykes.searchlight.utils.lighting.LightRodConnection;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class CentreLightBlock extends AbstractLightBlock implements EntityBlock {
    private final DyeColor blockColor;
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WallLightBlockEntity(pos, state);
    }

    public CentreLightBlock(Properties properties, DyeColor blockColor) {
        super(properties);
        this.blockColor = blockColor;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.WALL)
                .setValue(LIT, true)
                .setValue(BRIGHTNESS, BrightnessStage.MEDIUM)
                .setValue(CONNECTION, LightRodConnection.SINGLE)
                .setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECTION);
        builder.add(AXIS);
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Calculate the axis based on the face the player clicked
        Direction.Axis axis = context.getClickedFace().getAxis();

        // Start with the default state and apply the axis
        BlockState state = this.defaultBlockState().setValue(AXIS, axis);

        // Apply your existing connection logic
        state = state.setValue(CONNECTION, this.getConnectionState(context.getLevel(), context.getClickedPos(), axis));

        // Ensure other necessary defaults are set (like LIT)
        return state.setValue(LIT, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }


    @Override
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        return state.setValue(CONNECTION, getConnectionState(level, pos, state.getValue(AXIS)));
    }

    private LightRodConnection getConnectionState(LevelAccessor level, BlockPos pos, Direction.Axis axis) {

        if (axis == Direction.Axis.Y) {
            boolean hasAbove = isMatchingConnection(level, pos.relative(Direction.UP));
            boolean hasBelow = isMatchingConnection(level, pos.relative(Direction.DOWN));

            if (hasAbove && hasBelow) return LightRodConnection.MIDDLE;
            if (hasAbove) return LightRodConnection.BOTTOM;
            if (hasBelow) return LightRodConnection.TOP;
        } else if (axis == Direction.Axis.X) {
            // X-axis rods connect to East and West
            boolean hasEast = isMatchingConnection(level, pos.relative(Direction.EAST));
            boolean hasWest = isMatchingConnection(level, pos.relative(Direction.WEST));

            if (hasEast && hasWest) return LightRodConnection.MIDDLE;
            if (hasEast) return LightRodConnection.TOP;
            if (hasWest) return LightRodConnection.BOTTOM;
        } else if (axis == Direction.Axis.Z) {
            // Z-axis rods connect to North and South
            boolean hasNorth = isMatchingConnection(level, pos.relative(Direction.NORTH));
            boolean hasSouth = isMatchingConnection(level, pos.relative(Direction.SOUTH));

            if (hasNorth && hasSouth) return LightRodConnection.MIDDLE;
            if (hasNorth) return LightRodConnection.TOP;
            if (hasSouth) return LightRodConnection.BOTTOM;
        }


        return LightRodConnection.SINGLE;
    }

    private boolean isMatchingConnection(LevelAccessor level, BlockPos target) {
        BlockState state = level.getBlockState(target);
        return state.getBlock() instanceof CentreLightBlock;
    }

    public static final com.mojang.serialization.MapCodec<CentreLightBlock> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec(), net.minecraft.world.item.DyeColor.CODEC.fieldOf("color").forGetter(CentreLightBlock::getBlockColor)).apply(instance, CentreLightBlock::new));

    @Override
    protected com.mojang.serialization.@NotNull MapCodec<? extends net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    private static final VoxelShape SHAPE_CTR = Block.box(6, 0, 6, 10, 16, 10);

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE_CTR;
    }


}