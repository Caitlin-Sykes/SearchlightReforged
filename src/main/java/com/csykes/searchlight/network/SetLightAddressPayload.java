package com.csykes.searchlight.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetLightAddressPayload(BlockPos pos, String address, String mode) implements CustomPacketPayload {
    public SetLightAddressPayload(BlockPos pos, String address) {
        this(pos, address, "FIXTURE");
    }

    public static final Type<SetLightAddressPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("searchlight", "set_light_address"));

    public static final StreamCodec<FriendlyByteBuf, SetLightAddressPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetLightAddressPayload::pos,
            ByteBufCodecs.STRING_UTF8, SetLightAddressPayload::address,
            ByteBufCodecs.STRING_UTF8, SetLightAddressPayload::mode,
            SetLightAddressPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
