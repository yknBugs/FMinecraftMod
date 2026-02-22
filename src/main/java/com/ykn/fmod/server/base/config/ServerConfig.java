/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.config;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ykn.fmod.server.base.util.PlayerMessageType;
import com.ykn.fmod.server.base.util.ServerMessageType;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ServerConfig extends ConfigReader {

    // Multiple threads may access the config class at the same time, the getters and setters in this class must be locked to ensure thread safety.
    protected final transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * If enabled, the server will translate all the messages and then send to the client.
     * This is useful if only the server has this mod installed and the client does not have it.
     * But if enabled, the message will not follow the client's language setting.
     * Default: true
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.BOOLEAN,
        codeEntry = "serverTranslation",
        commandEntry = "serverTranslation",
        i18nEntry = "translate",
        displayValueGetter = "getBooleanValueI18n",
        commandValueHint = "enable"
    )
    protected boolean serverTranslation;

    /**
     * The maximum number of nodes that can be executed in a single flow execution.
     * This is designed to prevent infinite loops in flow executions.
     * Default: 32767
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "maxFlowLength",
        commandEntry = "maxFlowLength",
        i18nEntry = "flowlength",
        minCommandInt = 0,
        commandValueHint = "length",
        toSliderValue = "logScaleToSlider",
        fromSliderValue = "logScalefromSlider"
    )
    protected int maxFlowLength;

    /**
     * The maximum depth of flow recursion.
     * If a flow calls another flow, the depth of recursion will increase by 1.
     * If the depth of recursion exceeds this value, the flow execution will be stopped.
     * This is designed to prevent StackOverflowError caused by infinite flow recursion.
     * Default: 16
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "maxFlowRecursionDepth",
        commandEntry = "maxFlowRecursionDepth",
        i18nEntry = "flowrecursion",
        minCommandInt = 0,
        minSliderInt = 0,
        maxSliderInt = 256,
        commandValueHint = "depth"
    )
    protected int maxFlowRecursionDepth;

    /**
     * How many history records of flow executions to keep.
     * Default: 32767
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "maxFlowHistorySize",
        commandEntry = "maxFlowHistorySize",
        i18nEntry = "flowhistory",
        minCommandInt = 0,
        commandValueHint = "size",
        toSliderValue = "logScaleToSlider",
        fromSliderValue = "logScalefromSlider"
    )
    protected int maxFlowHistorySize;

    /**
     * The message sent to the client when a non-hostile and non-passive entity dies.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.SERVERMESSAGE,
        codeEntry = "entityDeathMessage",
        commandEntry = "entityDeathMessage",
        i18nEntry = "entitydeath"
    )
    protected ServerMessageType entityDeathMessage;

    /**
     * The message sent to the client when a hostile entity dies.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.SERVERMESSAGE,
        codeEntry = "hostileDeathMessage",
        commandEntry = "hostileDeathMessage",
        i18nEntry = "hostiledeath"
    )
    protected ServerMessageType hostileDeathMessage;

    /**
     * The message sent to the client when a passive entity dies.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.SERVERMESSAGE,
        codeEntry = "passiveDeathMessage",
        commandEntry = "passiveDeathMessage",
        i18nEntry = "passivedeath"
    )
    protected ServerMessageType passiveDeathMessage;

    /**
     * The message sent to the client when a boss dies.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.SERVERMESSAGE,
        codeEntry = "bossDeathMessage",
        commandEntry = "bossDeathMessage",
        i18nEntry = "bossdeath"
    )
    protected ServerMessageType bossDeathMessage;

    /**
     * The message sent to the client when a mob with custom name dies.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.SERVERMESSAGE,
        codeEntry = "namedEntityDeathMessage",
        commandEntry = "namedEntityDeathMessage",
        i18nEntry = "nameddeath"
    )
    protected ServerMessageType namedEntityDeathMessage;

    /**
     * The message sent to the client when a mob - the mod that has once killed a player before - dies.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.SERVERMESSAGE,
        codeEntry = "killerDeathMessage",
        commandEntry = "killerDeathMessage",
        i18nEntry = "killerdeath"
    )
    protected ServerMessageType killerDeathMessage;

    /**
     * If an entity has a health greater than this value, it will be considered as a boss.
     * Default: 150
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.DOUBLE,
        codeEntry = "bossMaxHealthThreshold",
        commandEntry = "bossMaxHealthThreshold",
        i18nEntry = "bosshealth",
        minCommandDouble = 0.0,
        minSliderDouble = 0.0,
        maxSliderDouble = 1000.0,
        displayValueGetter = "getDoubleConfigDisplayText",
        commandValueHint = "health"
    )
    protected double bossMaxHealthThreshold;

    /**
     * The message sent to the player when the player changes the status of can sleep or cannot sleep.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "playerCanSleepMessage",
        commandEntry = "playerCanSleepMessage",
        i18nEntry = "cansleep"
    )
    protected PlayerMessageType playerCanSleepMessage;

    /**
     * The message sent to the player when the player dies, showing the coordinates of the death location.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "playerDeathCoord",
        commandEntry = "playerDeathCoord",
        i18nEntry = "deathcoord"
    )
    protected PlayerMessageType playerDeathCoord;

    /**
     * The message sent to the player when a projectile thrown by the player hits another entity.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "projectileHitOthers",
        commandEntry = "projectileHitOthers",
        i18nEntry = "hitothers"
    )
    protected PlayerMessageType projectileHitOthers;

    /**
     * The message sent to the player when the player is hit by a projectile.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "projectileBeingHit",
        commandEntry = "projectileBeingHit",
        i18nEntry = "beinghit"
    )
    protected PlayerMessageType projectileBeingHit;

    /**
     * The message sent to the player when the player is suspected of being AFK.
     * This message will not disappear until the player comes back.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "informAfk",
        commandEntry = "informAfk",
        i18nEntry = "informafk"
    )
    protected PlayerMessageType informAfk;

    /**
     * The threshold of the time in ticks that a player is suspected of being AFK.
     * Default: 1200 Ticks (1 minute)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "informAfkThreshold",
        commandEntry = "informAfkThreshold",
        i18nEntry = "afkthres",
        minCommandInt = 1,
        minSliderInt = 1,
        maxSliderInt = 6000,
        displayValueGetter = "getTickConfigDisplayText",
        commandValueHint = "seconds",
        commandInputToTrueValue = "commandInputSecondToTick"
    )
    protected int informAfkThreshold;

    /**
     * The message sent to the player when they are confirmed to be AFK.
     * This message will be sent only once.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "broadcastAfk",
        commandEntry = "broadcastAfk",
        i18nEntry = "bcafk"
    )
    protected PlayerMessageType broadcastAfk;

    /**
     * The threshold of the time in ticks that a player is confirmed to be AFK.
     * Default: 6000 Ticks (5 minutes)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "broadcastAfkThreshold",
        commandEntry = "broadcastAfkThreshold",
        i18nEntry = "bcafkthres",
        minCommandInt = 1,
        minSliderInt = 1,
        maxSliderInt = 36000,
        displayValueGetter = "getTickConfigDisplayText",
        commandValueHint = "seconds",
        commandInputToTrueValue = "commandInputSecondToTick"
    )
    protected int broadcastAfkThreshold;

    /**
     * The message sent to the player when they are back from AFK.
     * This message will be sent only once.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "stopAfk",
        commandEntry = "stopAfk",
        i18nEntry = "stopafk"
    )
    protected PlayerMessageType stopAfk;

    /**
     * The message sent to the player when a player changes the biome.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "changeBiomeMessage",
        commandEntry = "changeBiomeMessage",
        i18nEntry = "changebiome"
    )
    protected PlayerMessageType changeBiomeMessage;

    /**
     * The delay in ticks before sending the message when a player changes the biome.
     * This is designed to avoid spamming when a player frequently crosses the boundary of two biomes.
     * Default: 200 Ticks (10 seconds)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "changeBiomeDelay",
        commandEntry = "changeBiomeDelay",
        i18nEntry = "biomedelay",
        minCommandInt = 0,
        minSliderInt = 0,
        maxSliderInt = 1200,
        displayValueGetter = "getTickConfigDisplayText",
        commandValueHint = "seconds",
        commandInputToTrueValue = "commandInputSecondToTick"
    )
    protected int changeBiomeDelay;

    /**
     * The message sent to the player when a player attacks a boss.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "bossFightMessage",
        commandEntry = "bossFightMessage",
        i18nEntry = "bossfight"
    )
    protected PlayerMessageType bossFightMessage;

    /**
     * The interval in ticks before sending the message when a player attacks a boss.
     * This is designed to avoid spamming messages
     * Default: 1200 Ticks (60 seconds)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "bossFightInterval",
        commandEntry = "bossFightInterval",
        i18nEntry = "bossfightinterval",
        minCommandInt = 0,
        minSliderInt = 0,
        maxSliderInt = 3600,
        displayValueGetter = "getTickConfigDisplayText",
        commandValueHint = "seconds",
        commandInputToTrueValue = "commandInputSecondToTick"
    )
    protected int bossFightInterval;

    /**
     * The message sent to the player when a player is surrounded by monsters.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "monsterSurroundMessage",
        commandEntry = "monsterSurroundMessage",
        i18nEntry = "monstersurround"
    )
    protected PlayerMessageType monsterSurroundMessage;

    /**
     * The interval in ticks before sending the message when a player is surrounded by monsters.
     * This is designed to avoid spamming messages
     * Default: 1200 Ticks (60 seconds)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "monsterSurroundInterval",
        commandEntry = "monsterSurroundInterval",
        i18nEntry = "monsterinterval",
        minCommandInt = 0,
        minSliderInt = 0,
        maxSliderInt = 3600,
        displayValueGetter = "getTickConfigDisplayText",
        commandValueHint = "seconds",
        commandInputToTrueValue = "commandInputSecondToTick"
    )
    protected int monsterSurroundInterval;

    /**
     * If the number of the monsters near a player is larger than this value, the player will be considered as surrounded by monsters.
     * Default: 8
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "monsterNumberThreshold",
        commandEntry = "monsterNumberThreshold",
        i18nEntry = "monsternumber",
        minCommandInt = 1,
        minSliderInt = 1,
        maxSliderInt = 100,
        commandValueHint = "number"
    )
    protected int monsterNumberThreshold;

    /**
     * If the distance between a player and a monster is less than this value, the monster will be considered as near the player.
     * Default: 12
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.DOUBLE,
        codeEntry = "monsterDistanceThreshold",
        commandEntry = "monsterDistanceThreshold",
        i18nEntry = "monsterdistance",
        minCommandDouble = 0.0,
        minSliderDouble = 0.0,
        maxSliderDouble = 128.0,
        displayValueGetter = "getDoubleConfigDisplayText",
        commandValueHint = "meters"
    )
    protected double monsterDistanceThreshold;

    /**
     * The message send to players when the number of entities in the server is larger than the threshold.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.SERVERMESSAGE,
        codeEntry = "entityNumberWarning",
        commandEntry = "entityNumberWarning",
        i18nEntry = "entitywarning"
    )
    protected ServerMessageType entityNumberWarning;

    /**
     * The message to be sent when the entity density anaylze result is available.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.SERVERMESSAGE,
        codeEntry = "entityDensityWarning",
        commandEntry = "entityDensityWarning",
        i18nEntry = "densitywarning"
    )
    protected ServerMessageType entityDensityWarning;

    /**
     * If the number of the entities in the server is larger than this value, the warning message will be sent.
     * This is designed to avoid too many entities lagging the server.
     * Default: 3000
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "entityNumberThreshold",
        commandEntry = "entityNumberThreshold",
        i18nEntry = "entitynumber",
        minCommandInt = 1,
        minSliderInt = 1,
        maxSliderInt = 10000,
        commandValueHint = "number"
    )
    protected int entityNumberThreshold;

    /**
     * If the number of the entities in the server is larger than this value,
     * we will automatically perform entity density analysis to find the most crowded area.
     * Default: 3000 
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "entityDensityThreshold",
        commandEntry = "entityDensityThreshold",
        i18nEntry = "entitydensity",
        minCommandInt = 1,
        minSliderInt = 1,
        maxSliderInt = 10000,
        commandValueHint = "number"
    )
    protected int entityDensityThreshold;

    /**
     * When anaylzing entity density, the minimum number of entities required for each candidate position.
     * Default: 100
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "entityDensityNumber",
        commandEntry = "entityDensityNumber",
        i18nEntry = "densitynumber",
        minCommandInt = 2,
        minSliderInt = 2,
        maxSliderInt = 1000,
        commandValueHint = "number"
    )
    protected int entityDensityNumber;

    /**
     * The radius within which to count entities for density checks.
     * Default: 8.0
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.DOUBLE,
        codeEntry = "entityDensityRadius",
        commandEntry = "entityDensityRadius",
        i18nEntry = "densityradius",
        minCommandDouble = 0.0,
        minSliderDouble = 0.0,
        maxSliderDouble = 128.0,
        displayValueGetter = "getDoubleConfigDisplayText",
        commandValueHint = "meters"
    )
    protected double entityDensityRadius;

    /**
     * The interval in ticks to check how many entities are there in the server.
     * Frequently checking the number of entities may cause lag.
     * Default: 20 (1 second)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "entityNumberInterval",
        commandEntry = "entityNumberInterval",
        i18nEntry = "entityinterval",
        minCommandInt = 1,
        minSliderInt = 1,
        maxSliderInt = 1200,
        displayValueGetter = "getTickConfigDisplayText",
        commandValueHint = "seconds",
        commandInputToTrueValue = "commandInputSecondToTick"
    )
    protected int entityNumberInterval;

    /**
     * The interval in ticks to perform entity density analysis.
     * Frequently performing entity density analysis may cause lag.
     * Default: 20 (1 second)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "entityDensityInterval",
        commandEntry = "entityDensityInterval",
        i18nEntry = "densityinterval",
        minCommandInt = 1,
        minSliderInt = 1,
        maxSliderInt = 1200,
        displayValueGetter = "getTickConfigDisplayText",
        commandValueHint = "seconds",
        commandInputToTrueValue = "commandInputSecondToTick"
    )
    protected int entityDensityInterval;

    /**
     * The message sent to the player when a player receives a damage larger than a certain percentage of his max health.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "playerHurtMessage",
        commandEntry = "playerHurtMessage",
        i18nEntry = "playerhurt"
    )
    protected PlayerMessageType playerHurtMessage;

    /**
     * If a player receives a damage larger than this percentage of his max health, he will be considered as seriously hurt.
     * Default: 0.8 (80%)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.DOUBLE,
        codeEntry = "playerHurtThreshold",
        commandEntry = "playerHurtThreshold",
        i18nEntry = "damagethres",
        minCommandDouble = 0.0,
        maxCommandDouble = 1.0,
        minSliderDouble = 0.0,
        maxSliderDouble = 1.0,
        displayValueGetter = "getDoubleConfigDisplayText",
        commandValueHint = "percentage"
    )
    protected double playerHurtThreshold;

    /**
     * The message sent to the player when a player travels a long distance within a short time.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "travelMessage",
        commandEntry = "travelMessage",
        i18nEntry = "travelmessage"
    )
    protected PlayerMessageType travelMessage;

    /**
     * Controls how often the player can receive the message.
     * Default: 20 Ticks (1 second)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "travelMessageInterval",
        commandEntry = "travelMessageInterval",
        i18nEntry = "travelinterval",
        minCommandInt = 1,
        minSliderInt = 1,
        maxSliderInt = 1200,
        displayValueGetter = "getTickConfigDisplayText",
        commandValueHint = "seconds",
        commandInputToTrueValue = "commandInputSecondToTick"
    )
    protected int travelMessageInterval;

    /**
     * How many recent ticks to track for long-distance travel detection.
     * Default: 600 ticks (30 seconds)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "travelWindow",
        commandEntry = "travelWindow",
        i18nEntry = "travelwindow",
        minCommandInt = 1,
        minSliderInt = 1,
        maxSliderInt = 12000,
        displayValueGetter = "getTickConfigDisplayText",
        commandValueHint = "seconds",
        commandInputToTrueValue = "commandInputSecondToTick"
    )
    protected int travelWindow;

    /**
     * The total horizontal distance required within the tracked window to consider it long-distance travel.
     * Default: 100 blocks
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.DOUBLE,
        codeEntry = "travelTotalDistanceThreshold",
        commandEntry = "travelTotalDistanceThreshold",
        i18nEntry = "traveltotal",
        minCommandDouble = 0.0,
        minSliderDouble = 0.0,
        maxSliderDouble = 1000.0,
        displayValueGetter = "getDoubleConfigDisplayText",
        commandValueHint = "meters"
    )
    protected double travelTotalDistanceThreshold;

    /**
     * Interval in ticks for partial distance checks within the travel window.
     * Default: 200 ticks (10 seconds)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "travelPartialInterval",
        commandEntry = "travelPartialInterval",
        i18nEntry = "travelcheck",
        minCommandInt = 1,
        minSliderInt = 1,
        maxSliderInt = 12000,
        displayValueGetter = "getTickConfigDisplayText",
        commandValueHint = "seconds",
        commandInputToTrueValue = "commandInputSecondToTick"
    )
    protected int travelPartialInterval;

    /**
     * The minimum horizontal distance required between two positions separated by the partial interval.
     * Default: 40 blocks
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.DOUBLE,
        codeEntry = "travelPartialDistanceThreshold",
        commandEntry = "travelPartialDistanceThreshold",
        i18nEntry = "travelpartial",
        minCommandDouble = 0.0,
        minSliderDouble = 0.0,
        maxSliderDouble = 1000.0,
        displayValueGetter = "getDoubleConfigDisplayText",
        commandValueHint = "meters"
    )
    protected double travelPartialDistanceThreshold;

    /**
     * The maximum allowed single-tick horizontal distance before it is considered a teleport.
     * Default: 75 blocks
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.DOUBLE,
        codeEntry = "teleportThreshold",
        commandEntry = "teleportThreshold",
        i18nEntry = "teleportthres",
        minCommandDouble = 0.0,
        minSliderDouble = 0.0,
        maxSliderDouble = 256.0,
        displayValueGetter = "getDoubleConfigDisplayText",
        commandValueHint = "meters"
    )
    protected double teleportThreshold;

    /**
     * Controls where to show the message when a player teleports.
     * Default: NONE
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.PLAYERMESSAGE,
        codeEntry = "teleportMessage",
        commandEntry = "teleportMessage",
        i18nEntry = "teleport"
    )
    protected PlayerMessageType teleportMessage;

    /**
     * The URL of the target GPT server.
     * This mod will use the OpenAI API.
     * So if you want to deploy a local LLM, you must make sure it is compatible with the OpenAI API.
     * Default: A server created by llama-server --host 0.0.0.0 --port 12345 (Search the llama.cpp project on github for more information)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.STRING,
        codeEntry = "gptUrl",
        commandEntry = "gptUrl",
        i18nEntry = "gpturl",
        maxStringLength = 1024,
        commandValueHint = "url"
    )
    protected String gptUrl;

    /**
     * The access tokens of the GPT server.
     * If the sever does not need an access token, set it to an empty string, but not null.
     * Default: ""
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.STRING,
        codeEntry = "gptAccessTokens",
        commandEntry = "gptAccessTokens",
        i18nEntry = "gptkey",
        maxStringLength = 1024,
        isEditableInUI = false,
        notEditableReason = "fmod.options.gptkey.noedit",
        displayValueGetter = "getSecureGptAccessTokens",
        commandValueHint = "token"
    )
    protected String gptAccessTokens;

    /**
     * The model of the GPT server.
     * Default: ""
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.STRING,
        codeEntry = "gptModel",
        commandEntry = "gptModel",
        i18nEntry = "gptmodel",
        maxStringLength = 1024,
        commandValueHint = "model"
    )
    protected String gptModel;

    /**
     * The system prompt of generating the response.
     * Default: ""
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.STRING,
        codeEntry = "gptSystemPrompt",
        commandEntry = "gptSystemPrompt",
        i18nEntry = "gptprompt",
        maxStringLength = 4096,
        commandValueHint = "prompt"
    )
    protected String gptSystemPrompt;

    /**
     * The temperature parameter of generating the response.
     * Default: 0.8
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.DOUBLE,
        codeEntry = "gptTemperature",
        commandEntry = "gptTemperature",
        i18nEntry = "gpttemperature",
        minCommandDouble = 0.0,
        minSliderDouble = 0.0,
        maxSliderDouble = 2.0,
        displayValueGetter = "getDoubleConfigDisplayText",
        commandValueHint = "temperature"
    )
    protected double gptTemperature;

    /**
     * The timeout of the GPT server in milliseconds.
     * Default: 60000 (60 seconds)
     */
    @ConfigEntry(
        type = ConfigEntry.ConfigType.INTEGER,
        codeEntry = "gptServerTimeout",
        commandEntry = "gptServerTimeout",
        i18nEntry = "gpttimeout",
        minCommandInt = 1,
        minSliderInt = 1000,
        maxSliderInt = 180000,
        displayValueGetter = "getMiniSecondsDisplayText",
        commandValueHint = "seconds",
        commandInputToTrueValue = "commandInputSecondToMiniSecond"
    )
    protected int gptServerTimeout;

    public ServerConfig() {
        super("server.json");
        this.serverTranslation = true;
        this.maxFlowLength = 32767;
        this.maxFlowRecursionDepth = 16;
        this.maxFlowHistorySize = 32767;
        this.entityDeathMessage = ServerMessageType.empty();
        this.hostileDeathMessage = ServerMessageType.empty();
        this.passiveDeathMessage = ServerMessageType.empty();
        this.bossDeathMessage = ServerMessageType.empty();
        this.namedEntityDeathMessage = ServerMessageType.empty();
        this.killerDeathMessage = ServerMessageType.empty();
        this.bossMaxHealthThreshold = 150;
        this.playerCanSleepMessage = PlayerMessageType.empty();
        this.playerDeathCoord = PlayerMessageType.empty();
        this.projectileHitOthers = PlayerMessageType.empty();
        this.projectileBeingHit = PlayerMessageType.empty();
        this.informAfk = PlayerMessageType.empty();
        this.informAfkThreshold = 1200;
        this.broadcastAfk = PlayerMessageType.empty();
        this.broadcastAfkThreshold = 6000;
        this.stopAfk = PlayerMessageType.empty();
        this.changeBiomeMessage = PlayerMessageType.empty();
        this.changeBiomeDelay = 200;
        this.bossFightMessage = PlayerMessageType.empty();
        this.bossFightInterval = 1200;
        this.monsterSurroundMessage = PlayerMessageType.empty();
        this.monsterSurroundInterval = 1200;
        this.monsterNumberThreshold = 8;
        this.monsterDistanceThreshold = 12.0;
        this.entityNumberWarning = ServerMessageType.empty();
        this.entityDensityWarning = ServerMessageType.empty();
        this.entityNumberThreshold = 3000;
        this.entityDensityThreshold = 3000;
        this.entityDensityNumber = 100;
        this.entityDensityRadius = 8.0;
        this.entityNumberInterval = 20;
        this.entityDensityInterval = 20;
        this.playerHurtMessage = PlayerMessageType.empty();
        this.playerHurtThreshold = 0.8;
        this.travelMessage = PlayerMessageType.empty();
        this.travelMessageInterval = 20;
        this.travelWindow = 600;
        this.travelTotalDistanceThreshold = 100.0;
        this.travelPartialInterval = 200;
        this.travelPartialDistanceThreshold = 40.0;
        this.teleportThreshold = 75.0;
        this.teleportMessage = PlayerMessageType.empty();
        this.gptUrl = "http://127.0.0.1:12345/v1/chat/completions";
        this.gptAccessTokens = "";
        this.gptModel = "";
        this.gptSystemPrompt = "";
        this.gptTemperature = 0.8;
        this.gptServerTimeout = 60000;
    }

    public boolean getServerTranslation() {
        lock.readLock().lock();
        try {
            return serverTranslation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setServerTranslation(boolean serverTranslation) {
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
            if (maxFlowLength < 0) {
                return 0;
            }
            return maxFlowLength;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMaxFlowLength(int maxFlowLength) {
        lock.writeLock().lock();
        try {
            if (maxFlowLength < 0) {
                this.maxFlowLength = 0;
            } else {
                this.maxFlowLength = maxFlowLength;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getMaxFlowRecursionDepth() {
        lock.readLock().lock();
        try {
            if (maxFlowRecursionDepth < 0) {
                return 0;
            }
            return maxFlowRecursionDepth;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMaxFlowRecursionDepth(int maxFlowRecursionDepth) {
        lock.writeLock().lock();
        try {
            if (maxFlowRecursionDepth < 0) {
                this.maxFlowRecursionDepth = 0;
            } else {
                this.maxFlowRecursionDepth = maxFlowRecursionDepth;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getMaxFlowHistorySize() {
        lock.readLock().lock();
        try {
            if (maxFlowHistorySize < 0) {
                return 0;
            }
            return maxFlowHistorySize;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMaxFlowHistorySize(int maxFlowHistorySize) {
        lock.writeLock().lock();
        try {
            if (maxFlowHistorySize < 0) {
                this.maxFlowHistorySize = 0;
            } else {
                this.maxFlowHistorySize = maxFlowHistorySize;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ServerMessageType getEntityDeathMessage() {
        lock.readLock().lock();
        try {
            return entityDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityDeathMessage(ServerMessageType entityDeathMessage) {
        lock.writeLock().lock();
        try {
            this.entityDeathMessage = entityDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ServerMessageType getHostileDeathMessage() {
        lock.readLock().lock();
        try {
            return hostileDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setHostileDeathMessage(ServerMessageType hostileDeathMessage) {
        lock.writeLock().lock();
        try {
            this.hostileDeathMessage = hostileDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ServerMessageType getPassiveDeathMessage() {
        lock.readLock().lock();
        try {
            return passiveDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPassiveDeathMessage(ServerMessageType passiveDeathMessage) {
        lock.writeLock().lock();
        try {
            this.passiveDeathMessage = passiveDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ServerMessageType getBossDeathMessage() {
        lock.readLock().lock();
        try {
            return bossDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBossDeathMessage(ServerMessageType bossDeathMessage) {
        lock.writeLock().lock();
        try {
            this.bossDeathMessage = bossDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ServerMessageType getNamedEntityDeathMessage() {
        lock.readLock().lock();
        try {
            return namedEntityDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setNamedEntityDeathMessage(ServerMessageType namedEntityDeathMessage) {
        lock.writeLock().lock();
        try {
            this.namedEntityDeathMessage = namedEntityDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ServerMessageType getKillerDeathMessage() {
        lock.readLock().lock();
        try {
            return killerDeathMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setKillerDeathMessage(ServerMessageType killerDeathMessage) {
        lock.writeLock().lock();
        try {
            this.killerDeathMessage = killerDeathMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getBossMaxHealthThreshold() {
        lock.readLock().lock();
        try {
            if (bossMaxHealthThreshold < 0) {
                return 0;
            }
            return bossMaxHealthThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBossMaxHealthThreshold(double bossMaxHpThreshold) {
        lock.writeLock().lock();
        try {
            if (bossMaxHpThreshold < 0) {
                this.bossMaxHealthThreshold = 0;
            } else {
                this.bossMaxHealthThreshold = bossMaxHpThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PlayerMessageType getPlayerCanSleepMessage() {
        lock.readLock().lock();
        try {
            return playerCanSleepMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPlayerCanSleepMessage(PlayerMessageType playerCanSleepMessage) {
        lock.writeLock().lock();
        try {
            this.playerCanSleepMessage = playerCanSleepMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PlayerMessageType getPlayerDeathCoord() {
        lock.readLock().lock();
        try {
            return playerDeathCoord;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPlayerDeathCoord(PlayerMessageType playerDeathCoord) {
        lock.writeLock().lock();
        try {
            this.playerDeathCoord = playerDeathCoord;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PlayerMessageType getProjectileHitOthers() {
        lock.readLock().lock();
        try {
            return projectileHitOthers;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setProjectileHitOthers(PlayerMessageType projectileHitOthers) {
        lock.writeLock().lock();
        try {
            this.projectileHitOthers = projectileHitOthers;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PlayerMessageType getProjectileBeingHit() {
        lock.readLock().lock();
        try {
            return projectileBeingHit;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setProjectileBeingHit(PlayerMessageType projectileBeingHit) {
        lock.writeLock().lock();
        try {
            this.projectileBeingHit = projectileBeingHit;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PlayerMessageType getInformAfk() {
        lock.readLock().lock();
        try {
            return informAfk;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setInformAfk(PlayerMessageType informAfk) {
        lock.writeLock().lock();
        try {
            this.informAfk = informAfk;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getInformAfkThreshold() {
        lock.readLock().lock();
        try {
            if (informAfkThreshold < 0) {
                return 0;
            }
            return informAfkThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setInformAfkThreshold(int informAfkThreshold) {
        lock.writeLock().lock();
        try {
            if (informAfkThreshold < 0) {
                this.informAfkThreshold = 0;
            } else {
                this.informAfkThreshold = informAfkThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PlayerMessageType getBroadcastAfk() {
        lock.readLock().lock();
        try {
            return broadcastAfk;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBroadcastAfk(PlayerMessageType broadcastAfk) {
        lock.writeLock().lock();
        try {
            this.broadcastAfk = broadcastAfk;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getBroadcastAfkThreshold() {
        lock.readLock().lock();
        try {
            if (broadcastAfkThreshold < 0) {
                return 0;
            }
            return broadcastAfkThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBroadcastAfkThreshold(int broadcastAfkThreshold) {
        lock.writeLock().lock();
        try {
            if (broadcastAfkThreshold < 0) {
                this.broadcastAfkThreshold = 0;
            } else {
                this.broadcastAfkThreshold = broadcastAfkThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PlayerMessageType getStopAfk() {
        lock.readLock().lock();
        try {
            return stopAfk;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setStopAfk(PlayerMessageType stopAfk) {
        lock.writeLock().lock();
        try {
            this.stopAfk = stopAfk;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PlayerMessageType getChangeBiomeMessage() {
        lock.readLock().lock();
        try {
            return changeBiomeMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setChangeBiomeMessage(PlayerMessageType changeBiomeMessage) {
        lock.writeLock().lock();
        try {
            this.changeBiomeMessage = changeBiomeMessage;
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

    public PlayerMessageType getBossFightMessage() {
        lock.readLock().lock();
        try {
            return bossFightMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBossFightMessage(PlayerMessageType bossFightMessage) {
        lock.writeLock().lock();
        try {
            this.bossFightMessage = bossFightMessage;
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

    public PlayerMessageType getMonsterSurroundMessage() {
        lock.readLock().lock();
        try {
            return monsterSurroundMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMonsterSurroundMessage(PlayerMessageType monsterSurroundMessage) {
        lock.writeLock().lock();
        try {
            this.monsterSurroundMessage = monsterSurroundMessage;
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
            if (monsterNumberThreshold < 1) {
                return 1;
            }
            return monsterNumberThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setMonsterNumberThreshold(int monsterNumberThreshold) {
        lock.writeLock().lock();
        try {
            if (monsterNumberThreshold < 1) {
                this.monsterNumberThreshold = 1;
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

    public ServerMessageType getEntityNumberWarning() {
        lock.readLock().lock();
        try {
            return entityNumberWarning;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityNumberWarning(ServerMessageType entityNumberWarning) {
        lock.writeLock().lock();
        try {
            this.entityNumberWarning = entityNumberWarning;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ServerMessageType getEntityDensityWarning() {
        lock.readLock().lock();
        try {
            return entityDensityWarning;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityDensityWarning(ServerMessageType entityDensityWarning) {
        lock.writeLock().lock();
        try {
            this.entityDensityWarning = entityDensityWarning;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getEntityNumberThreshold() {
        lock.readLock().lock();
        try {
            if (entityNumberThreshold < 1) {
                return 1;
            }
            return entityNumberThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityNumberThreshold(int entityNumberThreshold) {
        lock.writeLock().lock();
        try {
            if (entityNumberThreshold < 1) {
                this.entityNumberThreshold = 1;
            } else {
                this.entityNumberThreshold = entityNumberThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getEntityDensityThreshold() {
        lock.readLock().lock();
        try {
            if (entityDensityThreshold < 1) {
                return 1;
            }
            return entityDensityThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityDensityThreshold(int entityDensityThreshold) {
        lock.writeLock().lock();
        try {
            if (entityDensityThreshold < 1) {
                this.entityDensityThreshold = 1;
            } else {
                this.entityDensityThreshold = entityDensityThreshold;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getEntityDensityNumber() {
        lock.readLock().lock();
        try {
            if (entityDensityNumber < 2) {
                return 2;
            }
            return entityDensityNumber;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityDensityNumber(int entityDensityNumber) {
        lock.writeLock().lock();
        try {
            if (entityDensityNumber < 2) {
                this.entityDensityNumber = 2;
            } else {
                this.entityDensityNumber = entityDensityNumber;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getEntityDensityRadius() {
        lock.readLock().lock();
        try {
            if (entityDensityRadius < 0) {
                return 0.0;
            }
            return entityDensityRadius;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityDensityRadius(double entityDensityRadius) {
        lock.writeLock().lock();
        try {
            if (entityDensityRadius < 0) {
                this.entityDensityRadius = 0.0;
            } else {
                this.entityDensityRadius = entityDensityRadius;
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

    public int getEntityDensityInterval() {
        lock.readLock().lock();
        try {
            if (entityDensityInterval <= 0) {
                return 1;
            }
            return entityDensityInterval;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEntityDensityInterval(int entityDensityInterval) {
        lock.writeLock().lock();
        try {
            if (entityDensityInterval <= 0) {
                this.entityDensityInterval = 1;
            } else {
                this.entityDensityInterval = entityDensityInterval;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PlayerMessageType getPlayerHurtMessage() {
        lock.readLock().lock();
        try {
            return playerHurtMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setPlayerHurtMessage(PlayerMessageType playerHurtMessage) {
        lock.writeLock().lock();
        try {
            this.playerHurtMessage = playerHurtMessage;
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

    public PlayerMessageType getTravelMessage() {
        lock.readLock().lock();
        try {
            return travelMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTravelMessage(PlayerMessageType travelMessage) {
        lock.writeLock().lock();
        try {
            this.travelMessage = travelMessage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getTravelMessageInterval() {
        lock.readLock().lock();
        try {
            if (travelMessageInterval <= 0) {
                return 1;
            }
            return travelMessageInterval;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTravelMessageInterval(int travelMessageInterval) {
        lock.writeLock().lock();
        try {
            if (travelMessageInterval <= 0) {
                this.travelMessageInterval = 1;
            } else {
                this.travelMessageInterval = travelMessageInterval;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getTravelWindow() {
        lock.readLock().lock();
        try {
            if (travelWindow <= 0) {
                return 1;
            }
            return travelWindow;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTravelWindow(int travelWindow) {
        lock.writeLock().lock();
        try {
            if (travelWindow <= 0) {
                this.travelWindow = 1;
            } else {
                this.travelWindow = travelWindow;
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

    public PlayerMessageType getTeleportMessage() {
        lock.readLock().lock();
        try {
            return teleportMessage;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTeleportMessage(PlayerMessageType teleportMessage) {
        lock.writeLock().lock();
        try {
            this.teleportMessage = teleportMessage;
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
            return gptTemperature;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setGptTemperature(double gptTemperature) {
        lock.writeLock().lock();
        try {
            if (gptTemperature < 0) {
                this.gptTemperature = 0;
            } else {
                this.gptTemperature = gptTemperature;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getGptServerTimeout() {
        lock.readLock().lock();
        try {
            if (gptServerTimeout < 1000) {
                return 1000;
            }
            return gptServerTimeout;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setGptServerTimeout(int gptServerTimeout) {
        lock.writeLock().lock();
        try {
            if (gptServerTimeout < 1000) {
                this.gptServerTimeout = 1000;
            } else {
                this.gptServerTimeout = gptServerTimeout;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double logScaleToSlider(Integer value) {
        return Math.log(value) / Math.log(Integer.MAX_VALUE);
    }

    public int logScalefromSlider(double sliderValue) {
        return (int) Math.round(Math.exp(sliderValue * Math.log(Integer.MAX_VALUE)));
    }

    public MutableComponent getDoubleConfigDisplayText(Double value) {
        return Component.literal(String.format("%.2f", value));
    }

    public MutableComponent getTickConfigDisplayText(Integer value) {
        double seconds = (double) value / 20.0;
        return Component.literal(String.format("%.1f", seconds));
    }

    public int commandInputSecondToTick(Integer seconds) {
        return (int) Math.round(20.0 * seconds);
    }

    public MutableComponent getMiniSecondsDisplayText(Integer value) {
        double seconds = (double) value / 1000.0;
        return Component.literal(String.format("%.1f", seconds));
    }

    public int commandInputSecondToMiniSecond(Integer seconds) {
        return (int) Math.round(1000.0 * seconds);
    }

    
    /**
     * Retrieves a secure version of the GPT access token.
     * The secure token masks the middle part of the original token with asterisks for security purposes.
     * 
     * @param value The original GPT access token to be secured.
     * @return A string representing the secure version of the GPT access token. 
     *         If the token length is greater than 20, the first 5 and last 5 characters are visible, 
     *         with the middle characters replaced by asterisks. 
     *         If the token length is between 1 and 20, the entire token is replaced by asterisks. 
     *         If the token is empty, "null" is returned.
     */
    public MutableComponent getSecureGptAccessTokens(String value) {
        String token = value == null ? "" : value;
        String secureToken = "";
        if (token.length() > 20) {
            secureToken = token.substring(0, 5) + String.valueOf("*".repeat(token.length() - 10)) + token.substring(token.length() - 5);
        } else if (token.length() > 0) {
            secureToken = String.valueOf("*".repeat(token.length()));
        } else {
            secureToken = "null";
        }
        return Component.literal(secureToken);
    }

    public MutableComponent getBooleanValueI18n(Boolean value) {
        // Reflection uses value.getClass() to find the method, where value is defined as Object
        // We cannot use primitive here or it will throw NoSuchMethodException
        if (value) {
            return Util.parseTranslatableText("options.on").withStyle(ChatFormatting.GREEN);
        } else {
            return Util.parseTranslatableText("options.off").withStyle(ChatFormatting.RED);
        }
    }
}
