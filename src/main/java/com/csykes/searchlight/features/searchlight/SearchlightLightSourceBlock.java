package com.csykes.searchlight.features.searchlight;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.utils.SearchlightUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SearchlightLightSourceBlock extends Block implements EntityBlock {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    public SearchlightLightSourceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(COLOR, DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SearchlightLightSourceBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return context.isHoldingItem(Searchlight.SEARCHLIGHT_ITEM.get()) ? Shapes.block() : Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (level != null && pos != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SearchlightLightSourceBlockEntity sourceBe && sourceBe.searchlightBlockPos != null) {
                BlockEntity parentBe = level.getBlockEntity(sourceBe.searchlightBlockPos);
                if (parentBe instanceof com.csykes.searchlight.utils.lighting.AddressableLight light) {
                    BlockState parentState = level.getBlockState(sourceBe.searchlightBlockPos);
                    if (parentState.hasProperty(com.csykes.searchlight.utils.lighting.AbstractLightBlock.LIT) && !parentState.getValue(com.csykes.searchlight.utils.lighting.AbstractLightBlock.LIT)) {
                        return 0;
                    }
                    return light.getBrightness().getLightLevel();
                }
            }
        }
        return 15;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!world.isClientSide) {
            SearchlightUtil.castBlockEntity(world.getBlockEntity(pos), pos, (SearchlightLightSourceBlockEntity be) -> be.moveLightSource());
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }
}