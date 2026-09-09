package com.csykes.searchlight.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnlinkDirectorLightPayload(BlockPos directorPos, int slot) implements CustomPacketPayload {
    public static final Type<UnlinkDirectorLightPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("searchlight", "unlink_director_light"));

    public static final StreamCodec<ByteBuf, UnlinkDirectorLightPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, UnlinkDirectorLightPayload::directorPos,
            ByteBufCodecs.VAR_INT, UnlinkDirectorLightPayload::slot,
            UnlinkDirectorLightPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
