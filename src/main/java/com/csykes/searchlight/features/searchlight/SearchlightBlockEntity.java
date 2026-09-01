package com.csykes.searchlight.features.searchlight;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.utils.SearchlightUtil;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.csykes.searchlight.MutableVector3d;
import com.csykes.searchlight.utils.lighting.AddressableLight;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.csykes.searchlight.utils.lighting.LightRequest;
import net.minecraft.world.item.DyeColor;

public class SearchlightBlockEntity extends BlockEntity implements AddressableLight {
    private @Nullable BlockPos lightSourcePos;
    private String address = "";
    private BrightnessStage brightness = BrightnessStage.MEDIUM;
    private LightRequest lightRequest = LightRequest.RELEASE;

    public SearchlightBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(Searchlight.SEARCHLIGHT_BE.get(), blockPos, blockState);
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
        setChanged();
    }

    @Override
    public BrightnessStage getBrightness() {
        return brightness;
    }

    @Override
    public void setBrightness(BrightnessStage brightness) {
        this.brightness = brightness != null ? brightness : BrightnessStage.MEDIUM;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.getLightEngine().checkBlock(worldPosition);
            if (lightSourcePos != null) {
                level.getLightEngine().checkBlock(lightSourcePos);
            }
        }
    }

    @Override
    public LightRequest getLightRequest() {
        return lightRequest;
    }

    @Override
    public void setLightRequest(LightRequest lightRequest) {
        this.lightRequest = lightRequest != null ? lightRequest : LightRequest.RELEASE;
        setChanged();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (level != null && level.isClientSide) {
            level.getLightEngine().checkBlock(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (lightSourcePos != null) {
                level.getLightEngine().checkBlock(lightSourcePos);
                level.sendBlockUpdated(lightSourcePos, level.getBlockState(lightSourcePos), level.getBlockState(lightSourcePos), 3);
            }
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        if (level != null && level.isClientSide) {
            level.getLightEngine().checkBlock(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (lightSourcePos != null) {
                level.getLightEngine().checkBlock(lightSourcePos);
                level.sendBlockUpdated(lightSourcePos, level.getBlockState(lightSourcePos), level.getBlockState(lightSourcePos), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("address", address);
        tag.putString("brightness", brightness.name());
        tag.putString("light_request", lightRequest.name());
        if (lightSourcePos != null) {
            tag.putInt("light_source_x", lightSourcePos.getX());
            tag.putInt("light_source_y", lightSourcePos.getY());
            tag.putInt("light_source_z", lightSourcePos.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.address = tag.getString("address");
        if (tag.contains("brightness")) {
            try {
                this.brightness = BrightnessStage.valueOf(tag.getString("brightness"));
            } catch (IllegalArgumentException e) {
                this.brightness = BrightnessStage.MEDIUM;
            }
        } else {
            this.brightness = BrightnessStage.MEDIUM;
        }
        if (tag.contains("light_request")) {
            try {
                this.lightRequest = LightRequest.valueOf(tag.getString("light_request"));
            } catch (IllegalArgumentException e) {
                this.lightRequest = LightRequest.RELEASE;
            }
        } else {
            this.lightRequest = LightRequest.RELEASE;
        }
        if (tag.contains("light_source_x") && tag.contains("light_source_y") && tag.contains("light_source_z")) {
            lightSourcePos = new BlockPos(tag.getInt("light_source_x"), tag.getInt("light_source_y"), tag.getInt("light_source_z"));
        } else {
            lightSourcePos = null;
        }
    }

    public DyeColor getColor() {
        BlockState state = getBlockState();
        if (state.hasProperty(SearchlightBlock.COLOR)) {
            return state.getValue(SearchlightBlock.COLOR);
        }
        return DyeColor.WHITE;
    }

    public void setColor(DyeColor color) {
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(SearchlightBlock.COLOR)) {
                level.setBlockAndUpdate(getBlockPos(), state.setValue(SearchlightBlock.COLOR, color));
                if (lightSourcePos != null) {
                    BlockState lightState = level.getBlockState(lightSourcePos);
                    if (lightState.is(Searchlight.LIGHT_SOURCE_BLOCK.get()) && lightState.hasProperty(SearchlightLightSourceBlock.COLOR)) {
                        level.setBlockAndUpdate(lightSourcePos, lightState.setValue(SearchlightLightSourceBlock.COLOR, color));
                    }
                }
            }
        }
    }

    public @Nullable BlockPos getLightSourcePos() {
        return lightSourcePos;
    }

    public @NotNull Vec3 getBeamDirection() {
        if (lightSourcePos == null)
            return SearchlightUtil.directionToBeamVector(SearchlightUtil.getDirection(getBlockState()));
        BlockPos delta = lightSourcePos.subtract(getBlockPos());
        return new Vec3(delta.getX(), delta.getY(), delta.getZ()).normalize();
    }

    public boolean deleteLightSource() {
        if (level == null || level.isClientSide) return false;
        BlockPos oldLightSourcePos = lightSourcePos;
        this.lightSourcePos = null;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        if (oldLightSourcePos != null && level.getBlockState(oldLightSourcePos).getBlock() instanceof SearchlightLightSourceBlock) {
            SearchlightUtil.castBlockEntity(level.getBlockEntity(oldLightSourcePos), oldLightSourcePos, (SearchlightLightSourceBlockEntity be) -> {
                be.suppressMovement = true;
            });
            return level.setBlockAndUpdate(oldLightSourcePos, Blocks.AIR.defaultBlockState());
        }
        return false;
    }

    public boolean turnOffLightSource() {
        BlockPos lightPos = getLightSourcePos();
        if (lightPos == null)
            return false;

        if (level != null && !level.isClientSide && level.getBlockState(lightPos).getBlock() instanceof SearchlightLightSourceBlock) {
            SearchlightUtil.castBlockEntity(level.getBlockEntity(lightPos), lightPos, (SearchlightLightSourceBlockEntity be) -> {
                be.suppressMovement = true;
            });
            level.setBlockAndUpdate(lightPos, Blocks.AIR.defaultBlockState());
        }

        setChanged();
        return true;
    }

    public boolean turnOnLightSource() {
        if (lightSourcePos != null) {
            BlockState currentState = level.getBlockState(lightSourcePos);
            if (currentState.isAir() || currentState.getBlock() instanceof SearchlightLightSourceBlock) {
                return placeLightSource(lightSourcePos);
            }
            return this.raycastAndPlaceLightSource(getBeamDirection());
        }

        return this.raycastAndPlaceLightSource(SearchlightUtil.directionToBeamVector(SearchlightUtil.getDirection(getBlockState())));
    }

    public boolean raycastAndPlaceLightSource(@NotNull Vec3 beamDirection) {
        if (beamDirection.equals(Vec3.ZERO)) return false;
        beamDirection = beamDirection.normalize();
        BlockPos newLightPos = calculateLightSourcePosition(beamDirection);

        if (getBlockState().getValue(AbstractLightBlock.LIT)) {
            return placeLightSource(newLightPos);
        }

        setLightSourcePos(newLightPos);
        return true;
    }

    public boolean placeLightSource(@Nullable BlockPos newLightPos) {
        if (newLightPos == null) {
            deleteLightSource();
            return false;
        }

        if (level == null || level.isClientSide) return false;

        // If there's an existing light source somewhere else, delete it
        if (lightSourcePos != null && !lightSourcePos.equals(newLightPos)) {
            deleteLightSource();
        }

        BlockState oldBlockState = level.getBlockState(newLightPos);
        DyeColor currentColor = getColor();
        BlockState lightSourceState = Searchlight.LIGHT_SOURCE_BLOCK.get().defaultBlockState().setValue(SearchlightLightSourceBlock.COLOR, currentColor);
        if (!level.setBlockAndUpdate(newLightPos, lightSourceState))
            return false;

        if (!SearchlightUtil.castBlockEntity(level.getBlockEntity(newLightPos), newLightPos, (SearchlightLightSourceBlockEntity lightBlockEntity) -> {
            lightBlockEntity.searchlightBlockPos = getBlockPos();
            setLightSourcePos(newLightPos);
        })) {
            level.setBlockAndUpdate(newLightPos, oldBlockState);
            // If it failed to place, and it wasn't already there, clear it
            if (lightSourcePos != null && lightSourcePos.equals(newLightPos)) {
                setLightSourcePos(null);
            }
            return false;
        }
        return true;
    }

    public @Nullable BlockPos calculateLightSourcePosition(@NotNull Vec3 beamDirection) {
        if (beamDirection.equals(Vec3.ZERO)) return null;
        beamDirection = beamDirection.normalize();
        MutableVector3d currentBlockPosD = new MutableVector3d(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5);
        BlockPos.MutableBlockPos currentBlockPos = new BlockPos.MutableBlockPos(currentBlockPosD.x, currentBlockPosD.y, currentBlockPosD.z);
        BlockPos.MutableBlockPos prevBlockPos = new BlockPos.MutableBlockPos(0, 0, 0);
        BlockPos lastValidBlockPos = null;
        int distance = 0;
        int safetySteps = 0;

        while (distance < Searchlight.MAX_DISTANCE) {
            prevBlockPos.set(currentBlockPos);
            currentBlockPosD.add(beamDirection);
            currentBlockPos.set(currentBlockPosD.x, currentBlockPosD.y, currentBlockPosD.z);
            if (prevBlockPos.equals(currentBlockPos)) {
                safetySteps++;
                if (safetySteps > 2000) {
                    break;
                }
                continue;
            }
            distance++;
            if (!level.isInWorldBounds(currentBlockPos)) return null;
            if (!level.isLoaded(currentBlockPos)) return null;

            BlockState currentBlockState = SearchlightUtil.getBlockStateForceLoad(level, currentBlockPos);
            if (!currentBlockState.isAir() && !currentBlockPos.equals(lightSourcePos) && !currentBlockPos.equals(getBlockPos())) {
                // Simplistic opacity check for now, can be improved to match vanilla realistic opacity if needed
                if (currentBlockState.getLightBlock(level, currentBlockPos) >= level.getMaxLightLevel() || !level.getFluidState(currentBlockPos).isEmpty()) {
                    return SearchlightUtil.moveAwayFromSurfaces(level, lastValidBlockPos);
                }
            }
            if (currentBlockState.isAir() || currentBlockPos.equals(lightSourcePos))
                lastValidBlockPos = currentBlockPos.immutable();
        }
        return lastValidBlockPos;
    }

    protected void setLightSourcePos(@Nullable BlockPos lightSourcePos) {
        this.lightSourcePos = lightSourcePos;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}