/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Base class representing a message display configuration.
 * <p>
 * A {@code MessageType} holds two {@link MessageType.Location} values: one for the
 * "main" audience (e.g., the directly involved player or operator) and one for
 * all other receivers. Subclasses ({@link ServerMessageType}, {@link PlayerMessageType})
 * add a {@code Receiver} filter to determine <em>who</em> gets which location.
 * <p>
 * Instances are immutable and intended to be stored as config fields and serialized
 * via Gson. All three enum-typed fields ({@code mainPlayerLocation},
 * {@code otherPlayerLocation}, and the subclass {@code receiver}) are
 * serialized as plain JSON strings, so the resulting config object looks like:
 * <pre>
 * {
 *   "mainPlayerLocation": "CHAT",
 *   "otherPlayerLocation": "NONE",
 *   "receiver": "ALL"
 * }
 * </pre>
 * Use the {@code of(...)} factory methods on the concrete subclasses to create instances.
 */
public class MessageType {

    /**
     * The display location used for the primary audience of a message
     * (e.g., the player who triggered the event, or an operator).
     */
    public final MessageType.Location mainPlayerLocation;

    /**
     * The display location used for all other players who are not considered
     * the primary audience of a message.
     * <p>
     * Set to {@link MessageType.Location#NONE} to suppress the message for other players.
     */
    public final MessageType.Location otherPlayerLocation;

    /**
     * Constructs a new {@code MessageType} with the specified display locations.
     *
     * @param mainPlayerLocation  the location for the primary audience; must not be null
     * @param otherPlayerLocation the location for all other receivers; must not be null
     */
    protected MessageType(MessageType.Location mainPlayerLocation, MessageType.Location otherPlayerLocation) {
        this.mainPlayerLocation = mainPlayerLocation;
        this.otherPlayerLocation = otherPlayerLocation;
    }

    /**
     * Specifies where a message should be displayed on the client's screen.
     * <p>
     * Gson serializes each constant as its name string (e.g., {@code "CHAT"}).
     */
    public static enum Location {

        /** 
         * The message is suppressed and will not be shown. 
         */
        NONE,

        /** 
         * The message is shown in the chat window. 
         */
        CHAT,

        /** 
         * The message is shown in the action bar (above the hotbar). 
         */
        ACTIONBAR
    }

    /**
     * Sends a message to the specified player based on the provided message location type.
     *
     * @param player The {@link ServerPlayerEntity} to whom the message will be sent. Null means console.
     * @param location   The {@link MessageType.Location} indicating where the message should be displayed (e.g., CHAT, ACTIONBAR, NONE). Must not be null.
     * @param message The {@link Text} message to be sent. Must not be null.
     */
    public static void sendMessage(@Nullable ServerPlayerEntity player, @NotNull MessageType.Location location, @NotNull Text message) {
        switch (location) {
            case NONE:
                break;
            case CHAT:
                if (player == null) {
                    LoggerFactory.getLogger(Util.LOGGERNAME).info(message.getString());
                } else {
                    sendTextMessage(player, message);
                }
                break;
            case ACTIONBAR:
                if (player == null) {
                    LoggerFactory.getLogger(Util.LOGGERNAME).debug(message.getString());
                } else {
                    sendActionBarMessage(player, message);
                }
                break;
            default:
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Invalid message type: " + location);
                break;
        }
    }
    
    /**
     * Broadcasts a message to players on the server based on the specified message location type.
     *
     * @param server The Minecraft server instance. Can be null if no server is available.
     * @param type   The location type where the message should be broadcasted. Must not be null.
     * @param message The message to broadcast. Must not be null.
     */
    public static void broadcastMessage(@Nullable MinecraftServer server, @NotNull MessageType.Location location, @NotNull Text message) {
        switch (location) {
            case NONE:
                break;
            case CHAT:
                broadcastTextMessage(server, message);
                break;
            case ACTIONBAR:
                broadcastActionBarMessage(server, message);
                break;
            default:
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Invalid message type: " + location);
                break;
        }
    }

