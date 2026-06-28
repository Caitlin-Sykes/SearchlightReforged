package com.csykes.searchlight.features.lighting_director;

import com.csykes.searchlight.utils.lighting.AddressableLight;
import com.csykes.searchlight.network.SetLightAddressPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public class LightAddressScreen extends Screen {
    private final BlockPos lightPos;
    private EditBox editBox;

    public LightAddressScreen(BlockPos pos) {
        super(Component.literal("Configure Light Address"));
        this.lightPos = pos;
    }

    @Override
    protected void init() {
        super.init();

        String currentAddress = "";
        if (this.minecraft != null && this.minecraft.level != null) {
            BlockEntity be = this.minecraft.level.getBlockEntity(lightPos);
            if (be instanceof AddressableLight addressable) {
                currentAddress = addressable.getAddress();
            }
        }

        editBox = new EditBox(this.font, this.width / 2 - 80, this.height / 2 - 10, 160, 20, Component.literal("Address"));
        editBox.setValue(currentAddress);
        this.addRenderableWidget(editBox);

        this.addRenderableWidget(Button.builder(Component.literal("Save"), (btn) -> {
            String newAddress = editBox.getValue();
            PacketDistributor.sendToServer(new SetLightAddressPayload(lightPos, newAddress));
            this.onClose();
        }).bounds(this.width / 2 - 82, this.height / 2 + 20, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), (btn) -> {
            this.onClose();
        }).bounds(this.width / 2 + 2, this.height / 2 + 20, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
