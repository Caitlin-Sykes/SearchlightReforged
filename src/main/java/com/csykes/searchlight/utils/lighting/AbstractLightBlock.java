package com.csykes.searchlight.utils.lighting;

import com.csykes.searchlight.SearchlightClient;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.searchlight.SearchlightBlock;
import com.csykes.searchlight.features.searchlight.SearchlightBlockEntity;
import com.csykes.searchlight.utils.SearchlightUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractLightBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final EnumProperty<BrightnessStage> BRIGHTNESS = EnumProperty.create("brightness", BrightnessStage.class);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<LightRequest> LIGHT_REQUEST = EnumProperty.create("light_request", LightRequest.class);
    public static final EnumProperty<LightRodConnection> CONNECTION = EnumProperty.create("connection", LightRodConnection.class);
    public static final EnumProperty<CornerLightStage> CORNER = EnumProperty.create("corner", CornerLightStage.class);
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    protected AbstractLightBlock(@NotNull Properties properties) {
        super(properties);
    }

    public abstract @Nullable BlockEntityType<?> getBlockEntityType();

    public @Nullable Block getBlockForColor(String colorKey) {
        return null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide) {
            world.getLightEngine().checkBlock(pos);
            world.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        updateLitState(world, pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            updateLitState(world, pos, state);
        }
    }

    protected boolean isMatchingConnection(LevelAccessor level, BlockPos pos, BlockState state, BlockState neighborState) {
        return false;
    }

    public List<BlockPos> getConnectedLights(Level level, BlockPos pos, BlockState state) {
        List<BlockPos> positions = new java.util.ArrayList<>();
        positions.add(pos);

        if (!state.hasProperty(CONNECTION)) {
            return positions;
        }

        Direction.Axis axis = state.hasProperty(BlockStateProperties.AXIS) ? state.getValue(BlockStateProperties.AXIS) : Direction.Axis.Y;
        
        Direction positiveDir;
        Direction negativeDir;
        if (axis == Direction.Axis.X) {
            positiveDir = Direction.EAST;
            negativeDir = Direction.WEST;
        } else if (axis == Direction.Axis.Z) {
            positiveDir = Direction.NORTH;
            negativeDir = Direction.SOUTH;
        } else {
            positiveDir = Direction.UP;
            negativeDir = Direction.DOWN;
        }

        // Traverse positive
        BlockPos current = pos.relative(positiveDir);
        while (true) {
            BlockState neighborState = level.getBlockState(current);
            if (isMatchingConnection(level, pos, state, neighborState)) {
                positions.add(current);
                current = current.relative(positiveDir);
            } else {
                break;
            }
        }

        // Traverse negative
        current = pos.relative(negativeDir);
        while (true) {
            BlockState neighborState = level.getBlockState(current);
            if (isMatchingConnection(level, pos, state, neighborState)) {
                positions.add(current);
                current = current.relative(negativeDir);
            } else {
                break;
            }
        }

        return positions;
    }


    protected LightRodConnection getConnectionState(LevelAccessor level, BlockPos pos, BlockState state, Direction.Axis axis) {
        Direction positiveDir;
        Direction negativeDir;
        if (axis == Direction.Axis.X) {
            positiveDir = Direction.EAST;
            negativeDir = Direction.WEST;
        } else if (axis == Direction.Axis.Z) {
            positiveDir = Direction.NORTH;
            negativeDir = Direction.SOUTH;
        } else {
            positiveDir = Direction.UP;
            negativeDir = Direction.DOWN;
        }

        boolean hasPositive = isMatchingConnection(level, pos, state, level.getBlockState(pos.relative(positiveDir)));
        boolean hasNegative = isMatchingConnection(level, pos, state, level.getBlockState(pos.relative(negativeDir)));

        if (hasPositive && hasNegative) return LightRodConnection.MIDDLE;
        if (hasPositive) {
            return axis == Direction.Axis.X ? LightRodConnection.TOP : LightRodConnection.BOTTOM;
        }
        if (hasNegative) {
            return axis == Direction.Axis.X ? LightRodConnection.BOTTOM : LightRodConnection.TOP;
        }
        return LightRodConnection.SINGLE;
    }


    public void updateLitState(Level world, BlockPos pos, BlockState state) {
        if (world.isClientSide) return;
        
        List<BlockPos> connected = getConnectedLights(world, pos, state);
        boolean isPoweredNow = false;
        LightRequest requested = LightRequest.RELEASE;
        
        for (BlockPos p : connected) {
            isPoweredNow |= world.hasNeighborSignal(p);
            BlockEntity be = world.getBlockEntity(p);
            if (be instanceof AddressableLight light && light.getLightRequest() != LightRequest.RELEASE) {
                requested = light.getLightRequest();
            }
        }
        
        boolean shouldBeLit = !isPoweredNow;
        if (requested != LightRequest.RELEASE) {
            shouldBeLit = requested == LightRequest.ON;
        }

        for (BlockPos p : connected) {
            BlockState s = world.getBlockState(p);
            if (s.hasProperty(LIT) && s.getValue(LIT) != shouldBeLit) {
                world.setBlockAndUpdate(p, s.setValue(LIT, shouldBeLit));
                world.getLightEngine().checkBlock(p);
                world.updateNeighborsAt(p, s.getBlock());
            }
        }
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.hasProperty(LIT) && !state.getValue(LIT)) {
            return 0;
        }
        if (level != null && pos != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AddressableLight light) {
                return light.getBrightness().getLightLevel();
            }
        }
        return 15;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis() == Direction.Axis.Y) {
                state = state.trySetValue(FACE, direction == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR);
                state = state.trySetValue(FACING, context.getHorizontalDirection());
            } else {
                state = state.trySetValue(FACE, AttachFace.WALL);
                state = state.trySetValue(FACING, direction.getOpposite());
            }

            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                break;
            }
        }
        return state.setValue(LIT, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.getItem() instanceof DyeItem dyeItem) {
            if (world.isClientSide) {
                return ItemInteractionResult.sidedSuccess(world.isClientSide);
            }
            DyeColor dyeColor = dyeItem.getDyeColor();
            String normalizedColor = dyeColor.getName().toLowerCase();
            Block block = state.getBlock();

            if (state.hasProperty(SearchlightBlock.COLOR)) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof SearchlightBlockEntity searchlightBe) {
                    searchlightBe.setColor(dyeColor);
                } else {
                    world.setBlockAndUpdate(pos, state.setValue(SearchlightBlock.COLOR, dyeColor));
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                world.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                return ItemInteractionResult.sidedSuccess(world.isClientSide);
            }

            Block newBlock = getBlockForColor(normalizedColor);

            if (newBlock != null && newBlock != block) {
                BlockEntity clickedBe = world.getBlockEntity(pos);
                List<BlockPos> connected = (clickedBe instanceof AddressableLight al && al.getLightMode() == LightMode.SEPARATE)
                        ? List.of(pos)
                        : getConnectedLights(world, pos, state);

                for (BlockPos connectedPos : connected) {
                    BlockState s = world.getBlockState(connectedPos);
                    BlockState ns = copyMatchingProperties(s, newBlock.defaultBlockState());

                    String oldAddress = "";
                    BrightnessStage oldBrightness = BrightnessStage.MEDIUM;
                    LightRequest oldLightRequest = LightRequest.RELEASE;
                    LightMode oldLightMode = LightMode.FIXTURE;
                    BlockEntity oldBe = world.getBlockEntity(connectedPos);
                    if (oldBe instanceof AddressableLight addressable) {
                        oldAddress = addressable.getAddress();
                        oldBrightness = addressable.getBrightness();
                        oldLightRequest = addressable.getLightRequest();
                        oldLightMode = addressable.getLightMode();
                    }

                    world.setBlockAndUpdate(connectedPos, ns);
                    world.updateNeighborsAt(connectedPos, newBlock);

                    BlockEntity newBe = world.getBlockEntity(connectedPos);
                    if (newBe instanceof AddressableLight addressable) {
                        addressable.setAddress(oldAddress);
                        addressable.setBrightness(oldBrightness);
                        addressable.setLightRequest(oldLightRequest);
                        addressable.setLightMode(oldLightMode);
                        newBe.setChanged();
                        world.sendBlockUpdated(connectedPos, ns, ns, 3);
                        world.getLightEngine().checkBlock(connectedPos);
                    }
                }

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                world.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                return ItemInteractionResult.sidedSuccess(world.isClientSide);
            }
        }

        if (stack.is(Items.GLOWSTONE_DUST) || stack.is(Items.REDSTONE)) {
            if (world.isClientSide) {
                return ItemInteractionResult.sidedSuccess(world.isClientSide);
            }

            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof AddressableLight light) {
                BrightnessStage brightness = light.getBrightness();
                BrightnessStage next = brightness;
                boolean success = false;

                if (stack.is(Items.GLOWSTONE_DUST) && brightness != BrightnessStage.ULTRA) {
                    next = brightness.next();
                    world.playSound(null, pos, SoundEvents.GLOW_ITEM_FRAME_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (next == BrightnessStage.ULTRA) {
                        player.displayClientMessage(Component.translatable("searchlight.message.highest_brightness"), true);
                    }
                    success = true;
                } else if (stack.is(Items.REDSTONE) && brightness != BrightnessStage.OFF) {
                    next = brightness.previous();
                    world.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (next == BrightnessStage.OFF) {
                        player.displayClientMessage(Component.translatable("searchlight.message.lowest_brightness"), true);
                    }
                    success = true;
                }

                if (success) {
                    List<BlockPos> connected = (light.getLightMode() == LightMode.SEPARATE)
                            ? List.of(pos)
                            : getConnectedLights(world, pos, state);
                    for (BlockPos connectedPos : connected) {
                        BlockEntity targetBe = world.getBlockEntity(connectedPos);
                        if (targetBe instanceof AddressableLight targetLight) {
                            targetLight.setBrightness(next);
                            targetBe.setChanged();
                            BlockState targetState = world.getBlockState(connectedPos);
                            world.sendBlockUpdated(connectedPos, targetState, targetState, 3);
                            world.getLightEngine().checkBlock(connectedPos);
                            world.updateNeighborsAt(connectedPos, targetState.getBlock());
                            if (targetBe instanceof SearchlightBlockEntity searchlight && searchlight.getLightSourcePos() != null) {
                                world.getLightEngine().checkBlock(searchlight.getLightSourcePos());
                                BlockState lsState = world.getBlockState(searchlight.getLightSourcePos());
                                world.sendBlockUpdated(searchlight.getLightSourcePos(), lsState, lsState, 3);
                            }
                        }
                    }

                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    return ItemInteractionResult.sidedSuccess(world.isClientSide);
                }
            }
        }

        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (ModList.get().isLoaded("computercraft") && player.isShiftKeyDown()) {
            if (world.isClientSide) {
                SearchlightClient.openLightAddressScreen(pos);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @SuppressWarnings("unchecked")
    private BlockState copyMatchingProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> property : from.getProperties()) {
            if (result.hasProperty(property)) {
                result = copyProperty(from, result, (Property) property);
            }
        }
        return result;
    }

    private <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }
}