package com.csykes.searchlight.features.corner_light;

import com.csykes.searchlight.utils.lighting.AbstractDirectionalLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.csykes.searchlight.utils.lighting.CornerLightStage;
import com.csykes.searchlight.utils.lighting.LightRodConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class CornerLightBlock extends AbstractDirectionalLightBlock {
    public static final EnumProperty<LightRodConnection> CONNECTION = EnumProperty.create("connection", LightRodConnection.class);
    public static final EnumProperty<CornerLightStage> CORNER = EnumProperty.create("corner", CornerLightStage.class);

    private final DyeColor blockColor;

    public CornerLightBlock(Properties properties, DyeColor blockColor) {
        super(properties);
        this.blockColor = blockColor;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(LIT, true)
                .setValue(BRIGHTNESS, BrightnessStage.MEDIUM)
                .setValue(CONNECTION, LightRodConnection.SINGLE)
                .setValue(CORNER, CornerLightStage.BOTTOM_LEFT));
    }

    public DyeColor getBlockColor() {
        return this.blockColor;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        BlockState baseState = super.getStateForPlacement(context);
        if (baseState == null) return null;

        BlockPos pos = context.getClickedPos();
        Direction playerFacing = context.getHorizontalDirection();

        // Determine predictable corner staging based on player orientation
        CornerLightStage corner = CornerLightStage.BOTTOM_LEFT;
        double lookAngle = context.getRotation(); // 0 to 360 degrees

        if (lookAngle >= 315 || lookAngle < 45) corner = CornerLightStage.TOP_RIGHT;
        else if (lookAngle >= 45 && lookAngle < 135) corner = CornerLightStage.BOTTOM_RIGHT;
        else if (lookAngle >= 135 && lookAngle < 225) corner = CornerLightStage.BOTTOM_LEFT;
        else corner = CornerLightStage.TOP_LEFT;

        return baseState
                .setValue(FACING, clickedFace)
                .setValue(CORNER, corner)
                .setValue(CONNECTION, this.getConnectionState(context.getLevel(), pos, clickedFace, corner));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECTION, CORNER);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Run updates when adjacent neighbor rods match our orientation vector context
        return state.setValue(CONNECTION, getConnectionState(level, pos, state.getValue(FACING), state.getValue(CORNER)));
    }

    private LightRodConnection getConnectionState(LevelAccessor level, BlockPos pos, Direction facing, CornerLightStage corner) {
        // Calculate neighbor connection check vectors relative to the light's placement plane
        Direction lineDir = getLineDirection(facing, corner);

        boolean hasForward = isMatchingConnection(level, pos.relative(lineDir), facing, corner);
        boolean hasBackward = isMatchingConnection(level, pos.relative(lineDir.getOpposite()), facing, corner);

        if (hasForward && hasBackward) return LightRodConnection.MIDDLE;
        if (hasForward) return LightRodConnection.BOTTOM;
        if (hasBackward) return LightRodConnection.TOP;
        return LightRodConnection.SINGLE;
    }

    private Direction getLineDirection(Direction facing, CornerLightStage corner) {
        if (facing.getAxis() == Direction.Axis.Y) {
            return (corner == CornerLightStage.BOTTOM_LEFT || corner == CornerLightStage.TOP_LEFT) ? Direction.NORTH : Direction.SOUTH;
        }
        return Direction.UP;
    }

    private LightRodConnection getConnectionState(LevelAccessor level, BlockPos pos, Direction facing) {
        // 1. Get the direction enum defining where the rod line actually travels
        CornerLightStage corner = level.getBlockState(pos).getValue(CORNER);
        Direction lineDir = this.getLineDirection(facing, corner);

        // 2. Search down the line instead of looking at the wall facing vector
        boolean hasForward = isMatchingConnection(level, pos.relative(lineDir), facing, corner);
        boolean hasBackward = isMatchingConnection(level, pos.relative(lineDir.getOpposite()), facing, corner);

        if (hasForward && hasBackward) return LightRodConnection.MIDDLE;
        if (hasForward) return LightRodConnection.BOTTOM;
        if (hasBackward) return LightRodConnection.TOP;
        return LightRodConnection.SINGLE;
    }

    private boolean isMatchingConnection(LevelAccessor level, BlockPos target, Direction facing, CornerLightStage corner) {
        BlockState state = level.getBlockState(target);
        // 3. Match ANY corner light block subclass instance so colors merge cleanly
        return state.getBlock() instanceof CornerLightBlock
                && state.getValue(FACING) == facing
                && state.getValue(CORNER) == corner;
    }
    
    public static final com.mojang.serialization.MapCodec<CornerLightBlock> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    propertiesCodec(),
                    net.minecraft.world.item.DyeColor.CODEC.fieldOf("color").forGetter(CornerLightBlock::getBlockColor)
            ).apply(instance, CornerLightBlock::new)
    );

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.DirectionalBlock> codec() {
        return CODEC;
    }
}