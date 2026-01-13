package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class BiomeMessage extends ScheduledTask {

    private ServerPlayer player;
    private ResourceLocation biomeId;
    
    public BiomeMessage(ServerPlayer player, ResourceLocation biomeId) {
        super(Util.serverConfig.getChangeBiomeDelay(), 0);
        this.player = player;
        this.biomeId = biomeId;
    }

    @Override
    public void onTrigger() {
        MutableComponent biomeText = null;
        if (biomeId == null) {
            biomeText = Util.parseTranslateableText("fmod.misc.unknown");
        } else {
            biomeText = Component.translatable("biome." + biomeId.toString().replace(":", "."));
        }
        Util.postMessage(player, Util.serverConfig.getChangeBiome(), MessageLocation.ACTIONBAR, Util.parseTranslateableText("fmod.message.biome.change", player.getDisplayName(), biomeText));
    }

    @Override
    public boolean shouldCancel() {
        if (player.hasDisconnected() || player.isRemoved()) {
            return true;
        }
        ResourceLocation currentBiomeId = player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null);
        if (currentBiomeId.equals(biomeId)) {
            return false;
        } else {
            // During delay period, if the player changes to a new biome, the scheduled task should be cancelled.
            return true;
        }
    }
}
