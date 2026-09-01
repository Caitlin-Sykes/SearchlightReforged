package com.csykes.searchlight.features.edge_light;

import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec;

@Getter
public class EdgeLightBlock extends AbstractLightBlock implements EntityBlock {
    private final DyeColor blockColor;
    private final String dyenamicColor;
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");


    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WallLightBlockEntity(pos, state);
    }

    public EdgeLightBlock(Properties properties, DyeColor blockColor) {
        this(properties, blockColor, null);
    }

    public EdgeLightBlock(Properties properties, String dyenamicColor) {
        this(properties, null, dyenamicColor);
    }

    private EdgeLightBlock(Properties properties, DyeColor blockColor, String dyenamicColor) {
        super(properties);
        this.blockColor = blockColor;
        this.dyenamicColor = dyenamicColor;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACE, AttachFace.CEILING)
                .setValue(LIT, true)
                .setValue(NORTH, true)
                .setValue(SOUTH, true)
                .setValue(EAST, true)
                .setValue(WEST, true)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH);
        builder.add(SOUTH);
        builder.add(EAST);
        builder.add(WEST);
        builder.add(FACE);
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Calculate the axis based on the face the player clicked
        Direction[] axis = context.getNearestLookingDirections();
        Direction placementDir = Arrays.stream(axis).filter((d) -> d == Direction.DOWN || d == Direction.UP).findFirst().orElse(Direction.DOWN);

        // Start with the default state and apply the axis
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        BlockState state = defaultBlockState()
                .setValue(FACE, placementDir == Direction.DOWN ? AttachFace.FLOOR : AttachFace.CEILING)
                .setValue(NORTH, level.getBlockState(pos.north()).isFaceSturdy(level, pos.north(), Direction.SOUTH))
                .setValue(SOUTH, level.getBlockState(pos.south()).isFaceSturdy(level, pos.south(), Direction.NORTH))
                .setValue(EAST, level.getBlockState(pos.east()).isFaceSturdy(level, pos.east(), Direction.WEST))
                .setValue(WEST, level.getBlockState(pos.west()).isFaceSturdy(level, pos.west(), Direction.EAST));

        if (!state.getValue(NORTH) && !state.getValue(SOUTH) && !state.getValue(EAST) && !state.getValue(WEST))
            state = state
                    .setValue(NORTH, true)
                    .setValue(SOUTH, true)
                    .setValue(EAST, true)
                    .setValue(WEST, true);

        // Ensure other necessary defaults are set (like LIT)
        return state.setValue(LIT, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }


    @Override
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        return state;
    }

    public static final MapCodec<EdgeLightBlock> CODEC = mapCodec(instance -> instance.group(propertiesCodec(), DyeColor.CODEC.fieldOf("color").forGetter(EdgeLightBlock::getBlockColor)).apply(instance, EdgeLightBlock::new));

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    private static final VoxelShape SHAPE_CTR_Z = Block.box(6, 6, 0, 10, 10, 16);

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        AttachFace face = state.getValue(FACE);
        if (face == AttachFace.WALL) {
            return SHAPE_CTR_Z;
        }

        int yMin = face == AttachFace.CEILING ? 14 : 0;
        int yMax = face == AttachFace.CEILING ? 16 : 2;

        VoxelShape shape = Shapes.empty();

        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, Block.box(0, yMin, 0, 16, yMax, 2));
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, Block.box(0, yMin, 14, 16, yMax, 16));
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, Block.box(14, yMin, 0, 16, yMax, 16));
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, Block.box(0, yMin, 0, 2, yMax, 16));
        }

        return shape.isEmpty() ? Shapes.block() : shape;
    }


}