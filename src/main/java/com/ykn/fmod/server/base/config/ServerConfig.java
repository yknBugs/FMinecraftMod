/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.config;

import com.ykn.fmod.server.base.util.PlayerMessageType;
import com.ykn.fmod.server.base.util.ServerMessageType;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ServerConfig extends ConfigReader {

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
    protected volatile boolean serverTranslation;

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
    protected volatile int maxFlowLength;

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
    protected volatile int maxFlowRecursionDepth;

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
    protected volatile int maxFlowHistorySize;

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
    protected volatile ServerMessageType entityDeathMessage;

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
    protected volatile ServerMessageType hostileDeathMessage;

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
    protected volatile ServerMessageType passiveDeathMessage;

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
    protected volatile ServerMessageType bossDeathMessage;

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
    protected volatile ServerMessageType namedEntityDeathMessage;

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
    protected volatile ServerMessageType killerDeathMessage;

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
    protected volatile double bossMaxHealthThreshold;

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
    protected volatile PlayerMessageType playerCanSleepMessage;

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
    protected volatile PlayerMessageType playerDeathCoord;

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
    protected volatile PlayerMessageType projectileHitOthers;

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
    protected volatile PlayerMessageType projectileBeingHit;

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
    protected volatile PlayerMessageType informAfk;

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
    protected volatile int informAfkThreshold;

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
    protected volatile PlayerMessageType broadcastAfk;

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
    protected volatile int broadcastAfkThreshold;

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
    protected volatile PlayerMessageType stopAfk;

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
    protected volatile PlayerMessageType changeBiomeMessage;

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
    protected volatile int changeBiomeDelay;

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
    protected volatile PlayerMessageType bossFightMessage;

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
    protected volatile int bossFightInterval;

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
    protected volatile PlayerMessageType monsterSurroundMessage;

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
    protected volatile int monsterSurroundInterval;

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
    protected volatile int monsterNumberThreshold;

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
    protected volatile double monsterDistanceThreshold;

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
    protected volatile ServerMessageType entityNumberWarning;

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
    protected volatile ServerMessageType entityDensityWarning;

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
    protected volatile int entityNumberThreshold;

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
    protected volatile int entityDensityThreshold;

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
    protected volatile int entityDensityNumber;

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
    protected volatile double entityDensityRadius;

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
    protected volatile int entityNumberInterval;

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
    protected volatile int entityDensityInterval;

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
    protected volatile PlayerMessageType playerHurtMessage;

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
    protected volatile double playerHurtThreshold;

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
    protected volatile PlayerMessageType travelMessage;

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
    protected volatile int travelMessageInterval;

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
    protected volatile int travelWindow;

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
    protected volatile double travelTotalDistanceThreshold;

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
    protected volatile int travelPartialInterval;

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
    protected volatile double travelPartialDistanceThreshold;

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
    protected volatile double teleportThreshold;

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
    protected volatile PlayerMessageType teleportMessage;

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
    protected volatile String gptUrl;

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
    protected volatile String gptAccessTokens;

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
    protected volatile String gptModel;

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
    protected volatile String gptSystemPrompt;

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
    protected volatile double gptTemperature;

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
    protected volatile int gptServerTimeout;

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
        return serverTranslation;
    }

    public void setServerTranslation(boolean serverTranslation) {
        this.serverTranslation = serverTranslation;
    }

    public int getMaxFlowLength() {
        if (maxFlowLength < 0) {
            return 0;
        }
        return maxFlowLength;
    }

    public void setMaxFlowLength(int maxFlowLength) {
        if (maxFlowLength < 0) {
            this.maxFlowLength = 0;
        } else {
            this.maxFlowLength = maxFlowLength;
        }
    }

    public int getMaxFlowRecursionDepth() {
        if (maxFlowRecursionDepth < 0) {
            return 0;
        }
        return maxFlowRecursionDepth;
    }

    public void setMaxFlowRecursionDepth(int maxFlowRecursionDepth) {
        if (maxFlowRecursionDepth < 0) {
            this.maxFlowRecursionDepth = 0;
        } else {
            this.maxFlowRecursionDepth = maxFlowRecursionDepth;
        }
    }

    public int getMaxFlowHistorySize() {
        if (maxFlowHistorySize < 0) {
            return 0;
        }
        return maxFlowHistorySize;
    }

    public void setMaxFlowHistorySize(int maxFlowHistorySize) {
        if (maxFlowHistorySize < 0) {
            this.maxFlowHistorySize = 0;
        } else {
            this.maxFlowHistorySize = maxFlowHistorySize;
        }
    }

    public ServerMessageType getEntityDeathMessage() {
        return entityDeathMessage;
    }

    public void setEntityDeathMessage(ServerMessageType entityDeathMessage) {
        this.entityDeathMessage = entityDeathMessage;
    }

    public ServerMessageType getHostileDeathMessage() {
        return hostileDeathMessage;
    }

    public void setHostileDeathMessage(ServerMessageType hostileDeathMessage) {
        this.hostileDeathMessage = hostileDeathMessage;
    }

    public ServerMessageType getPassiveDeathMessage() {
        return passiveDeathMessage;
    }

    public void setPassiveDeathMessage(ServerMessageType passiveDeathMessage) {
        this.passiveDeathMessage = passiveDeathMessage;
    }

    public ServerMessageType getBossDeathMessage() {
        return bossDeathMessage;
    }

    public void setBossDeathMessage(ServerMessageType bossDeathMessage) {
        this.bossDeathMessage = bossDeathMessage;
    }

    public ServerMessageType getNamedEntityDeathMessage() {
        return namedEntityDeathMessage;
    }

    public void setNamedEntityDeathMessage(ServerMessageType namedEntityDeathMessage) {
        this.namedEntityDeathMessage = namedEntityDeathMessage;
    }

    public ServerMessageType getKillerDeathMessage() {
        return killerDeathMessage;
    }

    public void setKillerDeathMessage(ServerMessageType killerDeathMessage) {
        this.killerDeathMessage = killerDeathMessage;
    }

    public double getBossMaxHealthThreshold() {
        if (bossMaxHealthThreshold < 0) {
            return 0;
        }
        return bossMaxHealthThreshold;
    }

    public void setBossMaxHealthThreshold(double bossMaxHpThreshold) {
        if (bossMaxHpThreshold < 0) {
            this.bossMaxHealthThreshold = 0;
        } else {
            this.bossMaxHealthThreshold = bossMaxHpThreshold;
        }
    }

    public PlayerMessageType getPlayerCanSleepMessage() {
        return playerCanSleepMessage;
    }

    public void setPlayerCanSleepMessage(PlayerMessageType playerCanSleepMessage) {
        this.playerCanSleepMessage = playerCanSleepMessage;
    }

    public PlayerMessageType getPlayerDeathCoord() {
        return playerDeathCoord;
    }

    public void setPlayerDeathCoord(PlayerMessageType playerDeathCoord) {
        this.playerDeathCoord = playerDeathCoord;
    }

    public PlayerMessageType getProjectileHitOthers() {
        return projectileHitOthers;
    }

    public void setProjectileHitOthers(PlayerMessageType projectileHitOthers) {
        this.projectileHitOthers = projectileHitOthers;
    }

    public PlayerMessageType getProjectileBeingHit() {
        return projectileBeingHit;
    }

    public void setProjectileBeingHit(PlayerMessageType projectileBeingHit) {
        this.projectileBeingHit = projectileBeingHit;
    }

    public PlayerMessageType getInformAfk() {
        return informAfk;
    }

    public void setInformAfk(PlayerMessageType informAfk) {
        this.informAfk = informAfk;
    }

    public int getInformAfkThreshold() {
        if (informAfkThreshold < 0) {
            return 0;
        }
        return informAfkThreshold;
    }

    public void setInformAfkThreshold(int informAfkThreshold) {
        if (informAfkThreshold < 0) {
            this.informAfkThreshold = 0;
        } else {
            this.informAfkThreshold = informAfkThreshold;
        }
    }

    public PlayerMessageType getBroadcastAfk() {
        return broadcastAfk;
    }

    public void setBroadcastAfk(PlayerMessageType broadcastAfk) {
        this.broadcastAfk = broadcastAfk;
    }

    public int getBroadcastAfkThreshold() {
        if (broadcastAfkThreshold < 0) {
            return 0;
        }
        return broadcastAfkThreshold;
    }

    public void setBroadcastAfkThreshold(int broadcastAfkThreshold) {
        if (broadcastAfkThreshold < 0) {
            this.broadcastAfkThreshold = 0;
        } else {
            this.broadcastAfkThreshold = broadcastAfkThreshold;
        }
    }

    public PlayerMessageType getStopAfk() {
        return stopAfk;
    }

    public void setStopAfk(PlayerMessageType stopAfk) {
        this.stopAfk = stopAfk;
    }

    public PlayerMessageType getChangeBiomeMessage() {
        return changeBiomeMessage;
    }

    public void setChangeBiomeMessage(PlayerMessageType changeBiomeMessage) {
        this.changeBiomeMessage = changeBiomeMessage;
    }

    public int getChangeBiomeDelay() {
        if (changeBiomeDelay < 0) {
            return 0;
        }
        return changeBiomeDelay;
    }

    public void setChangeBiomeDelay(int changeBiomeDelay) {
        if (changeBiomeDelay < 0) {
            this.changeBiomeDelay = 0;
        } else {
            this.changeBiomeDelay = changeBiomeDelay;
        }
    }

    public PlayerMessageType getBossFightMessage() {
        return bossFightMessage;
    }

    public void setBossFightMessage(PlayerMessageType bossFightMessage) {
        this.bossFightMessage = bossFightMessage;
    }

    public int getBossFightInterval() {
        if (bossFightInterval < 0) {
            return 0;
        }
        return bossFightInterval;
    }

    public void setBossFightInterval(int bossFightInterval) {
        if (bossFightInterval < 0) {
            this.bossFightInterval = 0;
        } else {
            this.bossFightInterval = bossFightInterval;
        }
    }

    public PlayerMessageType getMonsterSurroundMessage() {
        return monsterSurroundMessage;
    }

    public void setMonsterSurroundMessage(PlayerMessageType monsterSurroundMessage) {
        this.monsterSurroundMessage = monsterSurroundMessage;
    }

    public int getMonsterSurroundInterval() {
        if (monsterSurroundInterval < 0) {
            return 0;
        }
        return monsterSurroundInterval;
    }

    public void setMonsterSurroundInterval(int monsterSurroundInterval) {
        if (monsterSurroundInterval < 0) {
            this.monsterSurroundInterval = 0;
        } else {
            this.monsterSurroundInterval = monsterSurroundInterval;
        }
    }

    public int getMonsterNumberThreshold() {
        if (monsterNumberThreshold < 1) {
            return 1;
        }
        return monsterNumberThreshold;
    }

    public void setMonsterNumberThreshold(int monsterNumberThreshold) {
        if (monsterNumberThreshold < 1) {
            this.monsterNumberThreshold = 1;
        } else {
            this.monsterNumberThreshold = monsterNumberThreshold;
        }
    }

    public double getMonsterDistanceThreshold() {
        if (monsterDistanceThreshold < 0) {
            return 0.0;
        }
        return monsterDistanceThreshold;
    }

    public void setMonsterDistanceThreshold(double monsterDistanceThreshold) {
        if (monsterDistanceThreshold < 0) {
            this.monsterDistanceThreshold = 0;
        } else {
            this.monsterDistanceThreshold = monsterDistanceThreshold;
        }
    }

    public ServerMessageType getEntityNumberWarning() {
        return entityNumberWarning;
    }

    public void setEntityNumberWarning(ServerMessageType entityNumberWarning) {
        this.entityNumberWarning = entityNumberWarning;
    }

    public ServerMessageType getEntityDensityWarning() {
        return entityDensityWarning;
    }

    public void setEntityDensityWarning(ServerMessageType entityDensityWarning) {
        this.entityDensityWarning = entityDensityWarning;
    }

    public int getEntityNumberThreshold() {
        if (entityNumberThreshold < 1) {
            return 1;
        }
        return entityNumberThreshold;
    }

    public void setEntityNumberThreshold(int entityNumberThreshold) {
        if (entityNumberThreshold < 1) {
            this.entityNumberThreshold = 1;
        } else {
            this.entityNumberThreshold = entityNumberThreshold;
        }
    }

    public int getEntityDensityThreshold() {
        if (entityDensityThreshold < 1) {
            return 1;
        }
        return entityDensityThreshold;
    }

    public void setEntityDensityThreshold(int entityDensityThreshold) {
        if (entityDensityThreshold < 1) {
            this.entityDensityThreshold = 1;
        } else {
            this.entityDensityThreshold = entityDensityThreshold;
        }
    }

    public int getEntityDensityNumber() {
        if (entityDensityNumber < 2) {
            return 2;
        }
        return entityDensityNumber;
    }

    public void setEntityDensityNumber(int entityDensityNumber) {
        if (entityDensityNumber < 2) {
            this.entityDensityNumber = 2;
        } else {
            this.entityDensityNumber = entityDensityNumber;
        }
    }

    public double getEntityDensityRadius() {
        if (entityDensityRadius < 0) {
            return 0.0;
        }
        return entityDensityRadius;
    }

    public void setEntityDensityRadius(double entityDensityRadius) {
        if (entityDensityRadius < 0) {
            this.entityDensityRadius = 0.0;
        } else {
            this.entityDensityRadius = entityDensityRadius;
        }
    }

    public int getEntityNumberInterval() {
        if (entityNumberInterval <= 0) {
            return 1;
        }
        return entityNumberInterval;
    }

    public void setEntityNumberInterval(int entityNumberInterval) {
        if (entityNumberInterval <= 0) {
            this.entityNumberInterval = 1;
        } else {
            this.entityNumberInterval = entityNumberInterval;
        }
    }

    public int getEntityDensityInterval() {
        if (entityDensityInterval <= 0) {
            return 1;
        }
        return entityDensityInterval;
    }

    public void setEntityDensityInterval(int entityDensityInterval) {
        if (entityDensityInterval <= 0) {
            this.entityDensityInterval = 1;
        } else {
            this.entityDensityInterval = entityDensityInterval;
        }
    }

    public PlayerMessageType getPlayerHurtMessage() {
        return playerHurtMessage;
    }

    public void setPlayerHurtMessage(PlayerMessageType playerHurtMessage) {
        this.playerHurtMessage = playerHurtMessage;
    }

    public double getPlayerHurtThreshold() {
        if (playerHurtThreshold < 0) {
            return 0;
        }
        if (playerHurtThreshold > 1) {
            return 1;
        }
        return playerHurtThreshold;
    }

    public void setPlayerHurtThreshold(double playerHurtThreshold) {
        if (playerHurtThreshold < 0) {
            this.playerHurtThreshold = 0;
        } else if (playerHurtThreshold > 1) {
            this.playerHurtThreshold = 1;
        } else {
            this.playerHurtThreshold = playerHurtThreshold;
        }
    }

    public PlayerMessageType getTravelMessage() {
        return travelMessage;
    }

    public void setTravelMessage(PlayerMessageType travelMessage) {
        this.travelMessage = travelMessage;
    }

    public int getTravelMessageInterval() {
        if (travelMessageInterval <= 0) {
            return 1;
        }
        return travelMessageInterval;
    }

    public void setTravelMessageInterval(int travelMessageInterval) {
        if (travelMessageInterval <= 0) {
            this.travelMessageInterval = 1;
        } else {
            this.travelMessageInterval = travelMessageInterval;
        }
    }

    public int getTravelWindow() {
        if (travelWindow <= 0) {
            return 1;
        }
        return travelWindow;
    }

    public void setTravelWindow(int travelWindow) {
        if (travelWindow <= 0) {
            this.travelWindow = 1;
        } else {
            this.travelWindow = travelWindow;
        }
    }

    public double getTravelTotalDistanceThreshold() {
        if (travelTotalDistanceThreshold < 0) {
            return 0;
        }
        return travelTotalDistanceThreshold;
    }

    public void setTravelTotalDistanceThreshold(double travelTotalDistanceThreshold) {
        if (travelTotalDistanceThreshold < 0) {
            this.travelTotalDistanceThreshold = 0;
        } else {
            this.travelTotalDistanceThreshold = travelTotalDistanceThreshold;
        }
    }

    public int getTravelPartialInterval() {
        if (travelPartialInterval <= 0) {
            return 1;
        }
        return travelPartialInterval;
    }

    public void setTravelPartialInterval(int travelPartialInterval) {
        if (travelPartialInterval <= 0) {
            this.travelPartialInterval = 1;
        } else {
            this.travelPartialInterval = travelPartialInterval;
        }
    }

    public double getTravelPartialDistanceThreshold() {
        if (travelPartialDistanceThreshold < 0) {
            return 0;
        }
        return travelPartialDistanceThreshold;
    }

    public void setTravelPartialDistanceThreshold(double travelPartialDistanceThreshold) {
        if (travelPartialDistanceThreshold < 0) {
            this.travelPartialDistanceThreshold = 0;
        } else {
            this.travelPartialDistanceThreshold = travelPartialDistanceThreshold;
        }
    }

    public double getTeleportThreshold() {
        if (teleportThreshold < 0) {
            return 0;
        }
        return teleportThreshold;
    }

    public void setTeleportThreshold(double teleportThreshold) {
        if (teleportThreshold < 0) {
            this.teleportThreshold = 0;
        } else {
            this.teleportThreshold = teleportThreshold;
        }
    }

    public PlayerMessageType getTeleportMessage() {
        return teleportMessage;
    }

    public void setTeleportMessage(PlayerMessageType teleportMessage) {
        this.teleportMessage = teleportMessage;
    }

    public String getGptUrl() {
        return gptUrl;
    }

    public void setGptUrl(String gptUrl) {
        this.gptUrl = gptUrl;
    }

    public String getGptAccessTokens() {
        return gptAccessTokens;
    }

    public void setGptAccessTokens(String gptAccessTokens) {
        this.gptAccessTokens = gptAccessTokens;
    }

    public String getGptModel() {
        return gptModel;
    }

    public void setGptModel(String gptModel) {
        this.gptModel = gptModel;
    }

    public String getGptSystemPrompt() {
        return gptSystemPrompt;
    }

    public void setGptSystemPrompt(String gptSystemPrompt) {
        this.gptSystemPrompt = gptSystemPrompt;
    }

    public double getGptTemperature() {
        if (gptTemperature < 0) {
            return 0;
        }
        return gptTemperature;
    }

    public void setGptTemperature(double gptTemperature) {
        if (gptTemperature < 0) {
            this.gptTemperature = 0;
        } else {
            this.gptTemperature = gptTemperature;
        }
    }

    public int getGptServerTimeout() {
        if (gptServerTimeout < 1000) {
            return 1000;
        }
        return gptServerTimeout;
    }

    public void setGptServerTimeout(int gptServerTimeout) {
        if (gptServerTimeout < 1000) {
            this.gptServerTimeout = 1000;
        } else {
            this.gptServerTimeout = gptServerTimeout;
        }
    }

    public double logScaleToSlider(Integer value) {
        return Math.log(value) / Math.log(Integer.MAX_VALUE);
    }

    public int logScalefromSlider(double sliderValue) {
        return (int) Math.round(Math.exp(sliderValue * Math.log(Integer.MAX_VALUE)));
    }

    public MutableText getDoubleConfigDisplayText(Double value) {
        return Text.literal(String.format("%.2f", value));
    }

    public MutableText getTickConfigDisplayText(Integer value) {
        double seconds = (double) value / 20.0;
        return Text.literal(String.format("%.1f", seconds));
    }

    public int commandInputSecondToTick(Integer seconds) {
        return (int) Math.round(20.0 * seconds);
    }

    public MutableText getMiniSecondsDisplayText(Integer value) {
        double seconds = (double) value / 1000.0;
        return Text.literal(String.format("%.1f", seconds));
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
    public MutableText getSecureGptAccessTokens(String value) {
        String token = value == null ? "" : value;
        String secureToken = "";
        if (token.length() > 20) {
            secureToken = token.substring(0, 5) + String.valueOf("*".repeat(token.length() - 10)) + token.substring(token.length() - 5);
        } else if (token.length() > 0) {
            secureToken = String.valueOf("*".repeat(token.length()));
        } else {
            secureToken = "";
        }
        return Text.literal(secureToken);
    }

    public MutableText getBooleanValueI18n(Boolean value) {
        // Reflection uses value.getClass() to find the method, where value is defined as Object
        // We cannot use primitive here or it will throw NoSuchMethodException
        if (value) {
            return Util.parseTranslatableText("options.on").formatted(Formatting.GREEN);
        } else {
            return Util.parseTranslatableText("options.off").formatted(Formatting.RED);
        }
    }
}
