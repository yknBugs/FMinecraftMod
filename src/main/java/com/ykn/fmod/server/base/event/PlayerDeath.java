package com.ykn.fmod.server.base.event;

import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

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

        Text playerName = player.getDisplayName();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double pitch = player.getPitch();
        double yaw = player.getYaw();
        String strDim = player.getWorld().getRegistryKey().getValue().toString();
        String strX = String.format("%.2f", x);
        String strY = String.format("%.2f", y);
        String strZ = String.format("%.2f", z);
        String strPitch = String.format("%.2f", pitch);
        String strYaw = String.format("%.2f", yaw);
        MutableText biomeText = Util.getBiomeText(player);
        MutableText text = Util.parseTranslateableText("fmod.message.playerdeathcoord", playerName, biomeText, strX, strY, strZ).styled(style -> style.withClickEvent(
            new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in " + strDim + " run tp @s " + strX + " " + strY + " " + strZ + " " + strYaw + " " + strPitch)
        ).withHoverEvent(
            new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clicktp"))
        ));
        Util.postMessage(player, Util.serverConfig.getPlayerDeathCoord(), MessageLocation.CHAT, text);
    }

}
