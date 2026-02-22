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

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A {@link MessageType} specialization for player-triggered events where the message
 * audience can be filtered by team membership and/or source player identity in addition
 * to permission level.
 * <p> 
 * A single {@code PlayerMessageType} field replaces both and is fully serializable by Gson:
 * <pre>
 * {
 *   "mainPlayerLocation": "CHAT",
 *   "otherPlayerLocation": "NONE",
 *   "receiver": "SELFOP"
 * }
 * </pre>
 * Use the {@link #of} factory methods to create instances.
 *
 * @see ServerMessageType
 */
public class PlayerMessageType extends MessageType {

    /**
     * Determines which players receive the {@code mainMessage} vs the {@code otherMessage},
     * taking the source player's identity and team into account.
     *
     * @see PlayerMessageType.Receiver
     */
    public final PlayerMessageType.Receiver receiver;

    /**
     * Constructs a new {@code PlayerMessageType}.
     *
     * @param mainPlayerLocation  display location for receivers matched by {@code receiver}
     * @param otherPlayerLocation display location for all other receivers
     * @param receiver            the audience filter; must not be null
     */
    protected PlayerMessageType(MessageType.Location mainPlayerLocation, MessageType.Location otherPlayerLocation, PlayerMessageType.Receiver receiver) {
        super(mainPlayerLocation, otherPlayerLocation);
        this.receiver = receiver;
    }

    /**
     * Defines who receives the main message in a player-triggered event broadcast.
     * <p>
     * Receivers are evaluated per player against the {@code sourcePlayer} who triggered
     * the event. Gson serializes each constant as its name string (e.g., {@code "SELFOP"}).
     */
    public static enum Receiver {

        /** 
         * Every online player receives the main message. 
         */
        ALL,

        /**
         * Only operators (permission level ≥ 2) and the console receive the main message;
         * others receive the other message.
         */
        OP,

        /**
         * The source player themselves and operators receive the main message;
         * all other players receive the other message.
         */
        SELFOP,

        /**
         * Teammates of the source player, the source player themselves, and operators
         * receive the main message; all other players receive the other message.
         */
        TEAMOP,

        /**
         * Only teammates of the source player and the source player themselves receive
         * the main message; all other players receive the other message.
         */
        TEAM,

        /**
         * Only the source player themselves receives the main message;
         * all other players receive the other message.
         */
        SELF,

        /** 
         * No player receives the main message; everyone receives the other message. 
         */
        NONE
    }

    /**
     * Posts a message to a single player according to this type's receiver filter.
     * <p>
     * The appropriate message variant ({@code mainMessage} or {@code otherMessage}) and
     * display location are chosen based on {@link #receiver}, the {@code sourcePlayer}
     * identity, team membership, and permission level.
     *
     * @param sourcePlayer    the player who triggered the event; must not be null
     * @param currentReceiver the player to send the message to, or {@code null} for the console
     * @param mainMessage     the message shown to the primary audience; must not be null
     * @param otherMessage    the fallback message shown to non-primary receivers; must not be null
     */
    public void postMessage(@NotNull ServerPlayerEntity sourcePlayer, @Nullable ServerPlayerEntity currentReceiver, @NotNull Text mainMessage, @NotNull Text otherMessage) {
        switch (this.receiver) {
            case ALL:
                sendMessage(currentReceiver, this.mainPlayerLocation, mainMessage);
                break;
            case OP:
                if (currentReceiver == null || currentReceiver.hasPermissionLevel(2)) {
                    sendMessage(currentReceiver, this.mainPlayerLocation, mainMessage);
                } else {
                    sendMessage(currentReceiver, this.otherPlayerLocation, otherMessage);
                }
                break;
            case SELFOP:
                if (currentReceiver == null || currentReceiver.hasPermissionLevel(2) || currentReceiver.equals(sourcePlayer)) {
                    sendMessage(currentReceiver, this.mainPlayerLocation, mainMessage);
                } else {
                    sendMessage(currentReceiver, this.otherPlayerLocation, otherMessage);
                }
                break;
            case TEAMOP:
                if (currentReceiver == null || currentReceiver.hasPermissionLevel(2) || currentReceiver.equals(sourcePlayer) || currentReceiver.isTeammate(sourcePlayer)) {
                    sendMessage(currentReceiver, this.mainPlayerLocation, mainMessage);
                } else {
                    sendMessage(currentReceiver, this.otherPlayerLocation, otherMessage);
                }
                break;
            case TEAM:
                if (currentReceiver == null) {
                    sendMessage(currentReceiver, this.otherPlayerLocation, otherMessage);
                } else if (currentReceiver.equals(sourcePlayer) || currentReceiver.isTeammate(sourcePlayer)) {
                    sendMessage(currentReceiver, this.mainPlayerLocation, mainMessage);
                } else {
                    sendMessage(currentReceiver, this.otherPlayerLocation, otherMessage);
                }
            case SELF:
                if (currentReceiver == null) {
                    sendMessage(currentReceiver, this.otherPlayerLocation, otherMessage);
                } else if (currentReceiver.equals(sourcePlayer)) {
                    sendMessage(currentReceiver, this.mainPlayerLocation, mainMessage);
                } else {
                    sendMessage(currentReceiver, this.otherPlayerLocation, otherMessage);
                }
                break;
            case NONE:
                sendMessage(currentReceiver, this.otherPlayerLocation, otherMessage);
                break;
            default:
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Invalid message receiver type: " + this.receiver);
                break;
        }
    }

    /**
     * Broadcasts a message to all online players according to this type's receiver filter.
     * <p>
     * Iterates over every online player and delegates to
     * {@link #postMessage(ServerPlayerEntity, ServerPlayerEntity, Text, Text)} for each one.
     *
     * @param sourcePlayer the player who triggered the event; must not be null
     * @param mainMessage  the message shown to the primary audience; must not be null
     * @param otherMessage the fallback message shown to non-primary receivers; must not be null
     */
    public void postMessage(@NotNull ServerPlayerEntity sourcePlayer, @NotNull Text mainMessage, @NotNull Text otherMessage) {
        List<ServerPlayerEntity> players = Util.getOnlinePlayers(sourcePlayer.getServer());
        for (ServerPlayerEntity player : players) {
            this.postMessage(sourcePlayer, player, mainMessage, otherMessage);
        }
        this.postMessage(sourcePlayer, null, mainMessage, otherMessage);
    }

    /**
     * Broadcasts a message to all online players on the server according to this type's
     * receiver filter, using the source player's identity for audience evaluation.
     * Non-primary receivers will not receive any message.
     *
     * @param sourcePlayer the player who triggered the event; must not be null
     * @param mainMessage  the message shown to the primary audience; must not be null
     */
    public void postMessage(@NotNull ServerPlayerEntity sourcePlayer, @NotNull Text mainMessage) {
        PlayerMessageType type = this.updateOther(MessageType.Location.NONE);
        List<ServerPlayerEntity> players = Util.getOnlinePlayers(sourcePlayer.getServer());
        for (ServerPlayerEntity player : players) {
            type.postMessage(sourcePlayer, player, mainMessage, Text.empty());
        }
    }

    /**
     * Creates a {@code PlayerMessageType} with independent locations for the main and
     * other audiences.
     *
     * @param mainPlayerLocation  display location for the primary audience
     * @param otherPlayerLocation display location for all other receivers
     * @param receiver            the audience filter
     * @return a new {@code PlayerMessageType} instance
     */
    public static PlayerMessageType of(MessageType.Location mainPlayerLocation, MessageType.Location otherPlayerLocation, PlayerMessageType.Receiver receiver) {
        return new PlayerMessageType(mainPlayerLocation, otherPlayerLocation, receiver);
    }

    /**
     * Creates a {@code PlayerMessageType} where the other-audience location defaults to
     * {@link MessageType.Location#NONE}.
     *
     * @param mainPlayerLocation display location for the primary audience
     * @param receiver           the audience filter
     * @return a new {@code PlayerMessageType} instance
     */
    public static PlayerMessageType of(MessageType.Location mainPlayerLocation, PlayerMessageType.Receiver receiver) {
        return new PlayerMessageType(mainPlayerLocation, MessageType.Location.NONE, receiver);
    }

    /**
     * Creates a {@code PlayerMessageType} with {@link MessageType.Location#CHAT} for the
     * primary audience and {@link MessageType.Location#NONE} for all others.
     *
     * @param receiver the audience filter
     * @return a new {@code PlayerMessageType} instance
     */
    public static PlayerMessageType of(PlayerMessageType.Receiver receiver) {
        return new PlayerMessageType(MessageType.Location.CHAT, MessageType.Location.NONE, receiver);
    }

    /**
     * Creates a {@code PlayerMessageType} with no message for any audience.
     *
     * @return a new {@code PlayerMessageType} instance with all locations set to {@link MessageType.Location#NONE}
     */
    public static PlayerMessageType empty() {
        return new PlayerMessageType(MessageType.Location.NONE, MessageType.Location.NONE, PlayerMessageType.Receiver.NONE);
    }

    /**
     * Converts a ServerMessageType to a PlayerMessageType.
     *
     * @param serverMessageType the ServerMessageType to convert
     * @return a new PlayerMessageType with the converted values
     * @throws IllegalArgumentException if the receiver type is invalid (logs error and defaults to NONE)
     */
    public static PlayerMessageType fromServerMessageType(ServerMessageType serverMessageType) {
        MessageType.Location mainLocation = serverMessageType.mainPlayerLocation;
        MessageType.Location otherLocation = serverMessageType.otherPlayerLocation;
        PlayerMessageType.Receiver receiver;
        switch (serverMessageType.receiver) {
            case ALL:
                receiver = PlayerMessageType.Receiver.ALL;
                break;
            case OP:
                receiver = PlayerMessageType.Receiver.OP;
                break;
            case NONE:
                receiver = PlayerMessageType.Receiver.NONE;
                break;
            default:
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod:Invalid PlayerMessageType receiver: " + serverMessageType.receiver);
                receiver = PlayerMessageType.Receiver.NONE;
                break;
        }
        return new PlayerMessageType(mainLocation, otherLocation, receiver);
    }

    /**
     * Returns a new {@code PlayerMessageType} with the same locations but an updated receiver filter.
     *
     * @param newReceiver the new audience filter
     * @return a new {@code PlayerMessageType} instance with the updated receiver
     */
    public PlayerMessageType updateReceiver(PlayerMessageType.Receiver newReceiver) {
        return new PlayerMessageType(this.mainPlayerLocation, this.otherPlayerLocation, newReceiver);
    }

    /**
     * Returns a new {@code PlayerMessageType} with the same receiver filter but an updated main player location.
     *
     * @param newMainLocation the new display location for the primary audience
     * @return a new {@code PlayerMessageType} instance with the updated main player location
     */
    public PlayerMessageType updateMain(MessageType.Location newMainLocation) {
        return new PlayerMessageType(newMainLocation, this.otherPlayerLocation, this.receiver);
    }

    /**
     * Returns a new {@code PlayerMessageType} with the same receiver filter but an updated other player location.
     *
     * @param newOtherLocation the new display location for non-primary receivers
     * @return a new {@code PlayerMessageType} instance with the updated other player location
     */
    public PlayerMessageType updateOther(MessageType.Location newOtherLocation) {
        return new PlayerMessageType(this.mainPlayerLocation, newOtherLocation, this.receiver);
    }

    /**
     * Returns a localized and formatted text representation of a message receiver type for display in the UI.
     * @param method the message receiver type
     * @return a localized and formatted text representation of the message receiver type
     */
    public static MutableText getMessageReceiverI18n(PlayerMessageType.Receiver method) {
        switch (method) {
            case ALL:
                return Util.parseTranslatableText("fmod.message.type.toall").formatted(Formatting.YELLOW);
            case OP:
                return Util.parseTranslatableText("fmod.message.type.toop").formatted(Formatting.GOLD);
            case SELFOP:
                return Util.parseTranslatableText("fmod.message.type.toselfop").formatted(Formatting.YELLOW);
            case TEAMOP:
                return Util.parseTranslatableText("fmod.message.type.toteamop").formatted(Formatting.YELLOW);
            case TEAM:
                return Util.parseTranslatableText("fmod.message.type.toteam").formatted(Formatting.GREEN);
            case SELF:
                return Util.parseTranslatableText("fmod.message.type.toself").formatted(Formatting.GREEN);
            case NONE:
                return Util.parseTranslatableText("fmod.message.type.none").formatted(Formatting.RED);
            default:
                return Text.literal(method.toString());
        }
    }

    /**
     * Returns a localized and formatted text representation of this {@code PlayerMessageType} for display in the UI.
     *
     * @param type the PlayerMessageType to represent
     * @return a localized and formatted text representation of the PlayerMessageType
     */
    public static MutableText getMessageTypeI18n(PlayerMessageType type) {
        Text mainText = PlayerMessageType.getMessageLocationI18n(type.mainPlayerLocation);
        Text otherText = PlayerMessageType.getMessageLocationI18n(type.otherPlayerLocation);
        Text receiverText = PlayerMessageType.getMessageReceiverI18n(type.receiver);
        return Util.parseTranslatableText("fmod.message.type.option", mainText, otherText, receiverText);
    }

    /**
     * Returns true if the given object is equal to this {@code PlayerMessageType}.
     * Two instances are equal if they are of the same class and have equal
     * {@code mainPlayerLocation}, {@code otherPlayerLocation}, and {@code receiver}.
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
        PlayerMessageType other = (PlayerMessageType) obj;
        return mainPlayerLocation == other.mainPlayerLocation && otherPlayerLocation == other.otherPlayerLocation && receiver == other.receiver;
    }

    /**
     * Returns a hash code derived from {@code mainPlayerLocation}, {@code otherPlayerLocation},
     * and {@code receiver}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(mainPlayerLocation, otherPlayerLocation, receiver);
    }

    /**
     * Returns a string representation of this {@code PlayerMessageType}.
     *
     * @return a string in the form {@code PlayerMessageType{mainPlayerLocation=..., otherPlayerLocation=..., receiver=...}}
     */
    @Override
    public String toString() {
        return "PlayerMessageType{" +
               "mainPlayerLocation=" + mainPlayerLocation +
               ", otherPlayerLocation=" + otherPlayerLocation +
               ", receiver=" + receiver +
               '}';
    }
}
