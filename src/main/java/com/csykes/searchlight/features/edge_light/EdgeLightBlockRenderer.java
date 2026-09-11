package com.csykes.searchlight.features.edge_light;

import com.csykes.searchlight.utils.lighting.LightMode;
import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.ColorAveragingHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

import java.util.Objects;

/**
 * Renders individual edge pixel colors dynamically for {@link EdgeLightBlock}
 * without requiring chunk mesh re-baking. Corners and spans are handled without gaps or overlap.
 */
public class EdgeLightBlockRenderer implements BlockEntityRenderer<WallLightBlockEntity> {

    private static final ResourceLocation ROD_ON_TEXTURE = ResourceLocation.fromNamespaceAndPath("searchlight", "textures/block/edge_light_rod_on.png");
    private static final int FULL_BRIGHT = 15728880;
    private static final float EPS = 0.002f;
    private static final float U_MIN = 0.0f;
    private static final float U_MAX = 0.125f;

    public EdgeLightBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WallLightBlockEntity tile, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof EdgeLightBlock)) return;
        if (tile.getLightMode() != LightMode.PIXEL && tile.getEdgeLightData().isUniform(state)) return;

        EdgeLightData data = tile.getEdgeLightData();
        AttachFace face = state.hasProperty(EdgeLightBlock.FACE) ? state.getValue(EdgeLightBlock.FACE) : AttachFace.FLOOR;

        boolean hasNorth = state.hasProperty(EdgeLightBlock.NORTH) && state.getValue(EdgeLightBlock.NORTH) && data.isEdgeLit(Direction.NORTH);
        boolean hasSouth = state.hasProperty(EdgeLightBlock.SOUTH) && state.getValue(EdgeLightBlock.SOUTH) && data.isEdgeLit(Direction.SOUTH);
        boolean hasEast  = state.hasProperty(EdgeLightBlock.EAST)  && state.getValue(EdgeLightBlock.EAST)  && data.isEdgeLit(Direction.EAST);
        boolean hasWest  = state.hasProperty(EdgeLightBlock.WEST)  && state.getValue(EdgeLightBlock.WEST)  && data.isEdgeLit(Direction.WEST);

        if (!hasNorth && !hasSouth && !hasEast && !hasWest) return;

        float y0 = (face == AttachFace.CEILING) ? (14.0f / 16.0f - EPS) : (-EPS);
        float y1 = (face == AttachFace.CEILING) ? (1.0f + EPS) : (2.0f / 16.0f + EPS);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ROD_ON_TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        // NORTH: runs along X (X: 0..16, Z: 0..2). Owns NW and NE corners when active.
        if (hasNorth) {
            EdgeLightData.EdgeState edgeState = data.getEdge(Direction.NORTH);
            float z0 = -EPS;
            float z1 = 2.0f / 16.0f + EPS;
            renderEdgeXRod(y0, y1, consumer, pose, edgeState, z0, z1);
        }

        // SOUTH: runs along X (X: 0..16, Z: 14..16). Owns SW and SE corners when active.
        if (hasSouth) {
            EdgeLightData.EdgeState edgeState = data.getEdge(Direction.SOUTH);
            float z0 = 14.0f / 16.0f - EPS;
            float z1 = 1.0f + EPS;
            renderEdgeXRod(y0, y1, consumer, pose, edgeState, z0, z1);
        }

        // EAST: runs along Z (X: 14..16, Z: 0..16). Extends to 0/16 if North/South aren't lit.
        if (hasEast) {
            EdgeLightData.EdgeState edgeState = data.getEdge(Direction.EAST);
            float x0 = 14.0f / 16.0f - EPS;
            float x1 = 1.0f + EPS;
            int startSeg = hasNorth ? 1 : 0;
            int endSeg = hasSouth ? 6 : 7;
            renderEdgeZRod(edgeState, hasNorth, hasSouth, consumer, pose, x0, x1, y0, y1, startSeg, endSeg);
        }

        // WEST: runs along Z (X: 0..2, Z: 0..16). Extends to 0/16 if North/South aren't lit.
        if (hasWest) {
            EdgeLightData.EdgeState edgeState = data.getEdge(Direction.WEST);
            float x0 = -EPS;
            float x1 = 2.0f / 16.0f + EPS;
            int startSeg = hasNorth ? 1 : 0;
            int endSeg = hasSouth ? 6 : 7;
            renderEdgeZRod(edgeState, hasNorth, hasSouth, consumer, pose, x0, x1, y0, y1, startSeg, endSeg);
        }
    }

    private void renderEdgeZRod(EdgeLightData.EdgeState edgeState, boolean hasNorth, boolean hasSouth, VertexConsumer consumer, PoseStack.Pose pose, float x0, float x1, float y0, float y1, int startSeg, int endSeg) {
        if (edgeState.isUniform()) {
            int color = ColorAveragingHelper.getRgbForColor(edgeState.getColor());
            float z0 = hasNorth ? (2.0f / 16.0f) : (-EPS);
            float z1 = hasSouth ? (14.0f / 16.0f) : (1.0f + EPS);
            renderZRod(consumer, pose, x0, x1, y0, y1, z0, z1, color, !hasNorth, !hasSouth);
        } else {
            renderZRodSubPixels(consumer, pose, edgeState, x0, x1, y0, y1, startSeg, endSeg);
        }
    }

    private void renderEdgeXRod(float y0, float y1, VertexConsumer consumer, PoseStack.Pose pose, EdgeLightData.EdgeState edgeState, float z0, float z1) {
        if (edgeState.isUniform()) {
            int color = ColorAveragingHelper.getRgbForColor(edgeState.getColor());
            renderXRod(consumer, pose, -EPS, 1.0f + EPS, y0, y1, z0, z1, color, true, true);
        } else {
            renderXRodSubPixels(consumer, pose, edgeState, y0, y1, z0, z1, 0, 7);
        }
    }

    private void renderXRodSubPixels(VertexConsumer consumer, PoseStack.Pose pose,
                                     EdgeLightData.EdgeState edgeState,
                                     float y0, float y1, float z0, float z1,
                                     int startSeg, int endSeg) {
        for (int i = startSeg; i <= endSeg; i++) {
            if (!edgeState.isSubLit(i)) continue;

            int color = ColorAveragingHelper.getRgbForColor(edgeState.getSubColor(i));
            float x0 = (i == 0) ? -EPS : (i * 0.125f);
            float x1 = (i == 7) ? (1.0f + EPS) : ((i + 1) * 0.125f);

            boolean capStart = (i == startSeg && i == 0) || (i > startSeg && (!edgeState.isSubLit(i - 1) || !Objects.equals(edgeState.getSubColor(i), edgeState.getSubColor(i - 1))));
            boolean capEnd = (i == endSeg && i == 7) || (i < endSeg && (!edgeState.isSubLit(i + 1) || !Objects.equals(edgeState.getSubColor(i), edgeState.getSubColor(i + 1))));

            renderXRod(consumer, pose, x0, x1, y0, y1, z0, z1, color, capStart, capEnd);
        }
    }

    private void renderZRodSubPixels(VertexConsumer consumer, PoseStack.Pose pose,
                                     EdgeLightData.EdgeState edgeState,
                                     float x0, float x1, float y0, float y1,
                                     int startSeg, int endSeg) {
        for (int i = startSeg; i <= endSeg; i++) {
            if (!edgeState.isSubLit(i)) continue;

            int color = ColorAveragingHelper.getRgbForColor(edgeState.getSubColor(i));
            float z0 = (i == 0) ? -EPS : (i * 0.125f);
            float z1 = (i == 7) ? (1.0f + EPS) : ((i + 1) * 0.125f);

            boolean capStart = (i == startSeg && i == 0) || (i > startSeg && (!edgeState.isSubLit(i - 1) || !Objects.equals(edgeState.getSubColor(i), edgeState.getSubColor(i - 1))));
            boolean capEnd = (i == endSeg && i == 7) || (i < endSeg && (!edgeState.isSubLit(i + 1) || !Objects.equals(edgeState.getSubColor(i), edgeState.getSubColor(i + 1))));

            renderZRod(consumer, pose, x0, x1, y0, y1, z0, z1, color, capStart, capEnd);
        }
    }

    private void renderXRod(VertexConsumer consumer, PoseStack.Pose pose,
                            float x0, float x1, float y0, float y1, float z0, float z1,
                            int color, boolean capStart, boolean capEnd) {
        float v0 = Math.clamp(x0, 0.0f, 1.0f);
        float v1 = Math.clamp(x1, 0.0f, 1.0f);

        // UP (facing +Y)
        quad(consumer, pose,
                x0, y1, z0, U_MAX, v0,
                x0, y1, z1, U_MIN, v0,
                x1, y1, z1, U_MIN, v1,
                x1, y1, z0, U_MAX, v1,
                color, 0.0f, 1.0f, 0.0f);

        // DOWN (facing -Y)
        quad(consumer, pose,
                x0, y0, z1, U_MIN, v0,
                x0, y0, z0, U_MAX, v0,
                x1, y0, z0, U_MAX, v1,
                x1, y0, z1, U_MIN, v1,
                color, 0.0f, -1.0f, 0.0f);

        // NORTH (facing -Z)
        quad(consumer, pose,
                x1, y1, z0, U_MAX, v1,
                x1, y0, z0, U_MIN, v1,
                x0, y0, z0, U_MIN, v0,
                x0, y1, z0, U_MAX, v0,
                color, 0.0f, 0.0f, -1.0f);

        // SOUTH (facing +Z)
        quad(consumer, pose,
                x0, y1, z1, U_MAX, v0,
                x0, y0, z1, U_MIN, v0,
                x1, y0, z1, U_MIN, v1,
                x1, y1, z1, U_MAX, v1,
                color, 0.0f, 0.0f, 1.0f);

        // WEST cap (at x0, facing -X)
        if (capStart) {
            quad(consumer, pose,
                    x0, y1, z0, U_MIN, 0.0f,
                    x0, y0, z0, U_MIN, 0.125f,
                    x0, y0, z1, U_MAX, 0.125f,
                    x0, y1, z1, U_MAX, 0.0f,
                    color, -1.0f, 0.0f, 0.0f);
        }

        // EAST cap (at x1, facing +X)
        if (capEnd) {
            quad(consumer, pose,
                    x1, y1, z1, U_MIN, 0.875f,
                    x1, y0, z1, U_MIN, 1.0f,
                    x1, y0, z0, U_MAX, 1.0f,
                    x1, y1, z0, U_MAX, 0.875f,
                    color, 1.0f, 0.0f, 0.0f);
        }
    }

    private void renderZRod(VertexConsumer consumer, PoseStack.Pose pose,
                            float x0, float x1, float y0, float y1, float z0, float z1,
                            int color, boolean capStart, boolean capEnd) {
        float v0 = Math.clamp(z0, 0.0f, 1.0f);
        float v1 = Math.clamp(z1, 0.0f, 1.0f);

        // UP (facing +Y)
        quad(consumer, pose,
                x0, y1, z0, U_MIN, v0,
                x0, y1, z1, U_MIN, v1,
                x1, y1, z1, U_MAX, v1,
                x1, y1, z0, U_MAX, v0,
                color, 0.0f, 1.0f, 0.0f);

        // DOWN (facing -Y)
        quad(consumer, pose,
                x0, y0, z1, U_MIN, v1,
                x0, y0, z0, U_MIN, v0,
                x1, y0, z0, U_MAX, v0,
                x1, y0, z1, U_MAX, v1,
                color, 0.0f, -1.0f, 0.0f);

        // WEST (facing -X)
        quad(consumer, pose,
                x0, y1, z0, U_MAX, v0,
                x0, y0, z0, U_MIN, v0,
                x0, y0, z1, U_MIN, v1,
                x0, y1, z1, U_MAX, v1,
                color, -1.0f, 0.0f, 0.0f);

        // EAST (facing +X)
        quad(consumer, pose,
                x1, y1, z1, U_MAX, v1,
                x1, y0, z1, U_MIN, v1,
                x1, y0, z0, U_MIN, v0,
                x1, y1, z0, U_MAX, v0,
                color, 1.0f, 0.0f, 0.0f);

        // NORTH cap (at z0, facing -Z)
        if (capStart) {
            quad(consumer, pose,
                    x1, y1, z0, U_MIN, 0.0f,
                    x1, y0, z0, U_MIN, 0.125f,
                    x0, y0, z0, U_MAX, 0.125f,
                    x0, y1, z0, U_MAX, 0.0f,
                    color, 0.0f, 0.0f, -1.0f);
        }

        // SOUTH cap (at z1, facing +Z)
        if (capEnd) {
            quad(consumer, pose,
                    x0, y1, z1, U_MIN, 0.875f,
                    x0, y0, z1, U_MIN, 1.0f,
                    x1, y0, z1, U_MAX, 1.0f,
                    x1, y1, z1, U_MAX, 0.875f,
                    color, 0.0f, 0.0f, 1.0f);
        }
    }

    private void quad(VertexConsumer consumer, PoseStack.Pose pose,
                      float x0, float y0, float z0, float u0, float v0,
                      float x1, float y1, float z1, float u1, float v1,
                      float x2, float y2, float z2, float u2, float v2,
                      float x3, float y3, float z3, float u3, float v3,
                      int color, float nx, float ny, float nz) {
        vertex(consumer, pose, x0, y0, z0, u0, v0, color, nx, ny, nz);
        vertex(consumer, pose, x1, y1, z1, u1, v1, color, nx, ny, nz);
        vertex(consumer, pose, x2, y2, z2, u2, v2, color, nx, ny, nz);
        vertex(consumer, pose, x3, y3, z3, u3, v3, color, nx, ny, nz);
    }

    private void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                        float x, float y, float z, float u, float v,
                        int color, float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }
}
