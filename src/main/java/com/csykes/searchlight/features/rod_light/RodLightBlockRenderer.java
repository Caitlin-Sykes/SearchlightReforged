package com.csykes.searchlight.features.rod_light;

import com.csykes.searchlight.features.centre_light.CentreLightBlock;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.ColorAveragingHelper;
import com.csykes.searchlight.utils.lighting.CornerLightStage;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Objects;

/**
 * Dynamically renders sub-pixel segments for CornerLight and CentreLight single rods
 * when individual sub-pixel colors vary.
 */
public class RodLightBlockRenderer implements BlockEntityRenderer<WallLightBlockEntity> {

    private static final ResourceLocation ROD_ON_TEXTURE = ResourceLocation.fromNamespaceAndPath("searchlight", "textures/block/corner_light_rod_on.png");
    private static final int FULL_BRIGHT = 15728880;
    private static final float EPS = 0.002f;

    public RodLightBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WallLightBlockEntity tile, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = tile.getBlockState();
        boolean isCorner = state.getBlock() instanceof CornerLightBlock;
        boolean isCentre = state.getBlock() instanceof CentreLightBlock;

        if (!isCorner && !isCentre) return;
        // If the rod is uniform, the baked chunk mesh with block tinting renders it accurately
        // (including all mounting hardware and base caps). Dynamic BER is only needed when colors vary along the rod.
        if (tile.getRodLightData().isUniform()) return;

