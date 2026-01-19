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
     * Default: false
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
     * The message sent to the client when an entity dies.
     * Default: NONE
     */
    protected MessageLocation entityDeathMessage;

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
     * Controls who can receive the coordinates when a player dies.
     * Default: NONE
     */
    protected MessageReceiver playerDeathCoord;

    /**
     * Controls who can receive the message when a projectile thrown by a player hits another entity.
     * Default: NONE
     */
    protected MessageReceiver projectileHitOthers;

    /**
     * Controls who can receive the message when a projectile thrown by another entity hits the player.
     * Default: NONE
     */
    protected MessageReceiver projectileBeingHit;

    /**
     * Controls who can receive the message when a player is suspected of being AFK.
     * This message will show in the action bar, it will not disappear until the player comes back.
     * Default: NONE
     */
    protected MessageReceiver informAfking;

    /**
     * The threshold of the time in ticks that a player is suspected of being AFK.
     * Default: 1200 Ticks (1 minute)
     */
    protected int informAfkingThreshold;

    /**
     * Controls who can receive the message when a player is confirmed to be AFK.
     * This message will show in the chat, it will be sent only once.
     * Default: NONE
     */
    protected MessageReceiver broadcastAfking;

    /**
     * The threshold of the time in ticks that a player is confirmed to be AFK.
     * Default: 6000 Ticks (5 minutes)
     */
    protected int broadcastAfkingThreshold;

    /**
     * Controls who can receive the message when a player is back from AFK.
     * This message will show in the chat, it will be sent only once.
     * Default: NONE
     */
    protected MessageReceiver stopAfking;

    /**
     * Controls who can receive the message when a player changes the biome.
     * Default: NONE
     */
    protected MessageReceiver changeBiome;

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
    protected MessageLocation bossFightLoc;

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
    protected MessageLocation monsterSurroundLoc;

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
     * Controls who can receive the message when a player is seriously hurt.
     * Default: NONE
     */
    protected MessageReceiver playerSeriousHurt;

    /**
     * If a player receives a damage larger than this percentage of his max health, he will be considered as seriously hurt.
     * Default: 0.8 (80%)
     */
    protected double playerHurtThreshold;

    /**
     * Controls where to show the message when a player travels a long distance in a short time.
     * Default: NONE
     */
    protected MessageLocation travelMessageLoc;

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
    protected MessageLocation teleportMessageLoc;

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
        this.serverTranslation = false;
        this.maxFlowLength = 32767;
        this.keepFlowHistoryNumber = 32767;
        this.entityDeathMessage = MessageLocation.NONE;
        this.bossDeathMessage = MessageLocation.NONE;
        this.namedEntityDeathMessage = MessageLocation.NONE;
        this.playerCanSleepMessage = MessageLocation.NONE;
        this.killerDeathMessage = MessageLocation.NONE;
        this.bossMaxHpThreshold = 150;
        this.playerDeathCoord = MessageReceiver.NONE;
        this.projectileHitOthers = MessageReceiver.NONE;
        this.projectileBeingHit = MessageReceiver.NONE;
        this.informAfking = MessageReceiver.NONE;
        this.informAfkingThreshold = 1200;
        this.broadcastAfking = MessageReceiver.NONE;
        this.broadcastAfkingThreshold = 6000;
        this.stopAfking = MessageReceiver.NONE;
        this.changeBiome = MessageReceiver.NONE;
        this.changeBiomeDelay = 200;
        this.bossFightLoc = MessageLocation.NONE;
        this.bossFightReceiver = MessageReceiver.NONE;
        this.bossFightInterval = 1200;
        this.monsterSurroundLoc = MessageLocation.NONE;
        this.monsterSurroundReceiver = MessageReceiver.NONE;
        this.monsterSurroundInterval = 1200;
        this.monsterNumberThreshold = 8;
        this.monsterDistanceThreshold = 12.0;
        this.entityNumberWarning = MessageLocation.NONE;
        this.entityNumberThreshold = 3000;
        this.entityNumberInterval = 20;
        this.playerSeriousHurt = MessageReceiver.NONE;
        this.playerHurtThreshold = 0.8;
        this.travelMessageLoc = MessageLocation.NONE;
        this.travelMessageReceiver = MessageReceiver.NONE;
        this.travelWindowTicks = 600;
        this.travelTotalDistanceThreshold = 100.0;
        this.travelPartialInterval = 200;
        this.travelPartialDistanceThreshold = 40.0;
        this.teleportThreshold = 75.0;
        this.teleportMessageLoc = MessageLocation.NONE;
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

    public MessageReceiver getPlayerDeathCoord() {
        lock.readLock().lock();
        try {
            return playerDeathCoord;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPlayerDeathCoord(MessageReceiver playerDeathCoord) {
        lock.writeLock().lock();
        try {
            this.playerDeathCoord = playerDeathCoord;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getProjectileHitOthers() {
        lock.readLock().lock();
        try {
            return projectileHitOthers;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setProjectileHitOthers(MessageReceiver projectileHitOthers) {
        lock.writeLock().lock();
        try {
            this.projectileHitOthers = projectileHitOthers;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getProjectileBeingHit() {
        lock.readLock().lock();
        try {
            return projectileBeingHit;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setProjectileBeingHit(MessageReceiver projectileBeingHit) {
        lock.writeLock().lock();
        try {
            this.projectileBeingHit = projectileBeingHit;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getInformAfking() {
        lock.readLock().lock();
        try {
            return informAfking;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setInformAfking(MessageReceiver informAfking) {
        lock.writeLock().lock();
        try {
            this.informAfking = informAfking;
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

    public MessageReceiver getBroadcastAfking() {
        lock.readLock().lock();
        try {
            return broadcastAfking;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBroadcastAfking(MessageReceiver broadcastAfking) {
        lock.writeLock().lock();
        try {
            this.broadcastAfking = broadcastAfking;
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

    public MessageReceiver getStopAfking() {
        lock.readLock().lock();
        try {
            return stopAfking;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setStopAfking(MessageReceiver stopAfking) {
        lock.writeLock().lock();
        try {
            this.stopAfking = stopAfking;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MessageReceiver getChangeBiome() {
        lock.readLock().lock();
        try {
            return changeBiome;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setChangeBiome(MessageReceiver changeBiome) {
        lock.writeLock().lock();
        try {
            this.changeBiome = changeBiome;
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
            return bossFightLoc;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBossFightMessageLocation(MessageLocation bossFightType) {
        lock.writeLock().lock();
        try {
            this.bossFightLoc = bossFightType;
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

    public void setBossFightMessageReceiver(MessageReceiver bossFightMethod) {
        lock.writeLock().lock();
        try {
            this.bossFightReceiver = bossFightMethod;
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
            return monsterSurroundLoc;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMonsterSurroundMessageLocation(MessageLocation monsterSurroundType) {
        lock.writeLock().lock();
        try {
            this.monsterSurroundLoc = monsterSurroundType;
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

    public void setMonsterSurroundMessageReceiver(MessageReceiver monsterSurroundMethod) {
        lock.writeLock().lock();
        try {
            this.monsterSurroundReceiver = monsterSurroundMethod;
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

    public MessageReceiver getPlayerSeriousHurt() {
        lock.readLock().lock();
        try {
            return playerSeriousHurt;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPlayerSeriousHurt(MessageReceiver playerSeriousHurt) {
        lock.writeLock().lock();
        try {
            this.playerSeriousHurt = playerSeriousHurt;
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

    public MessageLocation getTravelMessageLoc() {
        lock.readLock().lock();
        try {
            return travelMessageLoc;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTravelMessageLoc(MessageLocation travelMessageLocation) {
        lock.writeLock().lock();
        try {
            this.travelMessageLoc = travelMessageLocation;
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
            return teleportMessageLoc;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTeleportMessageLocation(MessageLocation teleportMessageLoc) {
        lock.writeLock().lock();
        try {
            this.teleportMessageLoc = teleportMessageLoc;
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
