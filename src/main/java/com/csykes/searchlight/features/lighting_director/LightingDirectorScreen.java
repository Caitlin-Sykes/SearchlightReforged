package com.csykes.searchlight.features.lighting_director;

import com.csykes.searchlight.features.lighting_director.LightingDirectorBlockEntity.LinkedLightEntry;
import com.csykes.searchlight.network.UnlinkDirectorLightPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class LightingDirectorScreen extends Screen {
    private final BlockPos directorPos;
    private final List<LinkedLightEntry> entries = new ArrayList<>();

    private double scrollAmount = 0;
    private boolean isDraggingScrollbar = false;

    private static final int ENTRY_HEIGHT = 42;
    private static final int ENTRY_GAP = 4;
    private static final int ENTRY_TOTAL = ENTRY_HEIGHT + ENTRY_GAP;

    public LightingDirectorScreen(BlockPos directorPos) {
        super(Component.translatable("gui.searchlight.lighting_director.title"));
        this.directorPos = directorPos;
    }

    @Override
    protected void init() {
        super.init();
        refreshEntries();

        int centerX = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.searchlight.button.done"), btn -> this.onClose())
                .bounds(centerX - 50, this.height - 30, 100, 20)
                .build());
    }

    private void refreshEntries() {
        entries.clear();
        if (this.minecraft != null && this.minecraft.level != null) {
            BlockEntity be = this.minecraft.level.getBlockEntity(directorPos);
            if (be instanceof LightingDirectorBlockEntity director) {
                entries.addAll(director.getLinkedLightEntries());
            }
        }
        clampScroll();
    }

    private int getListLeft() {
        return Math.max(10, (this.width - 340) / 2);
    }

    private int getListTop() {
        return 42;
    }

    private int getListBottom() {
        return this.height - 38;
    }

    private int getListHeight() {
        return Math.max(1, getListBottom() - getListTop());
    }

    private int getContentHeight() {
        return entries.size() * ENTRY_TOTAL;
    }

    private double getMaxScroll() {
        return Math.max(0, getContentHeight() - getListHeight());
    }

    private void clampScroll() {
        scrollAmount = Math.max(0, Math.min(getMaxScroll(), scrollAmount));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;

        graphics.drawCenteredString(this.font, this.title, centerX, 12, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("gui.searchlight.lighting_director.linked_lights", entries.size()), centerX, 25, 0xFFAAAAAA);

        int listLeft = getListLeft();
        int listTop = getListTop();
        int listBottom = getListBottom();
        int listWidth = 340;
        int listHeight = getListHeight();

        // Background panel behind the list
        graphics.fill(listLeft - 4, listTop - 2, listLeft + listWidth + 4, listBottom + 2, 0x900B0B10);

        if (entries.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.searchlight.lighting_director.no_lights"), centerX, listTop + listHeight / 2 - 10, 0xFF888888);
            graphics.drawCenteredString(this.font, Component.translatable("gui.searchlight.lighting_director.use_linker"), centerX, listTop + listHeight / 2 + 6, 0xFF666666);
            return;
        }

        graphics.enableScissor(listLeft - 4, listTop, listLeft + listWidth + 4, listBottom);

        for (int i = 0; i < entries.size(); i++) {
            LinkedLightEntry entry = entries.get(i);
            int entryY = listTop + (i * ENTRY_TOTAL) - (int) scrollAmount;

            if (entryY + ENTRY_HEIGHT < listTop || entryY > listBottom) {
                continue;
            }

            int cardW = listWidth - 14;
            boolean isHovered = mouseX >= listLeft && mouseX <= listLeft + cardW && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT;

            // Card background & border
            int bgCol = isHovered ? 0xD0242432 : 0xCC181822;
            int borderCol = isHovered ? 0xFF686888 : 0xFF353545;
            graphics.fill(listLeft, entryY, listLeft + cardW, entryY + ENTRY_HEIGHT, bgCol);
            graphics.fill(listLeft, entryY, listLeft + cardW, entryY + 1, borderCol);
            graphics.fill(listLeft, entryY + ENTRY_HEIGHT - 1, listLeft + cardW, entryY + ENTRY_HEIGHT, borderCol);
            graphics.fill(listLeft, entryY, listLeft + 1, entryY + ENTRY_HEIGHT, borderCol);
            graphics.fill(listLeft + cardW - 1, entryY, listLeft + cardW, entryY + ENTRY_HEIGHT, borderCol);

            // Row 1: Slot, Light Name, Coordinates
            graphics.drawString(this.font, Component.translatable("gui.searchlight.slot", entry.slot()), listLeft + 6, entryY + 6, 0xFFFFD700);
            graphics.drawString(this.font, entry.lightType(), listLeft + 52, entryY + 6, 0xFFFFFFFF);
            graphics.drawString(this.font, "[" + entry.pos().getX() + ", " + entry.pos().getY() + ", " + entry.pos().getZ() + "]", listLeft + 165, entryY + 6, 0xFF888888);

            // Row 2: Address, Mode, Connected Blocks Count
            String addrStr = entry.address().isEmpty() ? Component.translatable("gui.searchlight.none").getString() : entry.address();
            String modeStr = Component.translatable("gui.searchlight.mode." + entry.mode().toLowerCase()).getString();
            graphics.drawString(this.font, Component.translatable("gui.searchlight.id", addrStr), listLeft + 6, entryY + 22, 0xFF55FF55);
            graphics.drawString(this.font, Component.translatable("gui.searchlight.mode", modeStr), listLeft + 68, entryY + 22, 0xFFFFFF55);
            graphics.drawString(this.font, Component.translatable("gui.searchlight.blocks", entry.connectedCount()), listLeft + 165, entryY + 22, 0xFF55FFFF);

            // Unlink button
            int unlinkW = 46;
            int unlinkH = 18;
            int unlinkX = listLeft + cardW - unlinkW - 6;
            int unlinkY = entryY + (ENTRY_HEIGHT - unlinkH) / 2;

            boolean unlinkHover = mouseX >= unlinkX && mouseX <= unlinkX + unlinkW && mouseY >= unlinkY && mouseY <= unlinkY + unlinkH;
            int unlinkBg = unlinkHover ? 0xFF992222 : 0xFF661818;
            int unlinkBorder = unlinkHover ? 0xFFFF4444 : 0xFFAA2E2E;

            graphics.fill(unlinkX, unlinkY, unlinkX + unlinkW, unlinkY + unlinkH, unlinkBg);
            graphics.fill(unlinkX, unlinkY, unlinkX + unlinkW, unlinkY + 1, unlinkBorder);
            graphics.fill(unlinkX, unlinkY + unlinkH - 1, unlinkX + unlinkW, unlinkY + unlinkH, unlinkBorder);
            graphics.fill(unlinkX, unlinkY, unlinkX + 1, unlinkY + unlinkH, unlinkBorder);
            graphics.fill(unlinkX + unlinkW - 1, unlinkY, unlinkX + unlinkW, unlinkY + unlinkH, unlinkBorder);

            graphics.drawCenteredString(this.font, Component.translatable("gui.searchlight.button.unlink"), unlinkX + unlinkW / 2, unlinkY + 5, unlinkHover ? 0xFFFFFFFF : 0xFFD8D8D8);
        }

        graphics.disableScissor();

        // Scrollbar
        double maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            int sbX = listLeft + listWidth - 6;
            int sbW = 6;
            graphics.fill(sbX, listTop, sbX + sbW, listBottom, 0x60000000);

            int thumbH = Math.max(16, (int) ((float) listHeight / getContentHeight() * listHeight));
            int thumbY = listTop + (int) ((scrollAmount / maxScroll) * (listHeight - thumbH));
            int thumbCol = isDraggingScrollbar ? 0xFFBBBBBB : 0xFF777777;
            graphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, thumbCol);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (getMaxScroll() > 0) {
            scrollAmount -= scrollY * 18;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int listLeft = getListLeft();
            int listTop = getListTop();
            int listBottom = getListBottom();
            int listWidth = 340;

            // Check click on Unlink buttons
            if (mouseY >= listTop && mouseY <= listBottom) {
                for (int i = 0; i < entries.size(); i++) {
                    LinkedLightEntry entry = entries.get(i);
                    int entryY = listTop + (i * ENTRY_TOTAL) - (int) scrollAmount;
                    if (entryY + ENTRY_HEIGHT < listTop || entryY > listBottom) continue;

                    int cardW = listWidth - 14;
                    int unlinkW = 46;
                    int unlinkH = 18;
                    int unlinkX = listLeft + cardW - unlinkW - 6;
                    int unlinkY = entryY + (ENTRY_HEIGHT - unlinkH) / 2;

                    if (mouseX >= unlinkX && mouseX <= unlinkX + unlinkW && mouseY >= unlinkY && mouseY <= unlinkY + unlinkH) {
                        PacketDistributor.sendToServer(new UnlinkDirectorLightPayload(directorPos, entry.slot() - 1));
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            // Also update local BE immediately for instant UI response
                            if (this.minecraft.level != null) {
                                BlockEntity be = this.minecraft.level.getBlockEntity(directorPos);
                                if (be instanceof LightingDirectorBlockEntity director) {
                                    director.removeLinkedLight(entry.slot() - 1);
                                }
                            }
                        }
                        refreshEntries();
                        return true;
                    }
                }
            }

            // Check click on scrollbar
            double maxScroll = getMaxScroll();
            if (maxScroll > 0) {
                int sbX = listLeft + listWidth - 6;
                int sbW = 6;
                if (mouseX >= sbX && mouseX <= sbX + sbW && mouseY >= listTop && mouseY <= listBottom) {
                    isDraggingScrollbar = true;
                    updateScrollFromMouse(mouseY);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateScrollFromMouse(double mouseY) {
        int listTop = getListTop();
        int listHeight = getListHeight();
        double maxScroll = getMaxScroll();
        double progress = (mouseY - listTop) / (double) listHeight;
        scrollAmount = progress * maxScroll;
        clampScroll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
