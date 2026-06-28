package com.csykes.searchlight.features.lighting_director;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.utils.lighting.CornerLightStage;
import com.csykes.searchlight.utils.lighting.AddressableLight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LightingDirectorBlockEntity extends BlockEntity {
    private final List<BlockPos> linkedLights = new ArrayList<>(Collections.nCopies(64, (BlockPos) null));

    public LightingDirectorBlockEntity(BlockPos pos, BlockState state) {
        super(Searchlight.LIGHTING_DIRECTOR_BE.get(), pos, state);
    }

    private List<BlockPos> getConnectedCornerLights(Level level, BlockPos startPos, BlockState startState) {
        List<BlockPos> positions = new ArrayList<>();
        if (!(startState.getBlock() instanceof CornerLightBlock)) {
            positions.add(startPos);
            return positions;
        }

        CornerLightStage targetCorner = startState.getValue(CornerLightBlock.CORNER);
        positions.add(startPos);

        // Traverse UP
        BlockPos current = startPos.above();
        while (true) {
            BlockState state = level.getBlockState(current);
            if (state.getBlock() instanceof CornerLightBlock && state.getValue(CornerLightBlock.CORNER) == targetCorner) {
                positions.add(current);
                current = current.above();
            } else {
                break;
            }
        }

        // Traverse DOWN
        current = startPos.below();
        while (true) {
            BlockState state = level.getBlockState(current);
            if (state.getBlock() instanceof CornerLightBlock && state.getValue(CornerLightBlock.CORNER) == targetCorner) {
                positions.add(current);
                current = current.below();
            } else {
                break;
            }
        }

        return positions;
    }

    public int toggleLinkedLight(BlockPos pos, Level level) {
        int existingSlot = -1;
        List<BlockPos> connected = new ArrayList<>();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CornerLightBlock) {
            connected.addAll(getConnectedCornerLights(level, pos, state));
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
                            addressable.setAddress("light_" + (i + 1));
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
                list.add(posTag);
            }
        }
        tag.put("linked_lights", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        Collections.fill(linkedLights, null);
        if (tag.contains("linked_lights", 9)) {
            ListTag list = tag.getList("linked_lights", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag posTag = list.getCompound(i);
                int slot = posTag.getInt("slot");
                if (slot >= 0 && slot < linkedLights.size()) {
                    linkedLights.set(slot, new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z")));
                }
            }
        }
    }
}
