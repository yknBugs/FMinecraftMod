package com.ykn.fmod.server.base.event;

import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class PlayerDeath {

    private ServerPlayer player;
    private DamageSource damageSource;

    public PlayerDeath(ServerPlayer player, DamageSource damageSource) {
        this.player = player;
        this.damageSource = damageSource;
    }

    public ServerPlayer getPlayer() {
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

        LivingEntity killer = player.getKillCredit();
        if (killer != null && killer.isAlwaysTicking() == false) {
            Util.getServerData(killer.getServer()).addKillerEntity(killer);
        }

        Component playerName = player.getDisplayName();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double pitch = player.getXRot();
        double yaw = player.getYRot();
        String strDim = player.level().dimension().location().toString();
        String strX = String.format("%.2f", x);
        String strY = String.format("%.2f", y);
        String strZ = String.format("%.2f", z);
        String strPitch = String.format("%.2f", pitch);
        String strYaw = String.format("%.2f", yaw);
        MutableComponent biomeText = Util.getBiomeText(player);
        MutableComponent text = Util.parseTranslateableText("fmod.message.playerdeathcoord", playerName, biomeText, strX, strY, strZ).withStyle(style -> style.withClickEvent(
            new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in " + strDim + " run tp @s " + strX + " " + strY + " " + strZ + " " + strYaw + " " + strPitch)
        ).withHoverEvent(
            new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clicktp"))
        ));
        Util.postMessage(player, Util.serverConfig.getPlayerDeathCoord(), MessageLocation.CHAT, text);
    }

}
