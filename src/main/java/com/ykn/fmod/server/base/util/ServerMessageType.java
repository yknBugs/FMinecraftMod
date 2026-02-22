/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A {@link MessageType} specialization for server-wide events where the message
 * audience is determined by player permission level rather than team membership.
 * <p>
 * A single {@code ServerMessageType} field replaces both and is fully serializable by Gson:
 * <pre>
 * {
 *   "mainPlayerLocation": "CHAT",
 *   "otherPlayerLocation": "NONE",
 *   "receiver": "ALL"
 * }
 * </pre>
 * Use the {@link #of} factory methods to create instances.
 *
 * @see PlayerMessageType
 */
public class ServerMessageType extends MessageType {

    /**
     * Determines which players receive the {@code mainMessage} vs the {@code otherMessage}.
     *
     * @see ServerMessageType.Receiver
     */
    public final ServerMessageType.Receiver receiver;

    /**
     * Constructs a new {@code ServerMessageType}.
     *
     * @param mainPlayerLocation  display location for receivers matched by {@code receiver}
     * @param otherPlayerLocation display location for all other receivers
     * @param receiver            the audience filter; must not be null
     */
    protected ServerMessageType(MessageType.Location mainPlayerLocation, MessageType.Location otherPlayerLocation, ServerMessageType.Receiver receiver) {
        super(mainPlayerLocation, otherPlayerLocation);
        this.receiver = receiver;
    }

    /**
     * Defines who receives the main message in a server-wide broadcast.
     * <p>
     * Gson serializes each constant as its name string (e.g., {@code "OP"}).
     */
    public static enum Receiver {

        /**
         * Every online player receives the main message. 
         */
        ALL,

        /**
         * Only operators (permission level ≥ 2) and the console receive the main message;
         * other players receive the other message instead.
         */
        OP,

        /**
         * No player receives the main message; everyone receives the other message. 
         */
        NONE
    }

    /**
     * Posts a message to a single player according to this type's receiver filter.
     * <p>
     * The appropriate message variant ({@code mainMessage} or {@code otherMessage}) and
     * display location are chosen based on {@link #receiver} and the player's permission level.
     *
     * @param currentReceiver the player to send the message to, or {@code null} for the console
     * @param mainMessage     the message shown to the primary audience; must not be null
     * @param otherMessage    the fallback message shown to non-primary receivers; must not be null
     */
    public void postMessage(@Nullable ServerPlayerEntity currentReceiver, @NotNull Text mainMessage, @NotNull Text otherMessage) {
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
            case NONE:
                sendMessage(currentReceiver, this.otherPlayerLocation, otherMessage);
                break;
            default:
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Invalid receiver type: " + this.receiver);
                break;
        }
    }

    /**
     * Broadcasts a message to all online players on the server according to this type's
     * receiver filter.
     * <p>
     * Iterates over every online player and delegates to
     * {@link #postMessage(ServerPlayerEntity, Text, Text)} for each one.
     *
     * @param server       the Minecraft server instance; must not be null
     * @param mainMessage  the message shown to the primary audience; must not be null
     * @param otherMessage the fallback message shown to non-primary receivers; must not be null
     */
    public void postMessage(@NotNull MinecraftServer server, @NotNull Text mainMessage, @NotNull Text otherMessage) {
        for (ServerPlayerEntity player : Util.getOnlinePlayers(server)) {
            this.postMessage(player, mainMessage, otherMessage);
        }
        ServerPlayerEntity player = null;
        this.postMessage(player, mainMessage, otherMessage);
    }

    /**
    * Broadcasts a message to all online players on the server according to this type's
    * receiver filter, non-primary receivers will not receive any message.
    *
    * @param server      the Minecraft server instance; must not be null
    * @param mainMessage the message shown to all receivers; must not be null
    */
    public void postMessage(@NotNull MinecraftServer server, @NotNull Text mainMessage) {
        ServerMessageType type = this.updateOther(MessageType.Location.NONE);
        for (ServerPlayerEntity player : Util.getOnlinePlayers(server)) {
            type.postMessage(player, mainMessage, Text.empty());
        }
    }

    /**
     * Creates a {@code ServerMessageType} with independent locations for the main and
     * other audiences.
     *
     * @param mainPlayerLocation  display location for the primary audience
     * @param otherPlayerLocation display location for all other receivers
     * @param receiver            the audience filter
     * @return a new {@code ServerMessageType} instance
     */
    public static ServerMessageType of(MessageType.Location mainPlayerLocation, MessageType.Location otherPlayerLocation, ServerMessageType.Receiver receiver) {
        return new ServerMessageType(mainPlayerLocation, otherPlayerLocation, receiver);
    }

    /**
     * Creates a {@code ServerMessageType} where the other-audience location defaults to
     * {@link MessageType.Location#NONE}.
     *
     * @param mainPlayerLocation display location for the primary audience
     * @param receiver           the audience filter
     * @return a new {@code ServerMessageType} instance
     */
    public static ServerMessageType of(MessageType.Location mainPlayerLocation, ServerMessageType.Receiver receiver) {
        return new ServerMessageType(mainPlayerLocation, MessageType.Location.NONE, receiver);
    }

    /**
     * Creates a {@code ServerMessageType} with {@link MessageType.Location#CHAT} for the
     * primary audience and {@link MessageType.Location#NONE} for all others.
     *
     * @param receiver the audience filter
     * @return a new {@code ServerMessageType} instance
     */
    public static ServerMessageType of(ServerMessageType.Receiver receiver) {
        return new ServerMessageType(MessageType.Location.CHAT, MessageType.Location.NONE, receiver);
    }

    /**
     * Creates a {@code ServerMessageType} with no message for any audience.
     *
     * @return a new {@code ServerMessageType} instance with all locations set to {@link MessageType.Location#NONE}
     */
    public static ServerMessageType empty() {
        return new ServerMessageType(MessageType.Location.NONE, MessageType.Location.NONE, ServerMessageType.Receiver.NONE);
    }

    /**
     * Converts a {@link PlayerMessageType} to a {@link ServerMessageType}.
     *
     * @param playerMessageType the {@link PlayerMessageType} to convert
     * @return a new {@link ServerMessageType} instance with the converted properties
     * @throws IllegalArgumentException if the receiver type is invalid (defaults to NONE and logs an error)
     */
    public static ServerMessageType fromPlayerMessageType(PlayerMessageType playerMessageType) {
        MessageType.Location mainLocation = playerMessageType.mainPlayerLocation;
        MessageType.Location otherLocation = playerMessageType.otherPlayerLocation;
        ServerMessageType.Receiver receiver;
        switch (playerMessageType.receiver) {
            case ALL:
                receiver = ServerMessageType.Receiver.ALL;
                break;
            case OP:
                receiver = ServerMessageType.Receiver.OP;
                break;
            case NONE:
                receiver = ServerMessageType.Receiver.NONE;
                break;
            default:
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Invalid PlayerMessageType receiver: " + playerMessageType.receiver);
                receiver = ServerMessageType.Receiver.NONE;
                break;
        }
        return new ServerMessageType(mainLocation, otherLocation, receiver);
    }

    /**
     * Returns a new {@code ServerMessageType} with the same player locations as this one but
     * a different receiver filter.
     *
     * @param newReceiver the new receiver filter; must not be null
     * @return a new {@code ServerMessageType} instance with the updated receiver
     */
    public ServerMessageType updateReceiver(ServerMessageType.Receiver newReceiver) {
        return new ServerMessageType(this.mainPlayerLocation, this.otherPlayerLocation, newReceiver);
    }

    /**
     * Returns a new {@code ServerMessageType} with the same receiver filter as this one but
     * a different main player location.
     *
     * @param newMainLocation the new main player location; must not be null
     * @return a new {@code ServerMessageType} instance with the updated main player location
     */
    public ServerMessageType updateMain(MessageType.Location newMainLocation) {
        return new ServerMessageType(newMainLocation, this.otherPlayerLocation, this.receiver);
    }

    /**
     * Returns a new {@code ServerMessageType} with the same receiver filter as this one but
     * a different other player location.
     *
     * @param newOtherLocation the new other player location; must not be null
     * @return a new {@code ServerMessageType} instance with the updated other player location
     */
    public ServerMessageType updateOther(MessageType.Location newOtherLocation) {
        return new ServerMessageType(this.mainPlayerLocation, newOtherLocation, this.receiver);
    }

    /**
     * Returns a localized and formatted text representation of a message receiver type for display in the UI.
     * @param method the message receiver type
     * @return a localized and formatted text representation of the message receiver type
     */
    public static MutableText getMessageReceiverI18n(ServerMessageType.Receiver method) {
        switch (method) {
            case ALL:
                return Util.parseTranslatableText("fmod.message.type.toall").formatted(Formatting.YELLOW);
            case OP:
                return Util.parseTranslatableText("fmod.message.type.toop").formatted(Formatting.GOLD);
            case NONE:
                return Util.parseTranslatableText("fmod.message.type.none").formatted(Formatting.RED);
            default:
                return Text.literal(method.toString());
        }
    }

    /**
     * Returns a localized and formatted text representation of this {@code ServerMessageType} for display in the UI.
     *
     * @param type the {@code ServerMessageType} to represent
     * @return a localized and formatted text representation of the message type
     */
    public static MutableText getMessageTypeI18n(ServerMessageType type) {
        Text mainText = ServerMessageType.getMessageLocationI18n(type.mainPlayerLocation);
        Text otherText = ServerMessageType.getMessageLocationI18n(type.otherPlayerLocation);
        Text receiverText = ServerMessageType.getMessageReceiverI18n(type.receiver);
        return Util.parseTranslatableText("fmod.message.type.option", mainText, otherText, receiverText);
    }

    /**
     * Returns true if the given object is equal to this {@code ServerMessageType}.
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
        ServerMessageType other = (ServerMessageType) obj;
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
     * Returns a string representation of this {@code ServerMessageType}.
     *
     * @return a string in the form {@code ServerMessageType{mainPlayerLocation=..., otherPlayerLocation=..., receiver=...}}
     */
    @Override
    public String toString() {
        return "ServerMessageType{" +
               "mainPlayerLocation=" + mainPlayerLocation +
               ", otherPlayerLocation=" + otherPlayerLocation +
               ", receiver=" + receiver +
               '}';
    }
}
