/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.logic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.text.Text;

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
    private Text messageText;

    /**
     * Constructs a LogicException with the specified details.
     * <p>
     * The message parameter will attempt to extract a string message in the following order:
     * 1. Direct string message parameter
     * 2. Text component converted to string
     * 3. Exception message from the reason throwable
     * 4. null if none of the above are available
     * 
     * @param reason the underlying exception that caused this error, may be null
     * @param messageText the user-facing message as a Text object, may be null
     * @param message the string message for standard exception handling, may be null
     */
    public LogicException(@Nullable Exception reason, @Nullable Text messageText, @Nullable String message) {
        super(parseExceptionMessage(reason, messageText, message));
        this.reason = reason;
        this.messageText = messageText;
    }

    /**
     * Parses and extracts an exception message from the provided parameters.
     * <p>
     * This method attempts to retrieve a message in the following priority order:
     * 1. Direct string message parameter
     * 2. Text component converted to string
     * 3. Exception message from the reason throwable
     * 4. null if none of the above are available
     * 
     * @param reason the exception to extract a message from, may be null
     * @param messageText the text component containing a message, may be null
     * @param message the string message, may be null
     * @return the parsed exception message, or null if no message is available
     */
    private static String parseExceptionMessage(@Nullable Exception reason, @Nullable Text messageText, @Nullable String message) {
        if (message != null) {
            return message;
        } else if (messageText != null) {
            return messageText.getString();
        } else if (reason != null) {
            return reason.getMessage();
        } else {
            return null;
        }
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
     * Gets the user-facing message as a Minecraft Text object.
     * <p>
     * This message is intended to be displayed to players in a localized format.
     * If no Text message was provided, it attempts to convert the string message
     * or the reason's message into a Text object.
     * 
     * @return The user-facing message as Text.
     */
    @NotNull
    public Text getMessageText() {
        if (messageText != null) {
            return messageText;
        } else if (getMessage() != null) {
            return Text.literal(getMessage());
        } else if (reason != null && reason.getMessage() != null) {
            return Text.literal(reason.getMessage());
        } else {
            return Util.parseTranslatableText("fmod.flow.error.unknown");
        }
    }
}
