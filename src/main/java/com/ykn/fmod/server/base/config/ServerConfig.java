/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.config;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ykn.fmod.server.base.util.MessageReceiver;
import com.ykn.fmod.server.base.util.MessageLocation;

public class ServerConfig extends ConfigReader {

    // Multiple threads may access the config class at the same time, the getters and setters in this class must be locked to ensure thread safety.
    protected final transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * If enabled, the server will translate all the messages and then send to the client.
     * This is useful if only the server has this mod installed and the client does not have it.
     * But if enabled, the message will not follow the client's language setting.
     * Default: true
     */
    protected boolean serverTranslation;

    /**
     * The maximum number of nodes that can be executed in a single flow execution.
     * This is designed to prevent infinite loops in flow executions.
     * Default: 32767
     */
    protected int maxFlowLength;

    /**
     * How many history records of flow executions to keep.
     * Default: 32767
     */
    protected int keepFlowHistoryNumber;

    /**
     * The message sent to the client when a non-hostile and non-passive entity dies.
     * Default: NONE
     */
    protected MessageLocation entityDeathMessage;

    /**
     * The message sent to the client when a hostile entity dies.
     * Default: NONE
     */
    protected MessageLocation hostileDeathMessage;

    /**
     * The message sent to the client when a passive entity dies.
     * Default: NONE
     */
    protected MessageLocation passiveDeathMessage;

    /**
     * The message sent to the client when a boss dies.
     * Default: NONE
     */
    protected MessageLocation bossDeathMessage;

    /**
     * The message sent to the client when a mob with custom name dies.
     * Default: NONE
     */
    protected MessageLocation namedEntityDeathMessage;

    /**
     * The message sent to the client when a mob - the mod that has once killed a player before - dies.
     * Default: NONE
     */
    protected MessageLocation killerDeathMessage;

    /**
     * The message sent to the player when the player can sleep or cannot sleep.
     * Default: NONE
     */
    protected MessageLocation playerCanSleepMessage;

    /**
     * If an entity has a health greater than this value, it will be considered as a boss.
     * Default: 150
     */
    protected double bossMaxHpThreshold;

    /**
     * Controls where to show the message when a player dies.
     * Default: NONE
     */
    protected MessageLocation playerDeathCoordLocation;

    /**
     * Controls who can receive the coordinates when a player dies.
     * Default: NONE
     */
    protected MessageReceiver playerDeathCoordReceiver;

    /**
     * Controls where to show the message when a projectile thrown by a player hits another entity.
     * Default: NONE
     */
    protected MessageLocation projectileHitOthersLocation;

    /**
     * Controls who can receive the message when a projectile thrown by a player hits another entity.
     * Default: NONE
     */
    protected MessageReceiver projectileHitOthersReceiver;

    /**
     * Controls where to show the message when a projectile thrown by another entity hits the player.
     * Default: NONE
     */
    protected MessageLocation projectileBeingHitLocation;

    /**
     * Controls who can receive the message when a projectile thrown by another entity hits the player.
     * Default: NONE
     */
    protected MessageReceiver projectileBeingHitReceiver;

    /**
     * Controls where to show the message when a player is suspected of being AFK.
     * This message will not disappear until the player comes back.
     * Default: NONE
     */
    protected MessageLocation informAfkingLocation;

    /**
     * Controls who can receive the message when a player is suspected of being AFK.
     * This message will not disappear until the player comes back.
     * Default: NONE
     */
    protected MessageReceiver informAfkingReceiver;

    /**
     * The threshold of the time in ticks that a player is suspected of being AFK.
     * Default: 1200 Ticks (1 minute)
     */
    protected int informAfkingThreshold;

    /**
     * Controls where to show the message when a player is confirmed to be AFK.
     * This message will be sent only once.
     * Default: NONE
     */
    protected MessageLocation broadcastAfkingLocation;

    /**
     * Controls who can receive the message when a player is confirmed to be AFK.
     * This message will be sent only once.
     * Default: NONE
     */
    protected MessageReceiver broadcastAfkingReceiver;

    /**
     * The threshold of the time in ticks that a player is confirmed to be AFK.
     * Default: 6000 Ticks (5 minutes)
     */
    protected int broadcastAfkingThreshold;

