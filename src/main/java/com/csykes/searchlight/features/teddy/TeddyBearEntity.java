package com.csykes.searchlight.features.teddy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

public class TeddyBearEntity extends TamableAnimal {
    private static final EntityDataAccessor<Boolean> HUGGING =
            SynchedEntityData.defineId(TeddyBearEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> EATING_HONEY =
            SynchedEntityData.defineId(TeddyBearEntity.class, EntityDataSerializers.BOOLEAN);

    private int eatingHoneyTimer = 0;

    public TeddyBearEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.eatingHoneyTimer > 0) {
                this.eatingHoneyTimer--;
                if (this.eatingHoneyTimer == 0) {
                    this.setEatingHoney(false);
                }
            }
        }
    }

    // This is where you define the custom AI
    @Override
    protected void registerGoals() {
        // 1. PANIC IN WATER OR RAIN (Highest Priority)
        // The bear will run around frantically if it touches water or rain
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D) {
            @Override
            public boolean canUse() {
                return mob.isInWater() || mob.level().isRainingAt(mob.blockPosition()) || super.canUse();
            }
        });

        // 2. SIT COMMAND
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));

        // 3. EXCITED BY HONEY ITEMS (Tempting)
        // Follows players holding Honey Bottles or Honeycombs quickly
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, Ingredient.of(Items.HONEY_BOTTLE, Items.HONEYCOMB), false));

        // 4. HUG THE OWNER (Custom Logic)
        this.goalSelector.addGoal(4, new Goal() {
            private int hugCooldown = 0;
            private int hugDuration = 0;

            // We must define this flag to tell the AI system that
            // while hugging, the bear should stop walking and looking around.
            {
                this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                // Use TeddyBearEntity.this to reference the bear, not the goal
                if (!TeddyBearEntity.this.isTame() || TeddyBearEntity.this.getOwner() == null || TeddyBearEntity.this.isOrderedToSit())
                    return false;

                if (hugCooldown > 0) {
                    hugCooldown--;
                    return false;
                }

                // 1-in-100 chance per tick to want a hug if owner is within 5 blocks
                return TeddyBearEntity.this.getRandom().nextInt(100) == 0 &&
                        TeddyBearEntity.this.distanceToSqr(TeddyBearEntity.this.getOwner()) < 25.0D;
            }

            @Override
            public void start() {
                TeddyBearEntity.this.setHugging(true);
                hugDuration = 40; // Hug lasts for 2 seconds (40 ticks)
                TeddyBearEntity.this.getNavigation().stop();
            }

            @Override
            public void tick() {
                // 10.0F is max yaw (horizontal) turn speed, 40.0F is max pitch (vertical) turn speed
                TeddyBearEntity.this.getLookControl().setLookAt(TeddyBearEntity.this.getOwner(), 10.0F, 40.0F);
                hugDuration--;
            }

            @Override
            public boolean canContinueToUse() {
                return hugDuration > 0 &&
                        TeddyBearEntity.this.getOwner() != null &&
                        TeddyBearEntity.this.distanceToSqr(TeddyBearEntity.this.getOwner()) < 36.0D;
            }

            @Override
            public void stop() {
                TeddyBearEntity.this.setHugging(false);
                hugCooldown = 400 + TeddyBearEntity.this.getRandom().nextInt(400);
            }
        });

        // 5. SEEK BEEHIVES AND HONEY BLOCKS
        this.goalSelector.addGoal(5, new MoveToBlockGoal(this, 1.1D, 16) {
            @Override
            protected boolean isValidTarget(LevelReader level, BlockPos pos) {
                return level.getBlockState(pos).is(Blocks.BEEHIVE) ||
                        level.getBlockState(pos).is(Blocks.BEE_NEST) ||
                        level.getBlockState(pos).is(Blocks.HONEY_BLOCK);
            }
        });

        // 6. FOLLOW OWNER
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));

        // 7. WANDER AROUND
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        // 8. IDLE LOOKING
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    // Define core attributes (health, speed, etc.)
    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HUGGING, false);
        builder.define(EATING_HONEY, false);
    }

    public boolean isHugging() {
        return this.entityData.get(HUGGING);
    }

    public void setHugging(boolean hugging) {
        this.entityData.set(HUGGING, hugging);
    }

    public boolean isEatingHoney() {
        return this.entityData.get(EATING_HONEY);
    }

    public void setEatingHoney(boolean eating) {
        this.entityData.set(EATING_HONEY, eating);
        if (eating) {
            this.eatingHoneyTimer = 60;
        }
    }

    public boolean isBeggingForHoney() {
        Player player = this.level().getNearestPlayer(this, 6.0D);
        if (player != null) {
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            return main.is(Items.HONEY_BOTTLE) || main.is(Items.HONEYCOMB) ||
                   off.is(Items.HONEY_BOTTLE) || off.is(Items.HONEYCOMB);
        }
        return false;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(Items.HONEYCOMB) || itemStack.is(Items.HONEY_BOTTLE);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // Tame the bear with Honeycomb or Honey Bottle
        if ((itemstack.is(Items.HONEYCOMB) || itemstack.is(Items.HONEY_BOTTLE)) && !this.isTame()) {
            if (!this.level().isClientSide) {
                this.tame(player);
                itemstack.consume(1, player);
                this.setEatingHoney(true);
                this.navigation.stop();
                // Spawn hearts
                this.level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Heal/Feed Honey when tamed
        if (this.isTame() && (itemstack.is(Items.HONEYCOMB) || itemstack.is(Items.HONEY_BOTTLE))) {
            if (!this.level().isClientSide) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(5.0F);
                }
                itemstack.consume(1, player);
                this.setEatingHoney(true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Sit/Stand if already tamed and interacted with by owner
        if (this.isTame() && this.isOwnedBy(player) && !this.isFood(itemstack)) {
            if (!this.level().isClientSide) {
                this.setOrderedToSit(!this.isOrderedToSit());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }
}