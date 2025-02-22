package com.ykn.fmod.server.base.event;

import java.util.List;

import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.schedule.FightMessage;
import com.ykn.fmod.server.base.schedule.PlayerHurtMessage;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

public class LivingEntityDamage {

    private LivingEntity entity;
    private DamageSource damageSource;
    private float amount;

    public LivingEntityDamage(LivingEntity entity, DamageSource damageSource, float amount) {
        this.entity = entity;
        this.damageSource = damageSource;
        this.amount = amount;
    }

    /**
     * This method is called when a living entity is damaged.
     * @see com.ykn.fmod.mixin.LivingEntityDamageMixin
     */
    public void onDamage() {
        if (entity.getServer() == null) {
            return;
        }
        if (damageSource == null) {
            return;
        }

        MinecraftServer server = entity.getServer();
        Entity attacker = damageSource.getSource();

        if (attacker != null && attacker.isPlayer() && attacker instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) attacker;
            PlayerData data = Util.getServerData(server).getPlayerData(player);
            if (entity.getMaxHealth() > Util.serverConfig.getBossMaxHpThreshold() && Util.getServerData(server).getTickPassed(data.lastBossFightTick) > Util.serverConfig.getBossFightInterval() && amount > 0) {
                data.lastBossFightTick = Util.getServerData(server).getServerTick();
                Util.getServerData(server).submitScheduledTask(new FightMessage(player, entity));
            }
        }

        if (entity.isPlayer() && entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            PlayerData data = Util.getServerData(server).getPlayerData(player);
            if (attacker != null && attacker instanceof Monster && Util.getServerData(server).getTickPassed(data.lastMonsterSurroundTick) > Util.serverConfig.getMonsterSurroundInterval() && amount > 0) {
                double distance = Util.serverConfig.getMonsterDistanceThreshold();
                Box box = new Box(player.getX() - distance, player.getY() - distance, player.getZ() - distance, player.getX() + distance, player.getY() + distance, player.getZ() + distance);
                List<Entity> nearestEntities = player.getWorld().getEntitiesByClass(Entity.class, box, monster -> (monster != null && (monster instanceof Monster)));
                if (nearestEntities.size() >= Util.serverConfig.getMonsterNumberThreshold() && player.getHealth() > 0) {
                    Text playerName = player.getDisplayName();
                    Text entityName = attacker.getDisplayName();
                    MutableText text = Util.parseTranslateableText("fmod.message.monsterattack", playerName, entityName, Integer.toString(nearestEntities.size()));
                    Util.postMessage(player, Util.serverConfig.getMonsterSurroundMessageReceiver(), Util.serverConfig.getMonsterSurroundMessageLocation(), text);
                    data.lastMonsterSurroundTick = Util.getServerData(server).getServerTick();
                }
            }
            Util.getServerData(server).submitScheduledTask(new PlayerHurtMessage(player, player.getHealth()));
        }
    }

}