    /**
     * Controls where to show the message when a player is back from AFK.
     * This message will be sent only once.
     * Default: NONE
     */
    protected MessageLocation stopAfkingLocation;

    /**
     * Controls who can receive the message when a player is back from AFK.
     * This message will be sent only once.
     * Default: NONE
     */
    protected MessageReceiver stopAfkingReceiver;

    /**
     * Controls where to show the message when a player changes the biome.
     * Default: NONE
     */
    protected MessageLocation changeBiomeLocation;

    /**
     * Controls who can receive the message when a player changes the biome.
     * Default: NONE
     */
    protected MessageReceiver changeBiomeReceiver;

    /**
     * The delay in ticks before sending the message when a player changes the biome.
     * This is designed to avoid spamming when a player frequently crosses the boundary of two biomes.
     * Default: 200 Ticks (10 seconds)
     */
    protected int changeBiomeDelay;

    /**
     * Controls where to show the message when a player attack a boss.
     * Default: NONE
     */
    protected MessageLocation bossFightLocation;

    /**
     * Controls who can receive the message when a player attack a boss.
     * Default: NONE
     */
    protected MessageReceiver bossFightReceiver;

    /**
     * The interval in ticks before sending the message when a player attack a boss.
     * This is designed to avoid spamming messages
     * Default: 1200 Ticks (60 seconds)
     */
    protected int bossFightInterval;

    /**
     * Controls where to show the message when a player is surrounded by monsters.
     * Default: NONE
     */
    protected MessageLocation monsterSurroundLocation;

    /**
     * Controls who can receive the message when a player is surrounded by monsters.
     * Default: NONE
     */
    protected MessageReceiver monsterSurroundReceiver;

    /**
     * The interval in ticks before sending the message when a player is surrounded by monsters.
     * This is designed to avoid spamming messages
     * Default: 1200 Ticks (60 seconds)
     */
    protected int monsterSurroundInterval;

    /**
     * If the number of the monsters near a player is larger than this value, the player will be considered as surrounded by monsters.
     * Default: 8
     */
    protected int monsterNumberThreshold;

    /**
     * If the distance between a player and a monster is less than this value, the monster will be considered as near the player.
     * Default: 12
     */
    protected double monsterDistanceThreshold;

    /**
     * Controls where to show the message when the number of entities in the server is larger than the threshold.
     * Default: NONE
     */
    protected MessageLocation entityNumberWarning;

    /**
     * If the number of the entities in the server is larger than this value, the warning message will be sent.
     * This is designed to avoid too many entities lagging the server.
     * Default: 3000
     */
    protected int entityNumberThreshold;

    /**
     * The interval in ticks to check how many entities are there in the server.
     * Frequently checking the number of entities may cause lag.
     * Default: 20 (1 second)
     */
    protected int entityNumberInterval;

    /**
     * Controls where to show the message when a player is seriously hurt.
     * Default: NONE
     */
    protected MessageLocation playerSeriousHurtLocation;

    /**
     * Controls who can receive the message when a player is seriously hurt.
     * Default: NONE
     */
    protected MessageReceiver playerSeriousHurtReceiver;

    /**
     * If a player receives a damage larger than this percentage of his max health, he will be considered as seriously hurt.
     * Default: 0.8 (80%)
     */
    protected double playerHurtThreshold;

    /**
     * Controls where to show the message when a player travels a long distance in a short time.
     * Default: NONE
     */
    protected MessageLocation travelMessageLocation;

    /**
     * Controls who can receive the message when a player travels a long distance in a short time.
     * Default: NONE
     */
    protected MessageReceiver travelMessageReceiver;

    /**
     * How many recent ticks to track for long-distance travel detection.
     * Default: 600 ticks (30 seconds)
     */
    protected int travelWindowTicks;

    /**
     * The total horizontal distance required within the tracked window to consider it long-distance travel.
     * Default: 100 blocks
     */
    protected double travelTotalDistanceThreshold;

    /**
     * Interval in ticks for partial distance checks within the travel window.
     * Default: 200 ticks (10 seconds)
     */
    protected int travelPartialInterval;

    /**
     * The minimum horizontal distance required between two positions separated by the partial interval.
     * Default: 40 blocks
     */
    protected double travelPartialDistanceThreshold;

