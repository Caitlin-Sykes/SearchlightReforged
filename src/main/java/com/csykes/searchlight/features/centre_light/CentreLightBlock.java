package com.csykes.searchlight.features.centre_light;

import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
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
    private final String dyenamicColor;
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WallLightBlockEntity(pos, state);
    }

    public CentreLightBlock(Properties properties, DyeColor blockColor) {
        this(properties, blockColor, null);
    }

    public CentreLightBlock(Properties properties, String dyenamicColor) {
        this(properties, null, dyenamicColor);
    }

    private CentreLightBlock(Properties properties, DyeColor blockColor, String dyenamicColor) {
        super(properties);
        this.blockColor = blockColor;
        this.dyenamicColor = dyenamicColor;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.WALL)
                .setValue(LIT, true)
                .setValue(CONNECTION, LightRodConnection.SINGLE)
                .setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACE);
        builder.add(FACING);
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
        state = state.setValue(CONNECTION, this.getConnectionState(context.getLevel(), context.getClickedPos(), state, axis));

        // Ensure other necessary defaults are set (like LIT)
        return state.setValue(LIT, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }


    @Override
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        return state.setValue(CONNECTION, getConnectionState(level, pos, state, state.getValue(AXIS)));
    }

    @Override
    protected boolean isMatchingConnection(LevelAccessor level, BlockPos pos, BlockState state, BlockState neighborState) {
        return neighborState.getBlock() instanceof CentreLightBlock && neighborState.getValue(AXIS) == state.getValue(AXIS);
    }

    public static final MapCodec<CentreLightBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec(), DyeColor.CODEC.fieldOf("color").forGetter(CentreLightBlock::getBlockColor)).apply(instance, CentreLightBlock::new));

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    private static final VoxelShape SHAPE_CTR_X = Block.box(0, 6, 6, 16, 10, 10);
    private static final VoxelShape SHAPE_CTR_Y = Block.box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape SHAPE_CTR_Z = Block.box(6, 6, 0, 10, 10, 16);

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_CTR_X;
            case Y -> SHAPE_CTR_Y;
            case Z -> SHAPE_CTR_Z;
        };
    }


}