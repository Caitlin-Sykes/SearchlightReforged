package com.csykes.searchlight.features.wall_light;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.centre_light.CentreLightBlock;
import com.csykes.searchlight.features.colour_lamp.ColourLampBlock;
import com.csykes.searchlight.features.colour_lamp_slab.ColourLampSlabBlock;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.edge_light.EdgeLightBlock;
import com.csykes.searchlight.utils.lighting.AddressableLight;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.csykes.searchlight.utils.lighting.LightRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class WallLightBlockEntity extends BlockEntity implements AddressableLight {
    private String address = "";
    private BrightnessStage brightness = BrightnessStage.MEDIUM;
    private LightRequest lightRequest = LightRequest.RELEASE;

    public WallLightBlockEntity(BlockPos pos, BlockState state) {
        super(
                state.getBlock() instanceof CornerLightBlock ? Searchlight.CORNER_LIGHT_BE.get() :
                        (state.getBlock() instanceof CentreLightBlock ? Searchlight.CENTRE_LIGHT_BE.get() :
                                state.getBlock() instanceof ColourLampBlock ? Searchlight.COLOUR_LAMPS_BE.get() :
                                        state.getBlock() instanceof ColourLampSlabBlock ? Searchlight.COLOUR_LAMPS_SLAB_BE.get() :
                                                state.getBlock() instanceof EdgeLightBlock ? Searchlight.EDGE_LIGHT_BE.get() :
                                                        Searchlight.WALL_LIGHT_BE.get()),
                pos, state
        );
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("address", address);
        tag.putString("brightness", brightness.name());
        tag.putString("light_request", lightRequest.name());
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
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (level != null && level.isClientSide) {
            level.getLightEngine().checkBlock(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        if (level != null && level.isClientSide) {
            level.getLightEngine().checkBlock(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}