    /**
     * The maximum allowed single-tick horizontal distance before it is considered a teleport.
     * Default: 75 blocks
     */
    protected double teleportThreshold;

    /**
     * Controls where to show the message when a player teleports.
     * Default: NONE
     */
    protected MessageLocation teleportMessageLocation;

    /**
     * Controls who can receive the message when a player teleports.
     * Default: NONE
     */
    protected MessageReceiver teleportMessageReceiver;

    /**
     * The URL of the target GPT server.
     * This mod will use the OpenAI API.
     * So if you want to deploy a local LLM, you must make sure it is compatible with the OpenAI API.
     * Default: A server created by llama-server --host 0.0.0.0 --port 12345 (Search the llama.cpp project on github for more information)
     */
    protected String gptUrl;

    /**
     * The access tokens of the GPT server.
     * If the sever does not need an access token, set it to an empty string, but not null.
     * Default: ""
     */
    protected String gptAccessTokens;

    /**
     * The model of the GPT server.
     * Default: ""
     */
    protected String gptModel;

    /**
     * The system prompt of generating the response.
     * Default: ""
     */
    protected String gptSystemPrompt;

    /**
     * The temperature parameter of generating the response.
     * Default: 0.8
     */
    protected double gptTemperature;

    /**
     * The timeout of the GPT server in milliseconds.
     * Default: 60000 (60 seconds)
     */
    protected int gptServerTimeout;

    public ServerConfig() {
        super("server.json");
        this.serverTranslation = true;
        this.maxFlowLength = 32767;
        this.keepFlowHistoryNumber = 32767;
        this.entityDeathMessage = MessageLocation.NONE;
        this.hostileDeathMessage = MessageLocation.NONE;
        this.passiveDeathMessage = MessageLocation.NONE;
        this.bossDeathMessage = MessageLocation.NONE;
        this.namedEntityDeathMessage = MessageLocation.NONE;
        this.playerCanSleepMessage = MessageLocation.NONE;
        this.killerDeathMessage = MessageLocation.NONE;
        this.bossMaxHpThreshold = 150;
        this.playerDeathCoordLocation = MessageLocation.NONE;
        this.playerDeathCoordReceiver = MessageReceiver.NONE;
        this.projectileHitOthersLocation = MessageLocation.NONE;
        this.projectileHitOthersReceiver = MessageReceiver.NONE;
        this.projectileBeingHitLocation = MessageLocation.NONE;
        this.projectileBeingHitReceiver = MessageReceiver.NONE;
        this.informAfkingLocation = MessageLocation.NONE;
        this.informAfkingReceiver = MessageReceiver.NONE;
        this.informAfkingThreshold = 1200;
        this.broadcastAfkingLocation = MessageLocation.NONE;
        this.broadcastAfkingReceiver = MessageReceiver.NONE;
        this.broadcastAfkingThreshold = 6000;
        this.stopAfkingLocation = MessageLocation.NONE;
        this.stopAfkingReceiver = MessageReceiver.NONE;
        this.changeBiomeLocation = MessageLocation.NONE;
        this.changeBiomeReceiver = MessageReceiver.NONE;
        this.changeBiomeDelay = 200;
        this.bossFightLocation = MessageLocation.NONE;
        this.bossFightReceiver = MessageReceiver.NONE;
        this.bossFightInterval = 1200;
        this.monsterSurroundLocation = MessageLocation.NONE;
        this.monsterSurroundReceiver = MessageReceiver.NONE;
        this.monsterSurroundInterval = 1200;
        this.monsterNumberThreshold = 8;
        this.monsterDistanceThreshold = 12.0;
        this.entityNumberWarning = MessageLocation.NONE;
        this.entityNumberThreshold = 3000;
        this.entityNumberInterval = 20;
        this.playerSeriousHurtLocation = MessageLocation.NONE;
        this.playerSeriousHurtReceiver = MessageReceiver.NONE;
        this.playerHurtThreshold = 0.8;
        this.travelMessageLocation = MessageLocation.NONE;
        this.travelMessageReceiver = MessageReceiver.NONE;
        this.travelWindowTicks = 600;
        this.travelTotalDistanceThreshold = 100.0;
        this.travelPartialInterval = 200;
        this.travelPartialDistanceThreshold = 40.0;
        this.teleportThreshold = 75.0;
        this.teleportMessageLocation = MessageLocation.NONE;
        this.teleportMessageReceiver = MessageReceiver.NONE;
        this.gptUrl = "http://127.0.0.1:12345/v1/chat/completions";
        this.gptAccessTokens = "";
        this.gptModel = "";
        this.gptSystemPrompt = "";
        this.gptTemperature = 0.8;
        this.gptServerTimeout = 60000;
    }

