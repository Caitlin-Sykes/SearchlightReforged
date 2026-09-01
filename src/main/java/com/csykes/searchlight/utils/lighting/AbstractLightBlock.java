package com.csykes.searchlight.utils.lighting;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.SearchlightClient;
import com.csykes.searchlight.features.centre_light.CentreLightBlock;
import com.csykes.searchlight.features.colour_lamp.ColourLampBlock;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.edge_light.EdgeLightBlock;
import com.csykes.searchlight.features.searchlight.SearchlightBlock;
import com.csykes.searchlight.features.searchlight.SearchlightBlockEntity;
import com.csykes.searchlight.features.wall_light.WallLightBlock;
import com.csykes.searchlight.utils.SearchlightUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;

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

    private boolean isCompatibleAxis(BlockState state, Direction.Axis traversalAxis) {
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return state.getValue(BlockStateProperties.AXIS) == traversalAxis;
        }
        return traversalAxis == Direction.Axis.Y;
    }

    protected boolean isMatchingConnection(LevelAccessor level, BlockPos pos, BlockState state, BlockState neighborState) {
        return false;
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
        boolean isPoweredNow = world.hasNeighborSignal(pos);
        boolean wasLitBefore = state.getValue(LIT);
        BlockEntity be = world.getBlockEntity(pos);
        LightRequest requested = (be instanceof AddressableLight light) ? light.getLightRequest() : LightRequest.RELEASE;

        if (state.hasProperty(CONNECTION)) {
            Direction.Axis axis = state.hasProperty(BlockStateProperties.AXIS) ? state.getValue(BlockStateProperties.AXIS) : Direction.Axis.Y;
            Direction upDir;
            Direction downDir;
            if (axis == Direction.Axis.X) {
                upDir = Direction.WEST;
                downDir = Direction.EAST;
            } else if (axis == Direction.Axis.Z) {
                upDir = Direction.SOUTH;
                downDir = Direction.NORTH;
            } else {
                upDir = Direction.UP;
                downDir = Direction.DOWN;
            }

            if (state.getValue(CONNECTION) == LightRodConnection.BOTTOM || state.getValue(CONNECTION) == LightRodConnection.MIDDLE) {
                int distance = 1;
                BlockState target = world.getBlockState(pos.relative(upDir, distance));
                while (target.getBlock() instanceof AbstractLightBlock && isCompatibleAxis(target, axis)) {
                    isPoweredNow |= world.hasNeighborSignal(pos.relative(upDir, distance));
                    BlockEntity neighborBe = world.getBlockEntity(pos.relative(upDir, distance));
                    if (neighborBe instanceof AddressableLight neighborLight && neighborLight.getLightRequest() != LightRequest.RELEASE) {
                        requested = neighborLight.getLightRequest();
                    }
                    distance++;
                    target = world.getBlockState(pos.relative(upDir, distance));
                }
            }

            if (state.getValue(CONNECTION) == LightRodConnection.TOP || state.getValue(CONNECTION) == LightRodConnection.MIDDLE) {
                int distance = 1;
                BlockState target = world.getBlockState(pos.relative(downDir, distance));
                while (target.getBlock() instanceof AbstractLightBlock && isCompatibleAxis(target, axis)) {
                    isPoweredNow |= world.hasNeighborSignal(pos.relative(downDir, distance));
                    distance++;
                    target = world.getBlockState(pos.relative(downDir, distance));
                }
            }
        }
        boolean shouldBeLit = !isPoweredNow;
        if (requested != LightRequest.RELEASE) {
            shouldBeLit = requested == LightRequest.ON;
        }

        if (wasLitBefore != shouldBeLit) {
            world.setBlockAndUpdate(pos, state.setValue(LIT, shouldBeLit));
            world.getLightEngine().checkBlock(pos);
            world.updateNeighborsAt(pos, this);
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

            Block newBlock = null;
            if (block instanceof WallLightBlock) {
                DeferredBlock<Block> newBlockHolder = Searchlight.WALL_LIGHTS.get(normalizedColor);
                if (newBlockHolder != null) newBlock = newBlockHolder.get();
            } else if (block instanceof CornerLightBlock) {
                DeferredBlock<Block> newBlockHolder = Searchlight.CORNER_LIGHTS.get(normalizedColor);
                if (newBlockHolder != null) newBlock = newBlockHolder.get();
            } else if (block instanceof EdgeLightBlock) {
                DeferredBlock<Block> newBlockHolder = Searchlight.EDGE_LIGHTS.get(normalizedColor);
                if (newBlockHolder != null) newBlock = newBlockHolder.get();
            } else if (block instanceof CentreLightBlock) {
                DeferredBlock<Block> newBlockHolder = Searchlight.CENTRE_LIGHTS.get(normalizedColor);
                if (newBlockHolder != null) newBlock = newBlockHolder.get();
            } else if (block instanceof ColourLampBlock) {
                DeferredBlock<Block> newBlockHolder = Searchlight.COLOUR_LAMPS.get(normalizedColor);
                if (newBlockHolder != null) newBlock = newBlockHolder.get();
            } else if (block instanceof com.csykes.searchlight.features.colour_lamp_slab.ColourLampSlabBlock) {
                DeferredBlock<Block> newBlockHolder = Searchlight.COLOUR_SLAB_LAMPS.get(normalizedColor);
                if (newBlockHolder != null) newBlock = newBlockHolder.get();
            } else if (block instanceof SearchlightBlock) {
                DeferredBlock<Block> newBlockHolder = Searchlight.SEARCHLIGHTS.get(normalizedColor);
                if (newBlockHolder != null) newBlock = newBlockHolder.get();
            }

            if (newBlock != null && newBlock != block) {
                BlockState newState = copyMatchingProperties(state, newBlock.defaultBlockState());
                String address = "";
                BrightnessStage oldBrightness = BrightnessStage.MEDIUM;
                LightRequest oldLightRequest = LightRequest.RELEASE;
                BlockEntity oldBe = world.getBlockEntity(pos);
                if (oldBe instanceof AddressableLight addressable) {
                    address = addressable.getAddress();
                    oldBrightness = addressable.getBrightness();
                    oldLightRequest = addressable.getLightRequest();
                }

                if (block instanceof CornerLightBlock) {
                    List<BlockPos> connected = SearchlightUtil.getConnectedCornerLights(world, pos, state);
                    for (BlockPos connectedPos : connected) {
                        BlockState s = world.getBlockState(connectedPos);
                        BlockState ns = copyMatchingProperties(s, newBlock.defaultBlockState());
                        world.setBlockAndUpdate(connectedPos, ns);
                        world.updateNeighborsAt(connectedPos, newBlock);
                    }
                } else {
                    world.setBlockAndUpdate(pos, newState);
                    world.updateNeighborsAt(pos, newBlock);
                }

                BlockEntity newBe = world.getBlockEntity(pos);
                if (newBe instanceof AddressableLight addressable) {
                    addressable.setAddress(address);
                    addressable.setBrightness(oldBrightness);
                    addressable.setLightRequest(oldLightRequest);
                    newBe.setChanged();
                    world.sendBlockUpdated(pos, newState, newState, 3);
                    world.getLightEngine().checkBlock(pos);
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
                    if (this instanceof CornerLightBlock) {
                        List<BlockPos> connected = SearchlightUtil.getConnectedCornerLights(world, pos, state);
                        for (BlockPos connectedPos : connected) {
                            BlockEntity targetBe = world.getBlockEntity(connectedPos);
                            if (targetBe instanceof AddressableLight targetLight) {
                                targetLight.setBrightness(next);
                                targetBe.setChanged();
                                BlockState targetState = world.getBlockState(connectedPos);
                                world.sendBlockUpdated(connectedPos, targetState, targetState, 3);
                                world.getLightEngine().checkBlock(connectedPos);
                                world.updateNeighborsAt(connectedPos, targetState.getBlock());
                            }
                        }
                    } else {
                        light.setBrightness(next);
                        be.setChanged();
                        world.sendBlockUpdated(pos, state, state, 3);
                        world.getLightEngine().checkBlock(pos);
                        world.updateNeighborsAt(pos, this);
                        if (be instanceof SearchlightBlockEntity searchlight && searchlight.getLightSourcePos() != null) {
                            world.getLightEngine().checkBlock(searchlight.getLightSourcePos());
                            BlockState lsState = world.getBlockState(searchlight.getLightSourcePos());
                            world.sendBlockUpdated(searchlight.getLightSourcePos(), lsState, lsState, 3);
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
        for (net.minecraft.world.level.block.state.properties.Property<?> property : from.getProperties()) {
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