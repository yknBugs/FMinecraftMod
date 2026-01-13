package com.ykn.fmod.server.flow.logic;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;

/**
 * Thrown when an exception occurs when executing a logic flow
 * Since logic flows are built by users, such kind of exceptions are expected and should be always sent back to users.
 */
public class LogicException extends Exception {

    /**
     * The raw exception that interrupted the logic flow execution
     */
    private Exception reason;

    /**
     * The message text to be sent to users. Can be a translatable text.
     */
    private Component messageText;

    
    public LogicException(@Nullable Exception reason, @Nullable Component messageText, @Nullable String message) {
        super((message == null && messageText != null) ? messageText.getString() : message);
        this.reason = reason;
        this.messageText = messageText;
    }

    @Nullable
    public Exception getRawException() {
        return reason;
    }

    @Nullable
    public Component getMessageText() {
        return messageText;
    }
}
