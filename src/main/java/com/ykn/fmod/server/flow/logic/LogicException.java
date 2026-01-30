/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.logic;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;

/**
 * Exception thrown when an error occurs during logic flow execution.
 * <p>
 * Since logic flows are user-created, these exceptions are expected and should always
 * be communicated back to the user in a friendly, translatable format. This exception
 * wraps both technical error details and user-facing messages.
 * <p>
 * Key features:
 * <ul>
 *   <li>Stores the underlying exception cause for debugging</li>
 *   <li>Provides a Minecraft Text object for localized user messages</li>
 *   <li>Can be used with or without an underlying exception</li>
 *   <li>Automatically converts Text to string for standard exception handling</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * throw new LogicException(
 *     originalException,
 *     Util.parseTranslatableText("fmod.flow.error.nullinput", nodeName),
 *     null
 * );
 * </pre>
 * 
 * @see ExecutionContext
 * @see FlowNode#execute(ExecutionContext)
 */
public class LogicException extends Exception {

    /**
     * The underlying exception that caused the logic flow to fail, if any.
     * <p>
     * This provides technical details for debugging and logging purposes.
     * May be null if the error doesn't have an underlying exception (e.g., validation errors).
     */
    private Exception reason;

    /**
     * The user-facing message text in Minecraft's Text format.
     * <p>
     * This should be a translatable text object that can be displayed to players
     * in their preferred language. May be null if only a plain string message is needed.
     */
    private Component messageText;

    /**
     * Creates a new LogicException with the specified details.
     * <p>
     * At least one of messageText or message should be provided. If both are provided,
     * the message parameter takes precedence. If only messageText is provided, it will
     * be converted to a string for the exception message.
     * 
     * @param reason The underlying exception that caused this error, or null if not applicable
     * @param messageText The user-facing translatable text message, or null if using plain string
     * @param message The plain string message, or null to use messageText
     */
    public LogicException(@Nullable Exception reason, @Nullable Component messageText, @Nullable String message) {
        super((message == null && messageText != null) ? messageText.getString() : message);
        this.reason = reason;
        this.messageText = messageText;
    }

    /**
     * Gets the underlying exception that caused this logic flow error.
     * <p>
     * This is useful for debugging and logging the technical details of the error.
     * 
     * @return The underlying exception, or null if this error doesn't wrap another exception
     */
    @Nullable
    public Exception getRawException() {
        return reason;
    }

    /**
     * Gets the user-facing message text in Minecraft's Text format.
     * <p>
     * This Text object can be sent directly to players and will be displayed
     * in their preferred language if it's a translatable text.
     * 
     * @return The Text message for display to users, or null if only a plain string message exists
     */
    @Nullable
    public Component getMessageText() {
        return messageText;
    }
}
