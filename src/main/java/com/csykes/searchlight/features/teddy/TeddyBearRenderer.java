package com.csykes.searchlight.features.teddy;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TeddyBearRenderer extends MobRenderer<TeddyBearEntity, TeddyEntityModel<TeddyBearEntity>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("searchlight", "textures/entity/teddy_texture.png");

    public TeddyBearRenderer(EntityRendererProvider.Context context) {
        // The 0.5f is the size of the shadow under the bear
        super(context, new TeddyEntityModel<>(context.bakeLayer(TeddyEntityModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(TeddyBearEntity entity) {
        return TEXTURE;
    }
}