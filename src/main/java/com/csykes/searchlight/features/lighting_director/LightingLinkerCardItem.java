package com.csykes.searchlight.features.lighting_director;

import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class LightingLinkerCardItem extends Item {
    public LightingLinkerCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockState state = level.getBlockState(clickedPos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // 1. If clicked on a Director, pair the card
        if (state.getBlock() instanceof LightingDirectorBlock) {
            String dimension = level.dimension().location().toString();
            CustomData.update(DataComponents.CUSTOM_DATA, stack, (tag) -> {
                tag.putInt("director_x", clickedPos.getX());
                tag.putInt("director_y", clickedPos.getY());
                tag.putInt("director_z", clickedPos.getZ());
                tag.putString("director_dim", dimension);
            });

            if (player != null) {
                player.sendSystemMessage(Component.literal("Paired card with Wireless Lighting Director at (" + clickedPos.getX() + ", " + clickedPos.getY() + ", " + clickedPos.getZ() + ")"));
            }
            return InteractionResult.SUCCESS;
        }

        // 2. If clicked on a Light Block, toggle its registration in the paired Director
        if (state.getBlock() instanceof AbstractLightBlock) {
            if (player != null && player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("This card is not paired to a Wireless Lighting Director. Right-click a Director first."));
                }
                return InteractionResult.SUCCESS;
            }

            CompoundTag tag = customData.copyTag();
            if (!tag.contains("director_x") || !tag.contains("director_y") || !tag.contains("director_z")) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("This card is not paired to a Wireless Lighting Director. Right-click a Director first."));
                }
                return InteractionResult.SUCCESS;
            }

            String currentDim = level.dimension().location().toString();
            String storedDim = tag.getString("director_dim");
            if (!currentDim.equals(storedDim)) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("Cannot toggle lights in a different dimension."));
                }
                return InteractionResult.SUCCESS;
            }

            int dx = tag.getInt("director_x");
            int dy = tag.getInt("director_y");
            int dz = tag.getInt("director_z");
            BlockPos directorPos = new BlockPos(dx, dy, dz);

            BlockEntity be = level.getBlockEntity(directorPos);
            if (be instanceof LightingDirectorBlockEntity directorBe) {
                int slotResult = directorBe.toggleLinkedLight(clickedPos, level);
                if (player != null) {
                    if (slotResult < 0) {
                        player.sendSystemMessage(Component.literal("Removed light from Director (Slot " + (-slotResult) + ")"));
                    } else if (slotResult > 0) {
                        player.sendSystemMessage(Component.literal("Linked light to Director at Slot " + slotResult));
                    } else {
                        player.sendSystemMessage(Component.literal("Wireless Lighting Director is full!"));
                    }
                }
            } else {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("Could not find paired Director at (" + dx + ", " + dy + ", " + dz + "). Is it too far or broken?"));
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (player.isShiftKeyDown()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, (tag) -> {
                tag.remove("director_x");
                tag.remove("director_y");
                tag.remove("director_z");
                tag.remove("director_dim");
            });
            player.sendSystemMessage(Component.literal("Cleared linker card pairing."));
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("director_x") && tag.contains("director_y") && tag.contains("director_z")) {
                int x = tag.getInt("director_x");
                int y = tag.getInt("director_y");
                int z = tag.getInt("director_z");
                tooltipComponents.add(Component.literal("Paired to Director at: " + x + ", " + y + ", " + z));
                tooltipComponents.add(Component.literal("Right-click a Light Block to toggle link."));
                return;
            }
        }
        tooltipComponents.add(Component.literal("Not paired. Right-click a Wireless Lighting Director to pair."));
    }
}
