package com.mat.engine;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Server-thread engine using NeoForge's {@link FakePlayerFactory} to simulate
 * player interactions in GameTest environments.
 */
@UtilityClass
public class SimulatedPlayerEngine {

    /**
     * Simulates a player using an item ID (or empty hand) on a block at the given relative position.
     */
    public static InteractionResult interactBlockWithItem(
            GameTestHelper helper,
            BlockPos relativePos,
            String itemId,
            InteractionHand hand,
            Direction face
    ) {
        ItemStack stack = ItemStack.EMPTY;
        if (itemId != null && !itemId.isEmpty()) {
            ResourceLocation itemKey = ResourceLocation.parse(itemId);
            var itemHolder = BuiltInRegistries.ITEM.getOptional(itemKey)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown item ID: " + itemId));
            stack = new ItemStack(itemHolder);
        }
        return interactBlock(helper, relativePos, stack, hand, face);
    }

    /**
     * Simulates a player using an item (or empty hand) on a block at the given relative position.
     *
     * @param helper      The active {@link GameTestHelper}.
     * @param relativePos The relative target position.
     * @param stack       The {@link ItemStack} in hand.
     * @param hand        The {@link InteractionHand} used.
     * @param face        The target block face.
     * @return The resulting {@link InteractionResult}.
     */
    public static InteractionResult interactBlock(
            GameTestHelper helper,
            BlockPos relativePos,
            ItemStack stack,
            InteractionHand hand,
            Direction face
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos absolutePos = helper.absolutePos(relativePos);

        ServerPlayer player = FakePlayerFactory.getMinecraft(level);

        Vec3 hitVec = Vec3.atCenterOf(absolutePos);
        player.setPos(hitVec.x, hitVec.y, hitVec.z);
        player.setYRot(face.toYRot());
        player.setXRot(0.0F);

        player.setItemInHand(hand, stack.copy());

        BlockHitResult hitResult = new BlockHitResult(hitVec, face, absolutePos, false);

        return player.gameMode.useItemOn(
                player,
                level,
                player.getItemInHand(hand),
                hand,
                hitResult
        );
    }
}
