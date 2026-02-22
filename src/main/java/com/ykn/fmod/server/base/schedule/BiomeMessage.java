/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class BiomeMessage extends ScheduledTask {

    private final ServerPlayer player;
    private final ResourceLocation biomeId;
    
    public BiomeMessage(ServerPlayer player, ResourceLocation biomeId) {
        super(Util.serverConfig.getChangeBiomeDelay(), 0);
        this.player = player;
        this.biomeId = biomeId;
    }

    @Override
    public void onTrigger() {
        MutableComponent biomeText = null;
        if (biomeId == null) {
            biomeText = Util.parseTranslatableText("fmod.misc.unknown");
        } else {
            biomeText = Component.translatable("biome." + biomeId.toString().replace(":", "."));
        }
        Component mainText = Util.parseTranslatableText("fmod.message.biome.change.main", player.getDisplayName(), Util.parseCoordText(player));
        Component otherText = Util.parseTranslatableText("fmod.message.biome.change.other", player.getDisplayName(), biomeText);
        Util.serverConfig.getChangeBiomeMessage().postMessage(player, mainText, otherText);
    }

    @Override
    public boolean shouldCancel() {
        if (player == null || player.hasDisconnected() || player.isRemoved() || player.getHealth() <= 0) {
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

    @Override
    public String toString() {
        return "BiomeMessage{player='" + player.getDisplayName().getString() + "', biomeId='" + biomeId + "'}";
    }
}
