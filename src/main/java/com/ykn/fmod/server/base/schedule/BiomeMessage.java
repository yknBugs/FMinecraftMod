package com.ykn.fmod.server.base.schedule;

import java.util.Objects;

import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class BiomeMessage extends ScheduledTask {

    private ServerPlayerEntity player;
    private Identifier biomeId;
    
    public BiomeMessage(ServerPlayerEntity player, Identifier biomeId) {
        super(Util.serverConfig.getChangeBiomeDelay(), 0);
        this.player = player;
        this.biomeId = biomeId;
    }

    @Override
    public void onTrigger() {
        MutableText biomeText = null;
        if (biomeId == null) {
            biomeText = Util.parseTranslateableText("fmod.misc.unknown");
        } else {
            biomeText = new TranslatableText("biome." + biomeId.toString().replace(":", "."));
        }
        Util.postMessage(player, Util.serverConfig.getChangeBiome(), MessageLocation.ACTIONBAR, Util.parseTranslateableText("fmod.message.biome.change", player.getDisplayName(), biomeText));
    }

    @Override
    public boolean shouldCancel() {
        if (player.isDisconnected() || player.isRemoved()) {
            return true;
        }
        Identifier currentBiomeId = player.getWorld().getRegistryManager().get(Registry.BIOME_KEY).getId(player.getWorld().getBiome(player.getBlockPos()));
        // if (currentBiomeId.equals(biomeId)) {
        if (Objects.equals(currentBiomeId, biomeId)) {
            return false;
        } else {
            // During delay period, if the player changes to a new biome, the scheduled task should be cancelled.
            return true;
        }
    }
}
