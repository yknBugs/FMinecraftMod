package com.ykn.fmod.server.base.event;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public class PlayerDeath {

    private ServerPlayerEntity player;
    private DamageSource damageSource;

    public PlayerDeath(ServerPlayerEntity player, DamageSource damageSource) {
        this.player = player;
        this.damageSource = damageSource;
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    /**
     * This method is called when a player dies.
     * @see com.ykn.fmod.server.mixin.PlayerDeathMixin
     */
    public void onPlayerDeath() {
        if (player.getServer() == null) {
            return;
        }

        LivingEntity killer = player.getPrimeAdversary();
        if (killer != null && killer.isPlayer() == false) {
            Util.getServerData(killer.getServer()).addKillerEntity(killer);
        }
        if (Util.serverConfig.isBcPlayerDeathCoord()) {
            Text playerName = player.getDisplayName();
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            String strX = String.format("%.2f", x);
            String strY = String.format("%.2f", y);
            String strZ = String.format("%.2f", z);
            Identifier biomeId = player.getWorld().getBiome(player.getBlockPos()).getKey().map(key -> key.getValue()).orElse(null);
            MutableText biomeText = null;
            if (biomeId == null) {
                biomeText = Util.parseTranslateableText("fmod.misc.unknown");
            } else {
                // Vanilla should contain this translation key.
                biomeText = Text.translatable("biome." + biomeId.toString().replace(":", "."));
            }
            MutableText text = Util.parseTranslateableText("fmod.message.playerdeathcoord", playerName, biomeText, strX, strY, strZ).styled(style -> style.withClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + strX + " " + strY + " " + strZ)
            ).withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clicktp"))
            ));
            Util.broadcastTextMessage(player.getServer(), text);
        }
    }

}
