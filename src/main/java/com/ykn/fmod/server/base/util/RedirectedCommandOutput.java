/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * A {@link CommandSource} implementation that redirects and stores command messages
 * instead of sending them to the standard output. This class captures all messages
 * sent through the {@link #sendMessage(Component)} method for later retrieval.
 */
public class RedirectedCommandOutput implements CommandSource {

    /**
     * List of all messages that have been captured by this redirected output.
     */
    private final ArrayList<Component> messages;

    /**
     * Constructs a new {@code RedirectedCommandOutput} with an empty message list.
     */
    private RedirectedCommandOutput() {
        this.messages = new ArrayList<>();
    }

    /**
     * Adds a message to the list of captured messages.
     *
     * @param var1 the {@link Component} message to capture
     */
    @Override
    public void sendSystemMessage(Component var1) {
        this.messages.add(var1);
    }

    /**
     * Indicates that this output should receive feedback from commands.
     *
     * @return {@code true} to enable receiving feedback
     */
    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    /**
     * Indicates that command output should be tracked by this output.
     *
     * @return {@code true} to enable output tracking
     */
    @Override
    public boolean acceptsFailure() {
        return true;
    }

    /**
     * Indicates whether console output should be broadcast to operators.
     *
     * @return {@code false} to disable broadcasting to ops
     */
    @Override
    public boolean shouldInformAdmins() {
        return false;
    }
    
    /**
     * Retrieves the last message that was captured, or {@code null} if no messages
     * have been captured.
     *
     * @return the last {@link Component} message, or {@code null} if the message list is empty
     */
    @Nullable
    public Component getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    /**
     * Retrieves all captured messages as a single {@link Component} object with messages
     * separated by newlines.
     *
     * @return a {@link Component} object containing all captured messages
     */
    @Nonnull
    public Component getAllMessage() {
        MutableComponent result = Component.empty();
        for (Component message : messages) {
            result.append(message).append("\n");
        }
        return result;
    }

    /**
     * Retrieves the string representation of the last captured message, or {@code null}
     * if no messages have been captured.
     *
     * @return the string content of the last message, or {@code null} if the message list is empty
     */
    @Nullable
    public String getLastOutput() {
        if (messages.isEmpty()) {
            return null;
        }
        return getLastMessage().getString();
    }

    /**
     * Retrieves all captured messages as a single string with messages separated
     * by newlines.
     *
     * @return a string containing all message contents
     */
    @Nonnull
    public String getRawOutput() {
        StringBuilder builder = new StringBuilder();
        for (Component message : messages) {
            builder.append(message.getString()).append("\n");
        }
        return builder.toString();
    }

    /**
     * Factory method to create a new instance of {@code RedirectedCommandOutput}.
     *
     * @return a new {@code RedirectedCommandOutput} instance
     */
    public static RedirectedCommandOutput create() {
        return new RedirectedCommandOutput();
    }
}