    /**
     * Sends an action bar message to the specified player.
     *
     * @param player  The {@link ServerPlayerEntity} to whom the message will be sent. Must not be null.
     * @param message The {@link Text} message to display in the action bar. Must not be null.
     */
    public static void sendActionBarMessage(@NotNull ServerPlayerEntity player, @NotNull Text message) {
        player.networkHandler.sendPacket(new OverlayMessageS2CPacket(message));
    }

    /**
     * Sends an action bar message to all online players on the server.
     *
     * @param server  The Minecraft server instance. If null, the method will return without performing any action.
     * @param message The message to be displayed in the action bar. Must not be null.
     */
    public static void broadcastActionBarMessage(@Nullable MinecraftServer server, @NotNull Text message) {
        if (server == null) {
            return;
        }
        List<ServerPlayerEntity> players = Util.getOnlinePlayers(server);
        for (ServerPlayerEntity player : players) {
            sendActionBarMessage(player, message);
        }
        LoggerFactory.getLogger(Util.LOGGERNAME).debug(message.getString());
    }

    /**
     * Sends a text message to the specified player.
     *
     * @param player  The player to whom the message will be sent. Must not be null.
     * @param message The text message to send to the player. Must not be null.
     */
    public static void sendTextMessage(@NotNull PlayerEntity player, @NotNull Text message) {
        player.sendMessage(message, false);
    }

    /**
     * Broadcasts a text message to all online players on the server and logs the message
     * to the server console.
     *
     * @param server The Minecraft server instance. If null, the method will return without doing anything.
     * @param message The text message to broadcast. Must not be null.
     */
    public static void broadcastTextMessage(@Nullable MinecraftServer server, @NotNull Text message) {
        if (server == null) {
            return;
        }
        List<ServerPlayerEntity> players = Util.getOnlinePlayers(server);
        for (ServerPlayerEntity player : players) {
            sendTextMessage(player, message);
        }
        LoggerFactory.getLogger(Util.LOGGERNAME).info(message.getString());
    }

    /**
     * Returns a localized and formatted text representation of the given message location type.
     * @param type the message location type to localize; must not be null
     * @return a {@link MutableText} containing the localized name of the message location type, formatted with colors for better readability
     */
    public static MutableText getMessageLocationI18n(MessageType.Location type) {
        switch (type) {
            case NONE:
                return Util.parseTranslatableText("fmod.message.type.none").formatted(Formatting.RED);
            case CHAT:
                return Util.parseTranslatableText("fmod.message.type.chat").formatted(Formatting.GREEN);
            case ACTIONBAR:
                return Util.parseTranslatableText("fmod.message.type.actionbar").formatted(Formatting.YELLOW);
            default:
                return Text.literal(type.toString());
        }
    }

    /**
     * Returns true if the given object is equal to this {@code MessageType}.
     * Two {@code MessageType} instances are equal if they are of the same class
     * and have equal {@code mainPlayerLocation} and {@code otherPlayerLocation}.
     *
     * @param obj the object to compare
     * @return true if both objects are of the same class and all fields are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MessageType other = (MessageType) obj;
        return mainPlayerLocation == other.mainPlayerLocation && otherPlayerLocation == other.otherPlayerLocation;
    }

    /**
     * Returns a hash code derived from {@code mainPlayerLocation} and {@code otherPlayerLocation}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(mainPlayerLocation, otherPlayerLocation);
    }

    /**
     * Returns a string representation of this {@code MessageType}.
     *
     * @return a string in the form {@code MessageType{mainPlayerLocation=..., otherPlayerLocation=...}}
     */
    @Override
    public String toString() {
        return "MessageType{" +
               "mainPlayerLocation=" + mainPlayerLocation +
               ", otherPlayerLocation=" + otherPlayerLocation +
               '}';
    }
}
