package com.csykes.searchlight.utils;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Utility for rendering Minecraft block items to PNG images using Minecraft client-side rendering pipeline.
 */
public class BlockItemRendererUtil {

    public static final int DEFAULT_SIZE = 64;

    /**
     * Renders an item specified by its ResourceLocation to a PNG file.
     *
     * @param itemId     ResourceLocation of the item (e.g. "searchlight:lighting_linker_card")
     * @param outputFile Destination PNG file
     * @param width      Target image width in pixels
     * @param height     Target image height in pixels
     * @throws IOException If image writing fails
     */
    public static void renderItemToPng(ResourceLocation itemId, File outputFile, int width, int height) throws IOException {
        Objects.requireNonNull(itemId, "itemId cannot be null");
        Objects.requireNonNull(outputFile, "outputFile cannot be null");

        Item item = BuiltInRegistries.ITEM.getOptional(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found in registry: " + itemId));

        ItemStack stack = new ItemStack(item);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Invalid empty ItemStack for item: " + itemId);
        }

        BufferedImage image = renderItemStack(stack, width, height);

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        ImageIO.write(image, "PNG", outputFile);
    }

    /**
     * Renders a block item specified by its ResourceLocation to a PNG file.
     *
     * @param blockId    ResourceLocation of the block (e.g. "searchlight:colour_lamp_white")
     * @param outputFile Destination PNG file
     * @param width      Target image width in pixels
     * @param height     Target image height in pixels
     * @throws IOException If image writing fails
     */
    public static void renderBlockItemToPng(ResourceLocation blockId, File outputFile, int width, int height) throws IOException {
        Objects.requireNonNull(blockId, "blockId cannot be null");
        Objects.requireNonNull(outputFile, "outputFile cannot be null");

        Block block = BuiltInRegistries.BLOCK.getOptional(blockId)
                .orElseThrow(() -> new IllegalArgumentException("Block not found in registry: " + blockId));

        Item item = block.asItem();
        ItemStack stack = new ItemStack(item);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Block does not have a valid Item representation: " + blockId);
        }

        BufferedImage image = renderItemStack(stack, width, height);

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        ImageIO.write(image, "PNG", outputFile);
    }

    /**
     * Renders a block item with default 64x64 resolution.
     */
    public static void renderBlockItemToPng(ResourceLocation blockId, File outputFile) throws IOException {
        renderBlockItemToPng(blockId, outputFile, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    /**
     * Renders an ItemStack off-screen and returns a BufferedImage.
     * Must be called on the Render / Client thread.
     */
    public static BufferedImage renderItemStack(ItemStack stack, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget renderTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);

        try {
            renderTarget.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            renderTarget.clear(Minecraft.ON_OSX);
            renderTarget.bindWrite(true);

            RenderSystem.viewport(0, 0, width, height);

            // Set up orthographic projection for the off-screen buffer dimensions (0..width, 0..height)
            RenderSystem.backupProjectionMatrix();
            Matrix4f ortho = new Matrix4f().setOrtho(0.0f, (float) width, (float) height, 0.0f, 1000.0f, 3000.0f);
            RenderSystem.setProjectionMatrix(ortho, com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);

            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            modelViewStack.identity();
            modelViewStack.translate(0.0f, 0.0f, -2000.0f);
            RenderSystem.applyModelViewMatrix();

            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            net.minecraft.client.gui.GuiGraphics graphics = new net.minecraft.client.gui.GuiGraphics(mc, bufferSource);

            // Scale 16x16 standard GUI item size up to fill width x height
            float scale = (float) width / 16.0f;
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();

            graphics.flush();
            bufferSource.endBatch();

            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();

            // Read pixels directly from the framebuffer texture using NativeImage
            com.mojang.blaze3d.platform.NativeImage nativeImage = new com.mojang.blaze3d.platform.NativeImage(width, height, false);
            RenderSystem.bindTexture(renderTarget.getColorTextureId());
            nativeImage.downloadTexture(0, false);
            nativeImage.flipY();

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int abgr = nativeImage.getPixelRGBA(x, y);
                    int a = (abgr >> 24) & 0xFF;
                    int b = (abgr >> 16) & 0xFF;
                    int g = (abgr >> 8) & 0xFF;
                    int r = abgr & 0xFF;
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    image.setRGB(x, y, argb);
                }
            }
            nativeImage.close();

            return image;
        } finally {
            renderTarget.unbindWrite();
            renderTarget.destroyBuffers();
            if (mc.getMainRenderTarget() != null) {
                mc.getMainRenderTarget().bindWrite(true);
            }
        }
    }
}
