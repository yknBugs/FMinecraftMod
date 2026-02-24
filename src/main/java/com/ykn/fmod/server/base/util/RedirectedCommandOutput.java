/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.command.CommandOutput;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * A {@link CommandOutput} implementation that redirects and stores command messages
 * instead of sending them to the standard output. This class captures all messages
 * sent through the {@link #sendMessage(Text)} method for later retrieval.
 */
public class RedirectedCommandOutput implements CommandOutput {

    /**
     * List of all messages that have been captured by this redirected output.
     */
    private final ArrayList<Text> messages;

    /**
     * Constructs a new {@code RedirectedCommandOutput} with an empty message list.
     */
    private RedirectedCommandOutput() {
        this.messages = new ArrayList<>();
    }

    /**
     * Adds a message to the list of captured messages.
     *
     * @param var1 the {@link Text} message to capture
     */
    @Override
    public void sendMessage(Text var1) {
        this.messages.add(var1);
    }

    /**
     * Indicates that this output should receive feedback from commands.
     *
     * @return {@code true} to enable receiving feedback
     */
    @Override
    public boolean shouldReceiveFeedback() {
        return true;
    }

    /**
     * Indicates that command output should be tracked by this output.
     *
     * @return {@code true} to enable output tracking
     */
    @Override
    public boolean shouldTrackOutput() {
        return true;
    }

    /**
     * Indicates whether console output should be broadcast to operators.
     *
     * @return {@code false} to disable broadcasting to ops
     */
    @Override
    public boolean shouldBroadcastConsoleToOps() {
        return false;
    }
    
    /**
     * Retrieves the last message that was captured, or {@code null} if no messages
     * have been captured.
     *
     * @return the last {@link Text} message, or {@code null} if the message list is empty
     */
    @Nullable
    public Text getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    /**
     * Retrieves all captured messages as a single {@link Text} object with messages
     * separated by newlines.
     *
     * @return a {@link Text} object containing all captured messages
     */
    @NotNull
    public Text getAllMessage() {
        MutableText result = Text.empty();
        for (Text message : messages) {
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
    @NotNull
    public String getRawOutput() {
        StringBuilder builder = new StringBuilder();
        for (Text message : messages) {
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
