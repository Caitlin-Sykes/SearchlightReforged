package com.csykes.searchlight.features.colour_lamp;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractConnectedLampBlock;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec;

@Getter
public class ColourLampBlock extends AbstractConnectedLampBlock implements EntityBlock {

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WallLightBlockEntity(Searchlight.COLOUR_LAMPS_BE.get(), pos, state);
    }

    public ColourLampBlock(Properties properties, DyeColor blockColor) {
        this(properties, blockColor, null);
    }

    public ColourLampBlock(Properties properties, String dyenamicColor) {
        this(properties, null, dyenamicColor);
    }

    public ColourLampBlock(Properties properties, DyeColor blockColor, String dyenamicColor) {
        super(properties, blockColor, dyenamicColor);
    }

    @Override
    public @Nullable Block getBlockForColor(String colorKey) {
        DeferredBlock<Block> holder = Searchlight.COLOUR_LAMPS.get(colorKey);
        return holder != null ? holder.get() : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // Loop through directions and update the state variable
        for (Direction dir : Direction.values()) {
            state = getDirection(dir, level, pos, state);
        }

        return state.setValue(LIT, !level.hasNeighborSignal(pos));
    }

    private BlockState getDirection(Direction dir, Level level, BlockPos pos, BlockState state) {
        boolean isConnected = level.getBlockState(pos.relative(dir)).getBlock() instanceof ColourLampBlock;

        return switch (dir) {
            case UP -> state.setValue(UP, isConnected);
            case DOWN -> state.setValue(DOWN, isConnected);
            case NORTH -> state.setValue(NORTH, isConnected);
            case EAST -> state.setValue(EAST, isConnected);
            case WEST -> state.setValue(WEST, isConnected);
            case SOUTH -> state.setValue(SOUTH, isConnected);
        };
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean isConnected = neighborState.getBlock() instanceof ColourLampBlock;

        return state.setValue(getPropertyForDirection(direction), isConnected);
    }

    public static final MapCodec<ColourLampBlock> CODEC = mapCodec(instance -> instance.group(propertiesCodec(), DyeColor.CODEC.fieldOf("color").forGetter(ColourLampBlock::getBlockColor)).apply(instance, ColourLampBlock::new));

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}