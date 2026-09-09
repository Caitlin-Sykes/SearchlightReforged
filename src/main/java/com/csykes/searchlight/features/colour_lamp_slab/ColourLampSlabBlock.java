package com.csykes.searchlight.features.colour_lamp_slab;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractConnectedLampBlock;
import com.csykes.searchlight.utils.lighting.ConnectableLightSlab;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec;

@Getter
public class ColourLampSlabBlock extends AbstractConnectedLampBlock implements EntityBlock, ConnectableLightSlab {
    public static final BooleanProperty TOP_HALF = BooleanProperty.create("top_half");

    @Override
    public BooleanProperty getTopHalfProperty() {
        return TOP_HALF;
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return Searchlight.COLOUR_LAMPS_SLAB_BE.get();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WallLightBlockEntity(getBlockEntityType(), pos, state);
    }

    public ColourLampSlabBlock(Properties properties, DyeColor blockColor) {
        this(properties, blockColor, null);
    }

    public ColourLampSlabBlock(Properties properties, String dyenamicColor) {
        this(properties, null, dyenamicColor);
    }

    public ColourLampSlabBlock(Properties properties, DyeColor blockColor, String dyenamicColor) {
        super(properties, blockColor, dyenamicColor);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(TOP_HALF, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TOP_HALF);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return ConnectableLightSlab.getSlabShape(state, TOP_HALF);
    }

    @Override
    public @Nullable Block getFullBlock() {
        String colorKey = getColorKey();
        if (colorKey != null) {
            DeferredBlock<Block> holder = Searchlight.COLOUR_LAMPS.get(colorKey);
            if (holder != null) {
                return holder.get();
            }
        }
        return null;
    }

    @Override
    public @Nullable Block getBlockForColor(String colorKey) {
        DeferredBlock<Block> holder = Searchlight.COLOUR_SLAB_LAMPS.get(colorKey);
        return holder != null ? holder.get() : null;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return ConnectableLightSlab.canSlabBeReplaced(state, context, TOP_HALF, this.asItem());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return ConnectableLightSlab.getSlabStateForPlacement(
                this,
                context,
                TOP_HALF,
                getFullBlock(),
                (dir, st) -> getDirection(dir, context.getLevel(), context.getClickedPos(), st)
        );
    }

    private BlockState getDirection(Direction dir, Level level, BlockPos pos, BlockState state) {
        BlockState neighborState = level.getBlockState(pos.relative(dir));
        boolean isConnected = ConnectableLightSlab.isSlabConnected(state, neighborState, TOP_HALF);

        return switch (dir) {
            case UP -> state.setValue(UP, !isConnected);
            case DOWN -> state.setValue(DOWN, !isConnected);
            case NORTH -> state.setValue(NORTH, isConnected);
            case EAST -> state.setValue(EAST, isConnected);
            case WEST -> state.setValue(WEST, isConnected);
            case SOUTH -> state.setValue(SOUTH, isConnected);
        };
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean isConnected = ConnectableLightSlab.isSlabConnected(state, neighborState, TOP_HALF);

        if (direction == Direction.UP || direction == Direction.DOWN) {
            return state.setValue(getPropertyForDirection(direction), !isConnected);
        }
        return state.setValue(getPropertyForDirection(direction), isConnected);
    }

    public static final MapCodec<ColourLampSlabBlock> CODEC = mapCodec(instance -> instance.group(propertiesCodec(), DyeColor.CODEC.fieldOf("color").forGetter(ColourLampSlabBlock::getBlockColor)).apply(instance, ColourLampSlabBlock::new));

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}