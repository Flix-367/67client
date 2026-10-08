package com.suschunk.addon.mixin;

import com.suschunk.addon.suschunk.ServerLightCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class LightPacketMixin {
    @Inject(method = "onLightUpdate", at = @At("TAIL"))
    private void suschunk$captureLightUpdate(LightUpdateS2CPacket packet, CallbackInfo ci) {
        ServerLightCache.get().ingest(packet.getChunkX(), packet.getChunkZ(), packet.getData(), MinecraftClient.getInstance().world);
    }

    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void suschunk$captureChunkLight(ChunkDataS2CPacket packet, CallbackInfo ci) {
        ServerLightCache.get().ingest(packet.getChunkX(), packet.getChunkZ(), packet.getLightData(), MinecraftClient.getInstance().world);
    }
}
