/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BiomeMessage extends ScheduledTask {

    private final ServerPlayerEntity player;
    private final Identifier biomeId;
    
    public BiomeMessage(ServerPlayerEntity player, Identifier biomeId) {
        super(Util.serverConfig.getChangeBiomeDelay(), 0);
        this.player = player;
        this.biomeId = biomeId;
    }

    @Override
    public void onTrigger() {
        MutableText biomeText = null;
        if (biomeId == null) {
            biomeText = Util.parseTranslatableText("fmod.misc.unknown");
        } else {
            biomeText = Text.translatable("biome." + biomeId.toString().replace(":", "."));
        }
        Util.postMessage(player, Util.serverConfig.getChangeBiome(), MessageLocation.ACTIONBAR, Util.parseTranslatableText("fmod.message.biome.change", player.getDisplayName(), biomeText));
    }

    @Override
    public boolean shouldCancel() {
        if (player == null || player.isDisconnected() || player.isRemoved()) {
            return true;
        }
        Identifier currentBiomeId = player.getWorld().getBiome(player.getBlockPos()).getKey().map(key -> key.getValue()).orElse(null);
        if (currentBiomeId.equals(biomeId)) {
            return false;
        } else {
            // During delay period, if the player changes to a new biome, the scheduled task should be cancelled.
            return true;
        }
    }
}