    public boolean isEnableServerTranslation() {
        lock.readLock().lock();
        try {
            return serverTranslation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEnableServerTranslation(boolean serverTranslation) {
        lock.writeLock().lock();
        try {
            this.serverTranslation = serverTranslation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getMaxFlowLength() {
        lock.readLock().lock();
        try {
            if (maxFlowLength <= 0) {
                return 32767;
            }
            return maxFlowLength;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMaxFlowLength(int maxFlowLength) {
        lock.writeLock().lock();
        try {
            if (maxFlowLength <= 0) {
                this.maxFlowLength = 32767;
            } else {
                this.maxFlowLength = maxFlowLength;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getKeepFlowHistoryNumber() {
        lock.readLock().lock();
        try {
            if (keepFlowHistoryNumber < 0) {
                return 0;
            }
            return keepFlowHistoryNumber;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setKeepFlowHistoryNumber(int keepFlowHistoryNumber) {
        lock.writeLock().lock();
        try {
            if (keepFlowHistoryNumber < 0) {
                this.keepFlowHistoryNumber = 0;
            } else {
                this.keepFlowHistoryNumber = keepFlowHistoryNumber;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getEntityDeathMessage() {
        lock.readLock().lock();
        try {
            return entityDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityDeathMessage(MessageLocation entityDeathMessage) {
        lock.writeLock().lock();
        try {
            this.entityDeathMessage = entityDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getHostileDeathMessage() {
        lock.readLock().lock();
        try {
            return hostileDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setHostileDeathMessage(MessageLocation hostileDeathMessage) {
        lock.writeLock().lock();
        try {
            this.hostileDeathMessage = hostileDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getPassiveDeathMessage() {
        lock.readLock().lock();
        try {
            return passiveDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPassiveDeathMessage(MessageLocation passiveDeathMessage) {
        lock.writeLock().lock();
        try {
            this.passiveDeathMessage = passiveDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getBossDeathMessage() {
        lock.readLock().lock();
        try {
            return bossDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBossDeathMessage(MessageLocation bossDeathMessage) {
        lock.writeLock().lock();
        try {
            this.bossDeathMessage = bossDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getNamedEntityDeathMessage() {
        lock.readLock().lock();
        try {
            return namedEntityDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setNamedEntityDeathMessage(MessageLocation namedEntityDeathMessage) {
        lock.writeLock().lock();
        try {
            this.namedEntityDeathMessage = namedEntityDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getKillerEntityDeathMessage() {
        lock.readLock().lock();
        try {
            return killerDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setKillerEntityDeathMessage(MessageLocation killerDeathMessage) {
        lock.writeLock().lock();
        try {
            this.killerDeathMessage = killerDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getPlayerCanSleepMessage() {
        lock.readLock().lock();
        try {
            return playerCanSleepMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPlayerCanSleepMessage(MessageLocation playerCanSleepMessage) {
        lock.writeLock().lock();
        try {
            this.playerCanSleepMessage = playerCanSleepMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getBossMaxHpThreshold() {
        lock.readLock().lock();
        try {
            if (bossMaxHpThreshold < 0) {
                return 0;
            }
            return bossMaxHpThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBossMaxHpThreshold(double bossMaxHpThreshold) {
        lock.writeLock().lock();
        try {
            if (bossMaxHpThreshold < 0) {
                this.bossMaxHpThreshold = 0;
            } else {
                this.bossMaxHpThreshold = bossMaxHpThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getPlayerDeathCoordLocation() {
        lock.readLock().lock();
        try {
            return playerDeathCoordLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPlayerDeathCoordLocation(MessageLocation playerDeathCoordLocation) {
        lock.writeLock().lock();
        try {
            this.playerDeathCoordLocation = playerDeathCoordLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getPlayerDeathCoordReceiver() {
        lock.readLock().lock();
        try {
            return playerDeathCoordReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPlayerDeathCoordReceiver(MessageReceiver playerDeathCoordReceiver) {
        lock.writeLock().lock();
        try {
            this.playerDeathCoordReceiver = playerDeathCoordReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getProjectileHitOthersLocation() {
        lock.readLock().lock();
        try {
            return projectileHitOthersLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setProjectileHitOthersLocation(MessageLocation projectileHitOthersLocation) {
        lock.writeLock().lock();
        try {
            this.projectileHitOthersLocation = projectileHitOthersLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getProjectileHitOthersReceiver() {
        lock.readLock().lock();
        try {
            return projectileHitOthersReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setProjectileHitOthersReceiver(MessageReceiver projectileHitOthersReceiver) {
        lock.writeLock().lock();
        try {
            this.projectileHitOthersReceiver = projectileHitOthersReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getProjectileBeingHitLocation() {
        lock.readLock().lock();
        try {
            return projectileBeingHitLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setProjectileBeingHitLocation(MessageLocation projectileBeingHitLocation) {
        lock.writeLock().lock();
        try {
            this.projectileBeingHitLocation = projectileBeingHitLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getProjectileBeingHitReceiver() {
        lock.readLock().lock();
        try {
            return projectileBeingHitReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setProjectileBeingHitReceiver(MessageReceiver projectileBeingHitReceiver) {
        lock.writeLock().lock();
        try {
            this.projectileBeingHitReceiver = projectileBeingHitReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getInformAfkingLocation() {
        lock.readLock().lock();
        try {
            return informAfkingLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setInformAfkingLocation(MessageLocation informAfkingLocation) {
        lock.writeLock().lock();
        try {
            this.informAfkingLocation = informAfkingLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getInformAfkingReceiver() {
        lock.readLock().lock();
        try {
            return informAfkingReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setInformAfkingReceiver(MessageReceiver informAfkingReceiver) {
        lock.writeLock().lock();
        try {
            this.informAfkingReceiver = informAfkingReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getInformAfkingThreshold() {
        lock.readLock().lock();
        try {
            if (informAfkingThreshold < 0) {
                return 0;
            }
            return informAfkingThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setInformAfkingThreshold(int informAfkingThreshold) {
        lock.writeLock().lock();
        try {
            if (informAfkingThreshold < 0) {
                this.informAfkingThreshold = 0;
            } else {
                this.informAfkingThreshold = informAfkingThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getBroadcastAfkingLocation() {
        lock.readLock().lock();
        try {
            return broadcastAfkingLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBroadcastAfkingLocation(MessageLocation broadcastAfkingLocation) {
        lock.writeLock().lock();
        try {
            this.broadcastAfkingLocation = broadcastAfkingLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getBroadcastAfkingReceiver() {
        lock.readLock().lock();
        try {
            return broadcastAfkingReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBroadcastAfkingReceiver(MessageReceiver broadcastAfkingReceiver) {
        lock.writeLock().lock();
        try {
            this.broadcastAfkingReceiver = broadcastAfkingReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getBroadcastAfkingThreshold() {
        lock.readLock().lock();
        try {
            if (broadcastAfkingThreshold < 0) {
                return 0;
            }
            return broadcastAfkingThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBroadcastAfkingThreshold(int broadcastAfkingThreshold) {
        lock.writeLock().lock();
        try {
            if (broadcastAfkingThreshold < 0) {
                this.broadcastAfkingThreshold = 0;
            } else {
                this.broadcastAfkingThreshold = broadcastAfkingThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getStopAfkingLocation() {
        lock.readLock().lock();
        try {
            return stopAfkingLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setStopAfkingLocation(MessageLocation stopAfkingLocation) {
        lock.writeLock().lock();
        try {
            this.stopAfkingLocation = stopAfkingLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getStopAfkingReceiver() {
        lock.readLock().lock();
        try {
            return stopAfkingReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setStopAfkingReceiver(MessageReceiver stopAfkingReceiver) {
        lock.writeLock().lock();
        try {
            this.stopAfkingReceiver = stopAfkingReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getChangeBiomeLocation() {
        lock.readLock().lock();
        try {
            return changeBiomeLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setChangeBiomeLocation(MessageLocation changeBiomeLocation) {
        lock.writeLock().lock();
        try {
            this.changeBiomeLocation = changeBiomeLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getChangeBiomeReceiver() {
        lock.readLock().lock();
        try {
            return changeBiomeReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setChangeBiomeReceiver(MessageReceiver changeBiomeReceiver) {
        lock.writeLock().lock();
        try {
            this.changeBiomeReceiver = changeBiomeReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getChangeBiomeDelay() {
        lock.readLock().lock();
        try {
            if (changeBiomeDelay < 0) {
                return 0;
            }
            return changeBiomeDelay;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setChangeBiomeDelay(int changeBiomeDelay) {
        lock.writeLock().lock();
        try {
            if (changeBiomeDelay < 0) {
                this.changeBiomeDelay = 0;
            } else {
                this.changeBiomeDelay = changeBiomeDelay;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getBossFightMessageLocation() {
        lock.readLock().lock();
        try {
            return bossFightLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBossFightMessageLocation(MessageLocation bossFightLocation) {
        lock.writeLock().lock();
        try {
            this.bossFightLocation = bossFightLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getBossFightMessageReceiver() {
        lock.readLock().lock();
        try {
            return bossFightReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBossFightMessageReceiver(MessageReceiver bossFightReceiver) {
        lock.writeLock().lock();
        try {
            this.bossFightReceiver = bossFightReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getBossFightInterval() {
        lock.readLock().lock();
        try {
            if (bossFightInterval < 0) {
                return 0;
            }
            return bossFightInterval;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBossFightInterval(int bossFightInterval) {
        lock.writeLock().lock();
        try {
            if (bossFightInterval < 0) {
                this.bossFightInterval = 0;
            } else {
                this.bossFightInterval = bossFightInterval;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getMonsterSurroundMessageLocation() {
        lock.readLock().lock();
        try {
            return monsterSurroundLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMonsterSurroundMessageLocation(MessageLocation monsterSurroundLocation) {
        lock.writeLock().lock();
        try {
            this.monsterSurroundLocation = monsterSurroundLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getMonsterSurroundMessageReceiver() {
        lock.readLock().lock();
        try {
            return monsterSurroundReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMonsterSurroundMessageReceiver(MessageReceiver monsterSurroundReceiver) {
        lock.writeLock().lock();
        try {
            this.monsterSurroundReceiver = monsterSurroundReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getMonsterSurroundInterval() {
        lock.readLock().lock();
        try {
            if (monsterSurroundInterval < 0) {
                return 0;
            }
            return monsterSurroundInterval;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMonsterSurroundInterval(int monsterSurroundInterval) {
        lock.writeLock().lock();
        try {
            if (monsterSurroundInterval < 0) {
                this.monsterSurroundInterval = 0;
            } else {
                this.monsterSurroundInterval = monsterSurroundInterval;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getMonsterNumberThreshold() {
        lock.readLock().lock();
        try {
            if (monsterNumberThreshold < 0) {
                return 0;
            }
            return monsterNumberThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMonsterNumberThreshold(int monsterNumberThreshold) {
        lock.writeLock().lock();
        try {
            if (monsterNumberThreshold < 0) {
                this.monsterNumberThreshold = 0;
            } else {
                this.monsterNumberThreshold = monsterNumberThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getMonsterDistanceThreshold() {
        lock.readLock().lock();
        try {
            if (monsterDistanceThreshold < 0) {
                return 0.0;
            }
            return monsterDistanceThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMonsterDistanceThreshold(double monsterDistanceThreshold) {
        lock.writeLock().lock();
        try {
            if (monsterDistanceThreshold < 0) {
                this.monsterDistanceThreshold = 0;
            } else {
                this.monsterDistanceThreshold = monsterDistanceThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getEntityNumberWarning() {
        lock.readLock().lock();
        try {
            return entityNumberWarning;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityNumberWarning(MessageLocation entityNumberWarning) {
        lock.writeLock().lock();
        try {
            this.entityNumberWarning = entityNumberWarning;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getEntityNumberThreshold() {
        lock.readLock().lock();
        try {
            if (entityNumberThreshold < 0) {
                return 0;
            }
            return entityNumberThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityNumberThreshold(int entityNumberThreshold) {
        lock.writeLock().lock();
        try {
            if (entityNumberThreshold < 0) {
                this.entityNumberThreshold = 0;
            } else {
                this.entityNumberThreshold = entityNumberThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getEntityNumberInterval() {
        lock.readLock().lock();
        try {
            if (entityNumberInterval <= 0) {
                return 1;
            }
            return entityNumberInterval;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityNumberInterval(int entityNumberInterval) {
        lock.writeLock().lock();
        try {
            if (entityNumberInterval <= 0) {
                this.entityNumberInterval = 1;
            } else {
                this.entityNumberInterval = entityNumberInterval;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getPlayerSeriousHurtLocation() {
        lock.readLock().lock();
        try {
            return playerSeriousHurtLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPlayerSeriousHurtLocation(MessageLocation playerSeriousHurtLocation) {
        lock.writeLock().lock();
        try {
            this.playerSeriousHurtLocation = playerSeriousHurtLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getPlayerSeriousHurtReceiver() {
        lock.readLock().lock();
        try {
            return playerSeriousHurtReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPlayerSeriousHurtReceiver(MessageReceiver playerSeriousHurtReceiver) {
        lock.writeLock().lock();
        try {
            this.playerSeriousHurtReceiver = playerSeriousHurtReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getPlayerHurtThreshold() {
        lock.readLock().lock();
        try {
            if (playerHurtThreshold < 0) {
                return 0;
            }
            if (playerHurtThreshold > 1) {
                return 1;
            }
            return playerHurtThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPlayerHurtThreshold(double playerHurtThreshold) {
        lock.writeLock().lock();
        try {
            if (playerHurtThreshold < 0) {
                this.playerHurtThreshold = 0;
            } else if (playerHurtThreshold > 1) {
                this.playerHurtThreshold = 1;
            } else {
                this.playerHurtThreshold = playerHurtThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getTravelMessageLocation() {
        lock.readLock().lock();
        try {
            return travelMessageLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTravelMessageLocation(MessageLocation travelMessageLocation) {
        lock.writeLock().lock();
        try {
            this.travelMessageLocation = travelMessageLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getTravelMessageReceiver() {
        lock.readLock().lock();
        try {
            return travelMessageReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTravelMessageReceiver(MessageReceiver travelMessageReceiver) {
        lock.writeLock().lock();
        try {
            this.travelMessageReceiver = travelMessageReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getTravelWindowTicks() {
        lock.readLock().lock();
        try {
            if (travelWindowTicks <= 0) {
                return 1;
            }
            return travelWindowTicks;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTravelWindowTicks(int travelWindowTicks) {
        lock.writeLock().lock();
        try {
            if (travelWindowTicks <= 0) {
                this.travelWindowTicks = 1;
            } else {
                this.travelWindowTicks = travelWindowTicks;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getTravelTotalDistanceThreshold() {
        lock.readLock().lock();
        try {
            if (travelTotalDistanceThreshold < 0) {
                return 0;
            }
            return travelTotalDistanceThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTravelTotalDistanceThreshold(double travelTotalDistanceThreshold) {
        lock.writeLock().lock();
        try {
            if (travelTotalDistanceThreshold < 0) {
                this.travelTotalDistanceThreshold = 0;
            } else {
                this.travelTotalDistanceThreshold = travelTotalDistanceThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getTravelPartialInterval() {
        lock.readLock().lock();
        try {
            if (travelPartialInterval <= 0) {
                return 1;
            }
            return travelPartialInterval;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTravelPartialInterval(int travelPartialInterval) {
        lock.writeLock().lock();
        try {
            if (travelPartialInterval <= 0) {
                this.travelPartialInterval = 1;
            } else {
                this.travelPartialInterval = travelPartialInterval;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getTravelPartialDistanceThreshold() {
        lock.readLock().lock();
        try {
            if (travelPartialDistanceThreshold < 0) {
                return 0;
            }
            return travelPartialDistanceThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTravelPartialDistanceThreshold(double travelPartialDistanceThreshold) {
        lock.writeLock().lock();
        try {
            if (travelPartialDistanceThreshold < 0) {
                this.travelPartialDistanceThreshold = 0;
            } else {
                this.travelPartialDistanceThreshold = travelPartialDistanceThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getTeleportThreshold() {
        lock.readLock().lock();
        try {
            if (teleportThreshold < 0) {
                return 0;
            }
            return teleportThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTeleportThreshold(double teleportThreshold) {
        lock.writeLock().lock();
        try {
            if (teleportThreshold < 0) {
                this.teleportThreshold = 0;
            } else {
                this.teleportThreshold = teleportThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageLocation getTeleportMessageLocation() {
        lock.readLock().lock();
        try {
            return teleportMessageLocation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTeleportMessageLocation(MessageLocation teleportMessageLocation) {
        lock.writeLock().lock();
        try {
            this.teleportMessageLocation = teleportMessageLocation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getTeleportMessageReceiver() {
        lock.readLock().lock();
        try {
            return teleportMessageReceiver;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTeleportMessageReceiver(MessageReceiver teleportMessageReceiver) {
        lock.writeLock().lock();
        try {
            this.teleportMessageReceiver = teleportMessageReceiver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getGptUrl() {
        lock.readLock().lock();
        try {
            return gptUrl;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setGptUrl(String gptUrl) {
        lock.writeLock().lock();
        try {
            this.gptUrl = gptUrl;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getGptAccessTokens() {
        lock.readLock().lock();
        try {
            return gptAccessTokens;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Retrieves a secure version of the GPT access token.
     * The secure token masks the middle part of the original token with asterisks for security purposes.
     * 
     * @return A string representing the secure version of the GPT access token. 
     *         If the token length is greater than 20, the first 5 and last 5 characters are visible, 
     *         with the middle characters replaced by asterisks. 
     *         If the token length is between 1 and 20, the entire token is replaced by asterisks. 
     *         If the token is empty, "null" is returned.
     */
    public String getSecureGptAccessTokens() {
        String token = "";
        lock.readLock().lock();
        try {
            token = gptAccessTokens;
        } finally {
            lock.readLock().unlock();
        }
        String secureToken = "";
        if (token.length() > 20) {
            secureToken = token.substring(0, 5) + String.valueOf("*".repeat(token.length() - 10)) + token.substring(token.length() - 5);
        } else if (token.length() > 0) {
            secureToken = String.valueOf("*".repeat(token.length()));
        } else {
            secureToken = "null";
        }
        return secureToken;
    }

    public void setGptAccessTokens(String gptAccessTokens) {
        lock.writeLock().lock();
        try {
            this.gptAccessTokens = gptAccessTokens;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getGptModel() {
        lock.readLock().lock();
        try {
            return gptModel;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setGptModel(String gptModel) {
        lock.writeLock().lock();
        try {
            this.gptModel = gptModel;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getGptSystemPrompt() {
        lock.readLock().lock();
        try {
            return gptSystemPrompt;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setGptSystemPrompt(String gptSystemPrompt) {
        lock.writeLock().lock();
        try {
            this.gptSystemPrompt = gptSystemPrompt;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getGptTemperature() {
        lock.readLock().lock();
        try {
            if (gptTemperature < 0) {
                return 0;
            }
            if (gptTemperature > 1) {
                return 1;
            }
            return gptTemperature;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setGptTemperature(double gptTemprature) {
        lock.writeLock().lock();
        try {
            if (gptTemprature < 0) {
                this.gptTemperature = 0;
            } else if (gptTemprature > 1) {
                this.gptTemperature = 1;
            } else {
                this.gptTemperature = gptTemprature;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getGptServerTimeout() {
        lock.readLock().lock();
        try {
            if (gptServerTimeout < 0) {
                return 0;
            }
            return gptServerTimeout;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setGptServerTimeout(int gptServerTimeout) {
        lock.writeLock().lock();
        try {
            if (gptServerTimeout < 0) {
                this.gptServerTimeout = 0;
            } else {
                this.gptServerTimeout = gptServerTimeout;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
