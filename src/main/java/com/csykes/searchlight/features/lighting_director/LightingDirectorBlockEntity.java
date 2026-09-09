package com.csykes.searchlight.features.lighting_director;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.AddressableLight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LightingDirectorBlockEntity extends BlockEntity {
    public record LinkedLightEntry(int slot, BlockPos pos, String lightType, String address, String mode, int connectedCount) {}

    private final List<BlockPos> linkedLights = new ArrayList<>(Collections.nCopies(64, (BlockPos) null));
    private final Map<Integer, LinkedLightEntry> cachedEntries = new HashMap<>();

    public LightingDirectorBlockEntity(BlockPos pos, BlockState state) {
        super(Searchlight.LIGHTING_DIRECTOR_BE.get(), pos, state);
    }

    public int toggleLinkedLight(BlockPos pos, Level level) {
        int existingSlot = -1;
        List<BlockPos> connected = new ArrayList<>();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AbstractLightBlock alb) {
            connected.addAll(alb.getConnectedLights(level, pos, state));
        } else {
            connected.add(pos);
        }

        for (int i = 0; i < linkedLights.size(); i++) {
            BlockPos p = linkedLights.get(i);
            if (p != null && connected.contains(p)) {
                existingSlot = i;
                break;
            }
        }

        if (existingSlot != -1) {
            linkedLights.set(existingSlot, null);
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            return -(existingSlot + 1);
        } else {
            for (int i = 0; i < linkedLights.size(); i++) {
                if (linkedLights.get(i) == null) {
                    linkedLights.set(i, pos);
                    
                    BlockEntity targetBe = level.getBlockEntity(pos);
                    if (targetBe instanceof AddressableLight addressable) {
                        if (addressable.getAddress() == null || addressable.getAddress().trim().isEmpty()) {
                            addressable.setAddress(String.format("%03d", i + 1));
                            targetBe.setChanged();
                            level.sendBlockUpdated(pos, targetBe.getBlockState(), targetBe.getBlockState(), 3);
                        }
                    }

                    setChanged();
                    if (level != null && !level.isClientSide) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    }
                    return i + 1;
                }
            }
        }
        return 0;
    }

    public boolean removeLinkedLight(int index) {
        if (index >= 0 && index < linkedLights.size()) {
            if (linkedLights.get(index) != null) {
                linkedLights.set(index, null);
                cachedEntries.remove(index);
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                return true;
            }
        }
        return false;
    }

    public void clearLinkedLights() {
        boolean changed = false;
        for (int i = 0; i < linkedLights.size(); i++) {
            if (linkedLights.get(i) != null) {
                linkedLights.set(i, null);
                changed = true;
            }
        }
        cachedEntries.clear();
        if (changed) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public List<BlockPos> getLinkedLights() {
        return linkedLights;
    }

    public List<LinkedLightEntry> getLinkedLightEntries() {
        List<LinkedLightEntry> entries = new ArrayList<>();
        for (int i = 0; i < linkedLights.size(); i++) {
            BlockPos p = linkedLights.get(i);
            if (p != null) {
                String name = "Light";
                String address = "";
                String mode = "Fixture";
                int count = 1;

                if (level != null && level.isLoaded(p)) {
                    BlockState st = level.getBlockState(p);
                    name = st.getBlock().getName().getString();
                    BlockEntity be = level.getBlockEntity(p);
                    if (be instanceof AddressableLight al) {
                        address = al.getAddress();
                        mode = al.getLightMode().getDisplayName();
                    }
                    if (st.getBlock() instanceof AbstractLightBlock alb) {
                        count = alb.getConnectedLights(level, p, st).size();
                    }
                } else if (cachedEntries.containsKey(i)) {
                    LinkedLightEntry cached = cachedEntries.get(i);
                    name = cached.lightType();
                    address = cached.address();
                    mode = cached.mode();
                    count = cached.connectedCount();
                }

                entries.add(new LinkedLightEntry(i + 1, p, name, address, mode, count));
            }
        }
        return entries;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ListTag list = new ListTag();
        for (int i = 0; i < linkedLights.size(); i++) {
            BlockPos pos = linkedLights.get(i);
            if (pos != null) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("slot", i);
                posTag.putInt("x", pos.getX());
                posTag.putInt("y", pos.getY());
                posTag.putInt("z", pos.getZ());

                String name = "Light";
                String address = "";
                String mode = "Fixture";
                int count = 1;

                if (level != null && level.isLoaded(pos)) {
                    BlockState st = level.getBlockState(pos);
                    name = st.getBlock().getName().getString();
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof AddressableLight al) {
                        address = al.getAddress();
                        mode = al.getLightMode().getDisplayName();
                    }
                    if (st.getBlock() instanceof AbstractLightBlock alb) {
                        count = alb.getConnectedLights(level, pos, st).size();
                    }
                } else if (cachedEntries.containsKey(i)) {
                    LinkedLightEntry cached = cachedEntries.get(i);
                    name = cached.lightType();
                    address = cached.address();
                    mode = cached.mode();
                    count = cached.connectedCount();
                }

                posTag.putString("name", name);
                posTag.putString("address", address);
                posTag.putString("mode", mode);
                posTag.putInt("count", count);

                list.add(posTag);
            }
        }
        tag.put("linked_lights", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        Collections.fill(linkedLights, null);
        cachedEntries.clear();
        if (tag.contains("linked_lights", 9)) {
            ListTag list = tag.getList("linked_lights", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag posTag = list.getCompound(i);
                int slot = posTag.getInt("slot");
                if (slot >= 0 && slot < linkedLights.size()) {
                    BlockPos p = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
                    linkedLights.set(slot, p);
                    String name = posTag.contains("name") ? posTag.getString("name") : "Light";
                    String address = posTag.contains("address") ? posTag.getString("address") : "";
                    String mode = posTag.contains("mode") ? posTag.getString("mode") : "Fixture";
                    int count = posTag.contains("count") ? posTag.getInt("count") : 1;
                    cachedEntries.put(slot, new LinkedLightEntry(slot + 1, p, name, address, mode, count));
                }
            }
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
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
