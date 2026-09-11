package com.csykes.searchlight.features.lighting_director;

import com.csykes.searchlight.network.SetLightAddressPayload;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.AddressableLight;
import com.csykes.searchlight.utils.lighting.LightMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class LightAddressScreen extends Screen {
    private final BlockPos lightPos;
    private int digit0 = 0; // Hundreds
    private int digit1 = 0; // Tens
    private int digit2 = 0; // Ones

    private LightMode selectedMode = LightMode.FIXTURE;
    private boolean isConnectingLight = false;
    private boolean isDropdownOpen = false;
    private final List<LightMode> availableModes = new ArrayList<>();

    private Button modeDropdownButton;

    // 7-segment bitmask table (segments: a, b, c, d, e, f, g)
    private static final int[] DIGIT_SEGMENTS = {
            0b0111111, // 0: a,b,c,d,e,f
            0b0000110, // 1: b,c
            0b1011011, // 2: a,b,d,e,g
            0b1001111, // 3: a,b,c,d,g
            0b1100110, // 4: b,c,f,g
            0b1101101, // 5: a,c,d,f,g
            0b1111101, // 6: a,c,d,e,f,g
            0b0000111, // 7: a,b,c
            0b1111111, // 8: a,b,c,d,e,f,g
            0b1101111  // 9: a,b,c,d,f,g
    };

    public LightAddressScreen(BlockPos pos) {
        super(Component.translatable("gui.searchlight.light_address.title"));
        this.lightPos = pos;
    }

    @Override
    protected void init() {
        super.init();

        availableModes.clear();
        // Read current address and mode from block entity if available
        if (this.minecraft != null && this.minecraft.level != null) {
            BlockEntity be = this.minecraft.level.getBlockEntity(lightPos);
            BlockState state = this.minecraft.level.getBlockState(lightPos);

            if (state.getBlock() instanceof AbstractLightBlock alb && alb.isConnectingLight(state)) {
                this.isConnectingLight = true;
                availableModes.add(LightMode.FIXTURE);
                availableModes.add(LightMode.SEPARATE);
                if (alb.supportsPixelMode(state)) {
                    availableModes.add(LightMode.PIXEL);
                }
            } else {
                this.isConnectingLight = false;
                availableModes.add(LightMode.FIXTURE);
            }

            if (be instanceof AddressableLight addressable) {
                String currentAddress = addressable.getAddress();
                parseAddressDigits(currentAddress);
                this.selectedMode = addressable.getLightMode();
                if (!availableModes.contains(this.selectedMode)) {
                    this.selectedMode = availableModes.get(0);
                }
            }
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int panelW = 122;
        int panelX = centerX - panelW / 2;
        int panelY = centerY - 48;

        int digitW = 22;
        int spacing = 14;
        int d0X = panelX + 16;
        int d1X = d0X + digitW + spacing;
        int d2X = d1X + digitW + spacing;

        int btnW = 24;
        int btnH = 20;

        // Top increment buttons
        this.addRenderableWidget(Button.builder(Component.literal("▲"), btn -> digit0 = (digit0 + 1) % 10)
                .bounds(d0X - 1, panelY - 24, btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("▲"), btn -> digit1 = (digit1 + 1) % 10)
                .bounds(d1X - 1, panelY - 24, btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("▲"), btn -> digit2 = (digit2 + 1) % 10)
                .bounds(d2X - 1, panelY - 24, btnW, btnH).build());

        // Bottom decrement buttons
        this.addRenderableWidget(Button.builder(Component.literal("▼"), btn -> digit0 = (digit0 + 9) % 10)
                .bounds(d0X - 1, panelY + 58, btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("▼"), btn -> digit1 = (digit1 + 9) % 10)
                .bounds(d1X - 1, panelY + 58, btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("▼"), btn -> digit2 = (digit2 + 9) % 10)
                .bounds(d2X - 1, panelY + 58, btnW, btnH).build());

        // Mode dropdown button
        int dropdownW = 140;
        int dropdownX = centerX - dropdownW / 2;
        int dropdownY = panelY + 84;

        modeDropdownButton = Button.builder(getModeButtonText(), btn -> {
            if (isConnectingLight) {
                isDropdownOpen = !isDropdownOpen;
            }
        }).bounds(dropdownX, dropdownY, dropdownW, 20).build();

        if (!isConnectingLight) {
            modeDropdownButton.active = false;
            modeDropdownButton.setTooltip(Tooltip.create(Component.translatable("gui.searchlight.mode.tooltip.disabled")));
        } else {
            modeDropdownButton.setTooltip(Tooltip.create(Component.translatable("gui.searchlight.mode.tooltip.connecting")));
        }
        this.addRenderableWidget(modeDropdownButton);

        // Save & Cancel buttons positioned cleanly below dropdown menu options
        int actionBtnY = dropdownY + 88;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.searchlight.button.save"), btn -> {
            String newAddress = String.format("%d%d%d", digit0, digit1, digit2);
            PacketDistributor.sendToServer(new SetLightAddressPayload(lightPos, newAddress, selectedMode.name()));
            this.onClose();
        }).bounds(centerX - 82, actionBtnY, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.searchlight.button.cancel"), btn -> {
            this.onClose();
        }).bounds(centerX + 2, actionBtnY, 80, 20).build());
    }

    private void parseAddressDigits(String address) {
        if (address == null || address.trim().isEmpty()) {
            digit0 = 0;
            digit1 = 0;
            digit2 = 1;
            return;
        }

        try {
            int val = Integer.parseInt(address.trim());
            val = Math.floorMod(val, 1000);
            digit0 = (val / 100) % 10;
            digit1 = (val / 10) % 10;
            digit2 = val % 10;
            return;
        } catch (NumberFormatException ignored) {}

        // Fallback: extract digits from text like "light_12"
        String digitsOnly = address.replaceAll("\\D+", "");
        if (!digitsOnly.isEmpty()) {
            try {
                int val = Integer.parseInt(digitsOnly);
                val = Math.floorMod(val, 1000);
                digit0 = (val / 100) % 10;
                digit1 = (val / 10) % 10;
                digit2 = val % 10;
                return;
            } catch (NumberFormatException ignored) {}
        }

        digit0 = 0;
        digit1 = 0;
        digit2 = 1;
    }

    private Component getModeButtonText() {
        Component modeName = Component.translatable("gui.searchlight.mode." + selectedMode.name().toLowerCase());
        return Component.translatable("gui.searchlight.mode.dropdown", isConnectingLight ? Component.literal(modeName.getString() + " ▾") : modeName);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        graphics.drawCenteredString(this.font, this.title, centerX, centerY - 82, 0xFFFFFFFF);

        // Render 7-segment display bezel and panel
        int panelW = 122;
        int panelH = 54;
        int panelX = centerX - panelW / 2;
        int panelY = centerY - 48;

        // Outer border
        graphics.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0xFF444444);
        // Inner bezel
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, 0xFF1E1E1E);
        // Display panel face
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF0A0A0C);

        int digitW = 22;
        int spacing = 14;
        int d0X = panelX + 16;
        int d1X = d0X + digitW + spacing;
        int d2X = d1X + digitW + spacing;
        int digitY = panelY + 6;

        draw7SegmentDigit(graphics, d0X, digitY, digit0);
        draw7SegmentDigit(graphics, d1X, digitY, digit1);
        draw7SegmentDigit(graphics, d2X, digitY, digit2);

        // Render Dropdown options popup overlay if open
        if (isDropdownOpen && modeDropdownButton != null && !availableModes.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0f, 0.0f, 400.0f);

            int optW = modeDropdownButton.getWidth();
            int optX = modeDropdownButton.getX();
            int optH = 20;
            int optStartY = modeDropdownButton.getY() + 21;
            int totalH = availableModes.size() * optH;

            // Background & border for dropdown menu
            graphics.fill(optX - 1, optStartY - 1, optX + optW + 1, optStartY + totalH + 1, 0xFF555555);
            graphics.fill(optX, optStartY, optX + optW, optStartY + totalH, 0xF0181818);

            for (int i = 0; i < availableModes.size(); i++) {
                LightMode mode = availableModes.get(i);
                int optY = optStartY + (i * optH);
                boolean hover = mouseX >= optX && mouseX <= optX + optW && mouseY >= optY && mouseY < optY + optH;
                if (hover) {
                    graphics.fill(optX, optY, optX + optW, optY + optH, 0x80444466);
                }
                int col = (selectedMode == mode) ? 0xFFFFFF55 : 0xFFDDDDDD;
                Component label = Component.translatable("gui.searchlight.mode." + mode.name().toLowerCase());
                graphics.drawString(this.font, label, optX + 8, optY + 6, col);
            }

            graphics.pose().popPose();
        }
    }

    private void draw7SegmentDigit(GuiGraphics graphics, int x, int y, int digit) {
        int mask = (digit >= 0 && digit <= 9) ? DIGIT_SEGMENTS[digit] : 0;
        int onColor = 0xFFFF2525;    // Glowing LED red
        int offColor = 0x20350808;   // Ghosted inactive segment

        int w = 22; // digit width
        int h = 42; // digit height
        int t = 3;  // segment thickness
        int halfH = h / 2;

        // a (top)
        graphics.fill(x + t, y, x + w - t, y + t, (mask & 1) != 0 ? onColor : offColor);
        // b (top right)
        graphics.fill(x + w - t, y + t, x + w, y + halfH, (mask & 2) != 0 ? onColor : offColor);
        // c (bottom right)
        graphics.fill(x + w - t, y + halfH + t, x + w, y + h - t, (mask & 4) != 0 ? onColor : offColor);
        // d (bottom)
        graphics.fill(x + t, y + h - t, x + w - t, y + h, (mask & 8) != 0 ? onColor : offColor);
        // e (bottom left)
        graphics.fill(x, y + halfH + t, x + t, y + h - t, (mask & 16) != 0 ? onColor : offColor);
        // f (top left)
        graphics.fill(x, y + t, x + t, y + halfH, (mask & 32) != 0 ? onColor : offColor);
        // g (middle)
        graphics.fill(x + t, y + halfH - t / 2, x + w - t, y + halfH + t / 2 + 1, (mask & 64) != 0 ? onColor : offColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isDropdownOpen && modeDropdownButton != null && !availableModes.isEmpty()) {
            int optW = modeDropdownButton.getWidth();
            int optX = modeDropdownButton.getX();
            int optH = 20;
            int optStartY = modeDropdownButton.getY() + 21;
            int totalH = availableModes.size() * optH;

            if (mouseX >= optX && mouseX <= optX + optW && mouseY >= optStartY && mouseY < optStartY + totalH) {
                int index = (int) ((mouseY - optStartY) / optH);
                if (index >= 0 && index < availableModes.size()) {
                    selectedMode = availableModes.get(index);
                    isDropdownOpen = false;
                    modeDropdownButton.setMessage(getModeButtonText());
                    return true;
                }
            }
            // Click outside closes dropdown
            isDropdownOpen = false;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isDropdownOpen && (keyCode == GLFW.GLFW_KEY_ESCAPE)) {
            isDropdownOpen = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