        RodLightData data = tile.getRodLightData();
        if (!data.hasAnyLitSubPixel()) return;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ROD_ON_TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        if (isCorner) {
            renderCornerRod(consumer, pose, state, data);
        } else {
            renderCentreRod(consumer, pose, state, data);
        }
    }

    private void renderCornerRod(VertexConsumer consumer, PoseStack.Pose pose, BlockState state, RodLightData data) {
        CornerLightStage corner = state.hasProperty(CornerLightBlock.CORNER)
                ? state.getValue(CornerLightBlock.CORNER)
                : CornerLightStage.BOTTOM_LEFT;

        float x0, x1, z0, z1;
        switch (corner) {
            case BOTTOM_LEFT -> {
                x0 = -EPS;
                x1 = 2.0f / 16.0f + EPS;
                z0 = 14.0f / 16.0f - EPS;
                z1 = 1.0f + EPS;
            }
            case BOTTOM_RIGHT -> {
                x0 = -EPS;
                x1 = 2.0f / 16.0f + EPS;
                z0 = -EPS;
                z1 = 2.0f / 16.0f + EPS;
            }
            case TOP_RIGHT -> {
                x0 = 14.0f / 16.0f - EPS;
                x1 = 1.0f + EPS;
                z0 = -EPS;
                z1 = 2.0f / 16.0f + EPS;
            }
            case TOP_LEFT -> {
                x0 = 14.0f / 16.0f - EPS;
                x1 = 1.0f + EPS;
                z0 = 14.0f / 16.0f - EPS;
                z1 = 1.0f + EPS;
            }
            default -> {
                return;
            }
        }

        renderYAxisSegments(consumer, pose, x0, x1, z0, z1, data);
    }

    private void renderCentreRod(VertexConsumer consumer, PoseStack.Pose pose, BlockState state, RodLightData data) {
        Direction.Axis axis = state.hasProperty(BlockStateProperties.AXIS)
                ? state.getValue(BlockStateProperties.AXIS)
                : Direction.Axis.Y;

        switch (axis) {
            case Y -> {
                float x0 = 7.0f / 16.0f - EPS;
                float x1 = 9.0f / 16.0f + EPS;
                float z0 = 7.0f / 16.0f - EPS;
                float z1 = 9.0f / 16.0f + EPS;
                renderYAxisSegments(consumer, pose, x0, x1, z0, z1, data);
            }
            case X -> {
                float y0 = 7.0f / 16.0f - EPS;
                float y1 = 9.0f / 16.0f + EPS;
                float z0 = 7.0f / 16.0f - EPS;
                float z1 = 9.0f / 16.0f + EPS;
                renderXAxisSegments(consumer, pose, y0, y1, z0, z1, data);
            }
            case Z -> {
                float x0 = 7.0f / 16.0f - EPS;
                float x1 = 9.0f / 16.0f + EPS;
                float y0 = 7.0f / 16.0f - EPS;
                float y1 = 9.0f / 16.0f + EPS;
                renderZAxisSegments(consumer, pose, x0, x1, y0, y1, data);
            }
        }
    }

    private void renderYAxisSegments(VertexConsumer consumer, PoseStack.Pose pose,
                                     float x0, float x1, float z0, float z1,
                                     RodLightData data) {
        for (int i = 0; i < RodLightData.SUB_PIXELS; i++) {
            if (!data.isSubPixelLit(i)) continue;

            int color = ColorAveragingHelper.getRgbForColor(data.getSubPixelColor(i));
            float y0 = (i == 0) ? -EPS : (i * 0.125f);
            float y1 = (i == 7) ? (1.0f + EPS) : ((i + 1) * 0.125f);

            boolean capStart = (i == 0) || (!data.isSubPixelLit(i - 1) || !Objects.equals(data.getSubPixelColor(i), data.getSubPixelColor(i - 1)));
            boolean capEnd = (i == 7) || (!data.isSubPixelLit(i + 1) || !Objects.equals(data.getSubPixelColor(i), data.getSubPixelColor(i + 1)));

            renderBox(consumer, pose, x0, x1, y0, y1, z0, z1, color, capStart, capEnd, Direction.Axis.Y);
        }
    }

    private void renderXAxisSegments(VertexConsumer consumer, PoseStack.Pose pose,
                                     float y0, float y1, float z0, float z1,
                                     RodLightData data) {
        for (int i = 0; i < RodLightData.SUB_PIXELS; i++) {
            if (!data.isSubPixelLit(i)) continue;

            int color = ColorAveragingHelper.getRgbForColor(data.getSubPixelColor(i));
            float x0 = (i == 0) ? -EPS : (i * 0.125f);
            float x1 = (i == 7) ? (1.0f + EPS) : ((i + 1) * 0.125f);

            boolean capStart = (i == 0) || (!data.isSubPixelLit(i - 1) || !Objects.equals(data.getSubPixelColor(i), data.getSubPixelColor(i - 1)));
            boolean capEnd = (i == 7) || (!data.isSubPixelLit(i + 1) || !Objects.equals(data.getSubPixelColor(i), data.getSubPixelColor(i + 1)));

            renderBox(consumer, pose, x0, x1, y0, y1, z0, z1, color, capStart, capEnd, Direction.Axis.X);
        }
    }

    private void renderZAxisSegments(VertexConsumer consumer, PoseStack.Pose pose,
                                     float x0, float x1, float y0, float y1,
                                     RodLightData data) {
        for (int i = 0; i < RodLightData.SUB_PIXELS; i++) {
            if (!data.isSubPixelLit(i)) continue;

            int color = ColorAveragingHelper.getRgbForColor(data.getSubPixelColor(i));
            float z0 = (i == 0) ? -EPS : (i * 0.125f);
            float z1 = (i == 7) ? (1.0f + EPS) : ((i + 1) * 0.125f);

            boolean capStart = (i == 0) || (!data.isSubPixelLit(i - 1) || !Objects.equals(data.getSubPixelColor(i), data.getSubPixelColor(i - 1)));
            boolean capEnd = (i == 7) || (!data.isSubPixelLit(i + 1) || !Objects.equals(data.getSubPixelColor(i), data.getSubPixelColor(i + 1)));

            renderBox(consumer, pose, x0, x1, y0, y1, z0, z1, color, capStart, capEnd, Direction.Axis.Z);
        }
    }

    private void renderBox(VertexConsumer consumer, PoseStack.Pose pose,
                           float x0, float x1, float y0, float y1, float z0, float z1,
                           int color, boolean capStart, boolean capEnd, Direction.Axis axis) {
        float uMin = 0.0f;
        float uMax = 0.25f;

        // Sides facing along Axis
        if (axis == Direction.Axis.Y) {
            float v0 = Math.max(0.0f, Math.min(1.0f, y0));
            float v1 = Math.max(0.0f, Math.min(1.0f, y1));

            // NORTH (-Z)
            quad(consumer, pose, x1, y1, z0, uMax, v1, x1, y0, z0, uMin, v1, x0, y0, z0, uMin, v0, x0, y1, z0, uMax, v0, color, 0.0f, 0.0f, -1.0f);
            // SOUTH (+Z)
            quad(consumer, pose, x0, y1, z1, uMax, v0, x0, y0, z1, uMin, v0, x1, y0, z1, uMin, v1, x1, y1, z1, uMax, v1, color, 0.0f, 0.0f, 1.0f);
            // WEST (-X)
            quad(consumer, pose, x0, y1, z0, uMax, v0, x0, y0, z0, uMin, v0, x0, y0, z1, uMin, v1, x0, y1, z1, uMax, v1, color, -1.0f, 0.0f, 0.0f);
            // EAST (+X)
            quad(consumer, pose, x1, y1, z1, uMax, v1, x1, y0, z1, uMin, v1, x1, y0, z0, uMin, v0, x1, y1, z0, uMax, v0, color, 1.0f, 0.0f, 0.0f);

            // DOWN (-Y) cap
            if (capStart) {
                quad(consumer, pose, x0, y0, z1, uMin, 0.0f, x0, y0, z0, uMax, 0.0f, x1, y0, z0, uMax, 0.25f, x1, y0, z1, uMin, 0.25f, color, 0.0f, -1.0f, 0.0f);
            }
            // UP (+Y) cap
            if (capEnd) {
                quad(consumer, pose, x0, y1, z0, uMax, 0.0f, x0, y1, z1, uMin, 0.0f, x1, y1, z1, uMin, 0.25f, x1, y1, z0, uMax, 0.25f, color, 0.0f, 1.0f, 0.0f);
            }
        } else if (axis == Direction.Axis.X) {
            float v0 = Math.max(0.0f, Math.min(1.0f, x0));
            float v1 = Math.max(0.0f, Math.min(1.0f, x1));

            // UP (+Y)
            quad(consumer, pose, x0, y1, z0, uMax, v0, x0, y1, z1, uMin, v0, x1, y1, z1, uMin, v1, x1, y1, z0, uMax, v1, color, 0.0f, 1.0f, 0.0f);
            // DOWN (-Y)
            quad(consumer, pose, x0, y0, z1, uMin, v0, x0, y0, z0, uMax, v0, x1, y0, z0, uMax, v1, x1, y0, z1, uMin, v1, color, 0.0f, -1.0f, 0.0f);
            // NORTH (-Z)
            quad(consumer, pose, x1, y1, z0, uMax, v1, x1, y0, z0, uMin, v1, x0, y0, z0, uMin, v0, x0, y1, z0, uMax, v0, color, 0.0f, 0.0f, -1.0f);
            // SOUTH (+Z)
            quad(consumer, pose, x0, y1, z1, uMax, v0, x0, y0, z1, uMin, v0, x1, y0, z1, uMin, v1, x1, y1, z1, uMax, v1, color, 0.0f, 0.0f, 1.0f);

            // WEST (-X) cap
            if (capStart) {
                quad(consumer, pose, x0, y1, z0, uMin, 0.0f, x0, y0, z0, uMin, 0.25f, x0, y0, z1, uMax, 0.25f, x0, y1, z1, uMax, 0.0f, color, -1.0f, 0.0f, 0.0f);
            }
            // EAST (+X) cap
            if (capEnd) {
                quad(consumer, pose, x1, y1, z1, uMin, 0.75f, x1, y0, z1, uMin, 1.0f, x1, y0, z0, uMax, 1.0f, x1, y1, z0, uMax, 0.75f, color, 1.0f, 0.0f, 0.0f);
            }
        } else { // Z axis
            float v0 = Math.max(0.0f, Math.min(1.0f, z0));
            float v1 = Math.max(0.0f, Math.min(1.0f, z1));

            // UP (+Y)
            quad(consumer, pose, x0, y1, z0, uMin, v0, x0, y1, z1, uMin, v1, x1, y1, z1, uMax, v1, x1, y1, z0, uMax, v0, color, 0.0f, 1.0f, 0.0f);
            // DOWN (-Y)
            quad(consumer, pose, x0, y0, z1, uMin, v1, x0, y0, z0, uMin, v0, x1, y0, z0, uMax, v0, x1, y0, z1, uMax, v1, color, 0.0f, -1.0f, 0.0f);
            // WEST (-X)
            quad(consumer, pose, x0, y1, z0, uMax, v0, x0, y0, z0, uMin, v0, x0, y0, z1, uMin, v1, x0, y1, z1, uMax, v1, color, -1.0f, 0.0f, 0.0f);
            // EAST (+X)
            quad(consumer, pose, x1, y1, z1, uMax, v1, x1, y0, z1, uMin, v1, x1, y0, z0, uMin, v0, x1, y1, z0, uMax, v0, color, 1.0f, 0.0f, 0.0f);

            // NORTH (-Z) cap
            if (capStart) {
                quad(consumer, pose, x1, y1, z0, uMin, 0.0f, x1, y0, z0, uMin, 0.25f, x0, y0, z0, uMax, 0.25f, x0, y1, z0, uMax, 0.0f, color, 0.0f, 0.0f, -1.0f);
            }
            // SOUTH (+Z) cap
            if (capEnd) {
                quad(consumer, pose, x0, y1, z1, uMin, 0.75f, x0, y0, z1, uMin, 1.0f, x1, y0, z1, uMax, 1.0f, x1, y1, z1, uMax, 0.75f, color, 0.0f, 0.0f, 1.0f);
            }
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
