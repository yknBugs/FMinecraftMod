/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.util.PlayerMessageType;
import com.ykn.fmod.server.base.util.ServerMessageType;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.command.CommandException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Central registry that discovers, stores, and exposes all {@link ConfigEntry}-annotated
 * fields found in registered {@link ConfigReader} implementations.
 *
 * <p>The registry fulfils three responsibilities:</p>
 * <ol>
 *   <li><b>Registration</b> – {@link #register(ConfigReader)} scans a {@link ConfigReader}
 *       instance via reflection and records every field annotated with {@link ConfigEntry}.</li>
 *   <li><b>Command generation</b> – {@link #buildCommand()} and {@link #buildCommand(String)}
 *       construct Brigadier {@code /f options} command nodes for each registered entry,
 *       covering all supported {@link ConfigEntry.ConfigType} variants.</li>
 *   <li><b>Runtime value access</b> – {@link #getValue(String)}, {@link #setValue(String, Object)},
 *       {@link #getDisplayValue(String, Object)}, and {@link #toTrueValue(String, Object)}
 *       provide reflective read/write access to the underlying config fields using the
 *       standard getter/setter naming convention.</li>
 * </ol>
 *
 * <p>All entries are stored in insertion-order {@link LinkedHashMap}s so that command
 * nodes and UI widgets are generated in the same order as they were registered.</p>
 *
 * @see ConfigEntry
 * @see ConfigReader
 */
public class ServerConfigRegistry {

    /** 
     * Maps every registered code-entry name to its {@link ConfigEntry} annotation metadata. 
     */
    private static final Map<String, ConfigEntry> configAnnotations = new LinkedHashMap<>();

    /** 
     * Maps every registered code-entry name to the {@link ConfigReader} instance that owns it. 
     */
    private static final Map<String, ConfigReader> configInstances = new LinkedHashMap<>();

    /**
     * Scans all declared fields of the given {@link ConfigReader} instance for
     * {@link ConfigEntry} annotations and registers each one with the registry.
     *
     * <p>For every annotated field the effective code-entry key is resolved as follows:
     * if {@link ConfigEntry#codeEntry()} is non-empty it is used; otherwise the field
     * name is used. If the resolved key already exists in the registry a warning is
     * logged and the previous entry is overwritten.</p>
     *
     * @param config the {@link ConfigReader} whose fields should be scanned and registered;
     *               must not be {@code null}
     */
    public static void register(ConfigReader config) {
        try {
            for (Field field : config.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(ConfigEntry.class)) {
                    field.setAccessible(true);
                    ConfigEntry annotation = field.getAnnotation(ConfigEntry.class);
                    String codeEntry = annotation.codeEntry().isEmpty() ? field.getName() : annotation.codeEntry();
                    if (configAnnotations.containsKey(codeEntry) || configInstances.containsKey(codeEntry)) {
                        LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Duplicate config entry name detected: " + codeEntry + " in class " + config.getClass().getName() + ", overwriting previous entry.");
                    }
                    configAnnotations.put(codeEntry, annotation);
                    configInstances.put(codeEntry, config);
                }
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Failed to register config entries for " + config.getClass().getName(), e);
        }
    }
    
    /**
     * Returns the {@link ConfigEntry} annotation metadata associated with the given
     * code-entry name, or {@code null} if no entry with that name has been registered.
     *
     * @param codeEntry the code-entry name to look up
     * @return the corresponding {@link ConfigEntry}, or {@code null} if not found
     */
    @Nullable
    public static ConfigEntry getAnnotation(String codeEntry) {
        return configAnnotations.get(codeEntry);
    }

    /**
     * Builds the root {@code options} Brigadier command node that groups sub-commands
     * for every currently registered config entry.
     *
     * <p>The generated node is {@code /f options} and requires permission level 4 (operator).
     * Each registered entry is appended as a child literal via {@link #buildCommand(String)}.</p>
     *
     * @return a fully constructed {@link LiteralArgumentBuilder} for {@code /f options}
     * @throws Exception if any per-entry command node cannot be built
     * @see #buildCommand(String)
     */
    @NotNull
    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand() throws Exception {
        LiteralArgumentBuilder<ServerCommandSource> commandNode = CommandManager.literal("options").requires(source -> source.hasPermissionLevel(4));
        for (String codeEntry : configAnnotations.keySet()) {
            commandNode = commandNode.then(buildCommand(codeEntry));
        }
        return commandNode;
    }

    /**
     * Builds the Brigadier command node for a single registered config entry.
     *
     * <p>The shape of the generated sub-tree depends on {@link ConfigEntry#type()}:</p>
     * <ul>
     *   <li>{@link ConfigEntry.ConfigType#DOUBLE} – {@code <commandEntry> [<hint>: double]}</li>
     *   <li>{@link ConfigEntry.ConfigType#INTEGER} – {@code <commandEntry> [<hint>: int]}</li>
     *   <li>{@link ConfigEntry.ConfigType#STRING} – {@code <commandEntry> [<hint>: greedyString]}</li>
     *   <li>{@link ConfigEntry.ConfigType#BOOLEAN} – {@code <commandEntry> [<hint>: bool]}</li>
     *   <li>{@link ConfigEntry.ConfigType#SERVERMESSAGE} – {@code <commandEntry> (main|other|receiver) <...>}</li>
     *   <li>{@link ConfigEntry.ConfigType#PLAYERMESSAGE} – {@code <commandEntry> (main|other|receiver) <...>}</li>
     * </ul>
     * <p>Calling the node without a value argument queries the current value; calling it with
     * a value argument sets the new value and broadcasts feedback.</p>
     *
     * @param codeEntry the code-entry name of the config entry to build a command for
     * @return a fully constructed {@link LiteralArgumentBuilder} for this entry's sub-command
     * @throws IllegalStateException if no entry with the given name is registered
     * @throws UnsupportedOperationException if the entry's {@link ConfigEntry.ConfigType} is not handled by this method
     */
    @NotNull
    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(String codeEntry) throws Exception {
        ConfigEntry configAnnotation = configAnnotations.get(codeEntry);
        if (configAnnotation == null) {
            throw new IllegalStateException("No config entry found for name: " + codeEntry);
        }
        String commandValueHint = configAnnotation.commandValueHint().isEmpty() ? "value" : configAnnotation.commandValueHint();
        LiteralArgumentBuilder<ServerCommandSource> commandNode = CommandManager.literal(configAnnotation.commandEntry().isEmpty() ? codeEntry : configAnnotation.commandEntry());
        switch (configAnnotation.type()) {
            case DOUBLE:
                {
                    double min = configAnnotation.minCommandDouble();
                    double max = configAnnotation.maxCommandDouble();
                    commandNode = commandNode.then(CommandManager.argument(commandValueHint, DoubleArgumentType.doubleArg(min, max))
                        .executes(context -> {return runOptionsCommand(codeEntry, DoubleArgumentType.getDouble(context, commandValueHint), context);})
                    ).executes(context -> {return runOptionsCommand(codeEntry, null, context);});
                }
                break;
            case INTEGER:
                {
                    int min = configAnnotation.minCommandInt();
                    int max = configAnnotation.maxCommandInt();
                    commandNode = commandNode.then(CommandManager.argument(commandValueHint, IntegerArgumentType.integer(min, max))
                        .executes(context -> {return runOptionsCommand(codeEntry, IntegerArgumentType.getInteger(context, commandValueHint), context);})
                    ).executes(context -> {return runOptionsCommand(codeEntry, null, context);});
                }
                break;
            case STRING:
                {
                    commandNode = commandNode.then(CommandManager.argument(commandValueHint, StringArgumentType.greedyString())
                        .executes(context -> {return runOptionsCommand(codeEntry, StringArgumentType.getString(context, commandValueHint), context);})
                    ).executes(context -> {return runOptionsCommand(codeEntry, null, context);});
                }
                break;
            case BOOLEAN:
                {
                    commandNode = commandNode.then(CommandManager.argument(commandValueHint, BoolArgumentType.bool())
                        .executes(context -> {return runOptionsCommand(codeEntry, BoolArgumentType.getBool(context, commandValueHint), context);})
                    ).executes(context -> {return runOptionsCommand(codeEntry, null, context);});
                }
                break;
            case SERVERMESSAGE:
                {
                    commandNode = commandNode.then(CommandManager.literal("main")
                        .then(CommandManager.literal("off").executes(context -> {return runServerMessageTypeOptionsCommand(codeEntry, "main", ServerMessageType.Location.NONE, context);}))
                        .then(CommandManager.literal("chat").executes(context -> {return runServerMessageTypeOptionsCommand(codeEntry, "main", ServerMessageType.Location.CHAT, context);}))
                        .then(CommandManager.literal("actionbar").executes(context -> {return runServerMessageTypeOptionsCommand(codeEntry, "main", ServerMessageType.Location.ACTIONBAR, context);}))
                    ).then(CommandManager.literal("other")
                        .then(CommandManager.literal("off").executes(context -> {return runServerMessageTypeOptionsCommand(codeEntry, "other", ServerMessageType.Location.NONE, context);}))
                        .then(CommandManager.literal("chat").executes(context -> {return runServerMessageTypeOptionsCommand(codeEntry, "other", ServerMessageType.Location.CHAT, context);}))
                        .then(CommandManager.literal("actionbar").executes(context -> {return runServerMessageTypeOptionsCommand(codeEntry, "other", ServerMessageType.Location.ACTIONBAR, context);}))
                    ).then(CommandManager.literal("receiver")
                        .then(CommandManager.literal("all").executes(context -> {return runServerMessageTypeOptionsCommand(codeEntry, "receiver", ServerMessageType.Receiver.ALL, context);}))
                        .then(CommandManager.literal("op").executes(context -> {return runServerMessageTypeOptionsCommand(codeEntry, "receiver", ServerMessageType.Receiver.OP, context);}))
                        .then(CommandManager.literal("none").executes(context -> {return runServerMessageTypeOptionsCommand(codeEntry, "receiver", ServerMessageType.Receiver.NONE, context);}))
                    ).executes(context -> {return runServerMessageTypeOptionsCommand(codeEntry, null, null, context);});
                }
                break;
            case PLAYERMESSAGE:
                {
                    commandNode = commandNode.then(CommandManager.literal("main")
                        .then(CommandManager.literal("off").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "main", PlayerMessageType.Location.NONE, context);}))
                        .then(CommandManager.literal("chat").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "main", PlayerMessageType.Location.CHAT, context);}))
                        .then(CommandManager.literal("actionbar").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "main", PlayerMessageType.Location.ACTIONBAR, context);}))
                    ).then(CommandManager.literal("other")
                        .then(CommandManager.literal("off").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "other", PlayerMessageType.Location.NONE, context);}))
                        .then(CommandManager.literal("chat").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "other", PlayerMessageType.Location.CHAT, context);}))
                        .then(CommandManager.literal("actionbar").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "other", PlayerMessageType.Location.ACTIONBAR, context);}))
                    ).then(CommandManager.literal("receiver")
                        .then(CommandManager.literal("all").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "receiver", PlayerMessageType.Receiver.ALL, context);}))
                        .then(CommandManager.literal("op").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "receiver", PlayerMessageType.Receiver.OP, context);}))
                        .then(CommandManager.literal("selfop").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "receiver", PlayerMessageType.Receiver.SELFOP, context);}))
                        .then(CommandManager.literal("teamop").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "receiver", PlayerMessageType.Receiver.TEAMOP, context);}))
                        .then(CommandManager.literal("team").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "receiver", PlayerMessageType.Receiver.TEAM, context);}))
                        .then(CommandManager.literal("self").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "receiver", PlayerMessageType.Receiver.SELF, context);}))
                        .then(CommandManager.literal("none").executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, "receiver", PlayerMessageType.Receiver.NONE, context);}))
                    ).executes(context -> {return runPlayerMessageTypeOptionsCommand(codeEntry, null, null, context);});
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported config type for command generation: " + configAnnotation.type());
        }
        return commandNode;
    }

    /**
     * Executes the options command for a {@link ConfigEntry.ConfigType#SERVERMESSAGE}-typed entry.
     *
     * <p>When {@code field} and {@code value} are both {@code null} the current value is
     * printed as feedback. Otherwise the specified sub-field of the
     * {@link com.ykn.fmod.server.base.util.ServerMessageType} is updated:</p>
     * <ul>
     *   <li>{@code "main"} – updates {@link com.ykn.fmod.server.base.util.ServerMessageType#mainPlayerLocation}</li>
     *   <li>{@code "other"} – updates {@link com.ykn.fmod.server.base.util.ServerMessageType#otherPlayerLocation}</li>
     *   <li>{@code "receiver"} – updates {@link com.ykn.fmod.server.base.util.ServerMessageType#receiver}</li>
     * </ul>
     *
     * @param codeEntry the code-entry name of the config entry being modified
     * @param field     the sub-field to update ({@code "main"}, {@code "other"}, {@code "receiver"}),
     *                  or {@code null} to query the current value
     * @param value     the new value for the specified sub-field, or {@code null} to query only
     * @param context   the Brigadier command context providing the command source
     * @return {@link com.mojang.brigadier.Command#SINGLE_SUCCESS} on success
     * @throws net.minecraft.command.CommandException if the entry is unknown, the cast fails,
     *         or the registry update fails
     */
    public static int runServerMessageTypeOptionsCommand(String codeEntry, String field, Object value, CommandContext<ServerCommandSource> context) {
        try {
            final ConfigEntry configAnnotation = configAnnotations.get(codeEntry);
            final Object currentValue = getValue(codeEntry);
            if (currentValue == null || configAnnotation == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.options.unknownoption", codeEntry));
            }
            String i18nEntry = configAnnotation.i18nEntry().isEmpty() ? codeEntry : configAnnotation.i18nEntry();
            final ServerMessageType currentMessageType = (ServerMessageType) currentValue;
            String i18nKey = "fmod.options." + i18nEntry;
            if (field == null || value == null) {
                Text displayValue = ServerMessageType.getMessageTypeI18n(currentMessageType);
                Text feedbackMessage = Util.parseTranslatableText("fmod.command.options.get", Util.parseTranslatableText(i18nKey), displayValue);
                context.getSource().sendFeedback(() -> feedbackMessage, false);
                return Command.SINGLE_SUCCESS;
            }

            Text displayValue = Text.empty();
            boolean isSuccess = false;
            switch (field) {
                case "main":
                    isSuccess = setValue(codeEntry, currentMessageType.updateMain((ServerMessageType.Location) value));
                    displayValue = ServerMessageType.getMessageLocationI18n((ServerMessageType.Location) value);
                    break;
                case "other":
                    isSuccess = setValue(codeEntry, currentMessageType.updateOther((ServerMessageType.Location) value));
                    displayValue = ServerMessageType.getMessageLocationI18n((ServerMessageType.Location) value);
                    break;
                case "receiver":
                    isSuccess = setValue(codeEntry, currentMessageType.updateReceiver((ServerMessageType.Receiver) value));
                    displayValue = ServerMessageType.getMessageReceiverI18n((ServerMessageType.Receiver) value);
                    break;
                default:
                    throw new CommandException(Util.parseTranslatableText("fmod.command.options.unknownoption", codeEntry));
            }
            if (!isSuccess) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
            }
            Text feedbackMessage = Util.parseTranslatableText("fmod.command.options.set", Util.parseTranslatableText(i18nKey), displayValue);
            context.getSource().sendFeedback(() -> feedbackMessage, true);
            Util.saveServerConfig();
        } catch (CommandException e) {
            throw e;
        } catch (ClassCastException e) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.options.classcast", value, codeEntry, e.getMessage()));
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f options " + codeEntry, e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Executes the options command for a {@link ConfigEntry.ConfigType#PLAYERMESSAGE}-typed entry.
     *
     * <p>When {@code field} and {@code value} are both {@code null} the current value is
     * printed as feedback. Otherwise the specified sub-field of the
     * {@link com.ykn.fmod.server.base.util.PlayerMessageType} is updated:</p>
     * <ul>
     *   <li>{@code "main"} – updates {@link com.ykn.fmod.server.base.util.PlayerMessageType#mainPlayerLocation}</li>
     *   <li>{@code "other"} – updates {@link com.ykn.fmod.server.base.util.PlayerMessageType#otherPlayerLocation}</li>
     *   <li>{@code "receiver"} – updates {@link com.ykn.fmod.server.base.util.PlayerMessageType#receiver}</li>
     * </ul>
     *
     * @param codeEntry the code-entry name of the config entry being modified
     * @param field     the sub-field to update ({@code "main"}, {@code "other"}, {@code "receiver"}),
     *                  or {@code null} to query the current value
     * @param value     the new value for the specified sub-field, or {@code null} to query only
     * @param context   the Brigadier command context providing the command source
     * @return {@link com.mojang.brigadier.Command#SINGLE_SUCCESS} on success
     * @throws net.minecraft.command.CommandException if the entry is unknown, the cast fails,
     *         or the registry update fails
     */
    public static int runPlayerMessageTypeOptionsCommand(String codeEntry, String field, Object value, CommandContext<ServerCommandSource> context) {
        try {
            final ConfigEntry configAnnotation = configAnnotations.get(codeEntry);
            final Object currentValue = getValue(codeEntry);
            if (currentValue == null || configAnnotation == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.options.unknownoption", codeEntry));
            }
            String i18nEntry = configAnnotation.i18nEntry().isEmpty() ? codeEntry : configAnnotation.i18nEntry();
            final PlayerMessageType currentMessageType = (PlayerMessageType) currentValue;
            String i18nKey = "fmod.options." + i18nEntry;
            if (field == null || value == null) {
                Text displayValue = PlayerMessageType.getMessageTypeI18n(currentMessageType);
                Text feedbackMessage = Util.parseTranslatableText("fmod.command.options.get", Util.parseTranslatableText(i18nKey), displayValue);
                context.getSource().sendFeedback(() -> feedbackMessage, false);
                return Command.SINGLE_SUCCESS;
            }

            Text displayValue = Text.empty();
            boolean isSuccess = false;
            switch (field) {
                case "main":
                    isSuccess = setValue(codeEntry, currentMessageType.updateMain((PlayerMessageType.Location) value));
                    displayValue = PlayerMessageType.getMessageLocationI18n((PlayerMessageType.Location) value);
                    break;
                case "other":
                    isSuccess = setValue(codeEntry, currentMessageType.updateOther((PlayerMessageType.Location) value));
                    displayValue = PlayerMessageType.getMessageLocationI18n((PlayerMessageType.Location) value);
                    break;
                case "receiver":
                    isSuccess = setValue(codeEntry, currentMessageType.updateReceiver((PlayerMessageType.Receiver) value));
                    displayValue = PlayerMessageType.getMessageReceiverI18n((PlayerMessageType.Receiver) value);
                    break;
                default:
                    throw new CommandException(Util.parseTranslatableText("fmod.command.options.unknownoption", codeEntry));
            }
            if (!isSuccess) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
            }
            Text feedbackMessage = Util.parseTranslatableText("fmod.command.options.set", Util.parseTranslatableText(i18nKey), displayValue);
            context.getSource().sendFeedback(() -> feedbackMessage, true);
            Util.saveServerConfig();
        } catch (CommandException e) {
            throw e;
        } catch (ClassCastException e) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.options.classcast", value, codeEntry, e.getMessage()));
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f options " + codeEntry, e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Executes the options command for simple config entry types
     * ({@link ConfigEntry.ConfigType#DOUBLE}, {@link ConfigEntry.ConfigType#INTEGER},
     * {@link ConfigEntry.ConfigType#STRING}, {@link ConfigEntry.ConfigType#BOOLEAN}).
     *
     * <p>If {@code value} is {@code null} the current config value is queried and sent
     * back as feedback to the command source. If {@code value} is non-{@code null} it is
     * first converted via {@link #toTrueValue(String, Object)} and then stored via
     * {@link #setValue(String, Object)}, after which the updated value is sent as feedback.</p>
     *
     * @param codeEntry the code-entry name of the config entry to read or update
     * @param value     the new value to set, or {@code null} to only query the current value
     * @param context   the Brigadier command context providing the command source
     * @return {@link com.mojang.brigadier.Command#SINGLE_SUCCESS} on success
     * @throws net.minecraft.command.CommandException if the entry is unknown, the type cast
     *         fails, or the registry update fails
     */
    public static int runOptionsCommand(String codeEntry, Object value, CommandContext<ServerCommandSource> context) {
        try {
            ConfigEntry configAnnotation = configAnnotations.get(codeEntry);
            if (configAnnotation == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.options.unknownoption", codeEntry));
            }
            String i18nEntry = configAnnotation.i18nEntry().isEmpty() ? codeEntry : configAnnotation.i18nEntry();
            String i18nKey = "fmod.options." + i18nEntry;
            if (value == null) {
                Object configValue = getValue(codeEntry);
                Text displayValue = getDisplayValue(codeEntry, configValue);
                Text feedbackMessage = Util.parseTranslatableText("fmod.command.options.get", Util.parseTranslatableText(i18nKey), displayValue);
                context.getSource().sendFeedback(() -> feedbackMessage, false);
            } else {
                Object valueToSet = toTrueValue(codeEntry, value);
                boolean isSuccess = setValue(codeEntry, valueToSet);
                if (!isSuccess) {
                    throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
                }
                Object newValue = getValue(codeEntry);
                Text displayValue = getDisplayValue(codeEntry, newValue);
                Text feedbackMessage = Util.parseTranslatableText("fmod.command.options.set", Util.parseTranslatableText(i18nKey), displayValue);
                context.getSource().sendFeedback(() -> feedbackMessage, true);
                Util.saveServerConfig();
            }
        } catch (CommandException e) {
            throw e;
        } catch (ClassCastException e) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.options.classcast", value, codeEntry, e.getMessage()));
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f options " + codeEntry, e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Retrieves the current value of the config field identified by {@code name} by
     * reflectively invoking the standard getter method
     * ({@code get<Name>()} with the first character capitalised) on the owning
     * {@link ConfigReader} instance.
     *
     * @param name the code-entry name of the config field to read
     * @return the current value returned by the getter, or {@code null} if the entry
     *         is not registered or if reflection fails
     */
    @Nullable
    public static Object getValue(String name) {
        try {
            ConfigReader config = configInstances.get(name);
            if (config == null) {
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: No config entry found for name: " + name);
                return null;
            }

            Method getter = config.getClass().getDeclaredMethod("get" + name.substring(0, 1).toUpperCase() + name.substring(1));
            return getter.invoke(config);
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Failed to get value for config entry: " + name, e);
            return null;
        }
    }

    /**
     * Updates the config field identified by {@code name} by reflectively invoking the
     * standard setter method ({@code set<Name>(T)} with the first character capitalised)
     * on the owning {@link ConfigReader} instance.
     *
     * <p>The setter method is resolved using the declared type of the backing field, so
     * {@code value} must be assignment-compatible with that type.</p>
     *
     * @param name  the code-entry name of the config field to update
     * @param value the new value to pass to the setter; must be compatible with the
     *              field's declared type
     * @return {@code true} if the setter was invoked successfully; {@code false} if the
     *         entry is not registered or if reflection fails
     */
    public static boolean setValue(String name, Object value) {
        try {
            ConfigReader config = configInstances.get(name);
            if (config == null) {
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: No config entry found for name: " + name);
                return false;
            }

            Field field = config.getClass().getDeclaredField(name);
            Method setter = config.getClass().getDeclaredMethod("set" + name.substring(0, 1).toUpperCase() + name.substring(1), field.getType());
            setter.invoke(config, value);
            return true;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Failed to set value for config entry: " + name, e);
            return false;
        }
    }

    /**
     * Returns a {@link Text} representation of {@code value} suitable for display in
     * command feedback and the options screen UI.
     *
     * <p>If {@link ConfigEntry#displayValueGetter()} is non-empty, that method is invoked
     * reflectively on the owning {@link ConfigReader} instance with {@code value} as its
     * argument and the result is returned. If the method returns a {@link Text} it is used
     * directly; otherwise it is wrapped with {@link Text#literal(String)}. If the attribute
     * is empty, {@code Text.literal(String.valueOf(value))} is returned as a default.</p>
     *
     * @param name  the code-entry name of the config entry
     * @param value the config value to convert to a display {@link Text}
     * @return a non-null {@link Text} representing the display form of {@code value}
     */
    public static Text getDisplayValue(String name, Object value) {
        try {
            ConfigEntry configAnnotation = configAnnotations.get(name);
            ConfigReader configInstance = configInstances.get(name);
            if (configAnnotation == null || configInstance == null) {
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: No config entry found for name: " + name);
                return Text.literal(String.valueOf(value));
            }
            String displayValueGetter = configAnnotation.displayValueGetter();
            if (displayValueGetter.isEmpty()) {
                return Text.literal(String.valueOf(value));
            }
            Method displayValueMethod = configInstance.getClass().getDeclaredMethod(displayValueGetter, value.getClass());
            Object displayValue = displayValueMethod.invoke(configInstance, value);
            if (displayValue instanceof Text) {
                return (Text) displayValue;
            } else {
                return Text.literal(String.valueOf(displayValue));
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Failed to get display value for config entry: " + name, e);
            return Text.literal(String.valueOf(value));
        }
    }

    /**
     * Converts a raw Brigadier command input value into the true value to be stored in
     * the config field.
     *
     * <p>If {@link ConfigEntry#commandInputToTrueValue()} is non-empty, that method is
     * invoked reflectively on the owning {@link ConfigReader} instance with {@code value}
     * as its argument and the result is returned. If the attribute is empty, or if
     * reflection fails, {@code value} is returned unchanged.</p>
     *
     * @param name  the code-entry name of the config entry
     * @param value the raw value as parsed by Brigadier
     * @return the converted true value to store, or {@code value} unchanged if no
     *         conversion method is defined or conversion fails
     */
    public static Object toTrueValue(String name, Object value) {
        try {
            ConfigEntry configAnnotation = configAnnotations.get(name);
            ConfigReader configInstance = configInstances.get(name);
            if (configAnnotation == null || configInstance == null) {
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: No config entry found for name: " + name);
                return value;
            }
            String toTrueValueMethodName = configAnnotation.commandInputToTrueValue();
            if (toTrueValueMethodName.isEmpty()) {
                return value;
            }
            Method toTrueValueMethod = configInstance.getClass().getDeclaredMethod(toTrueValueMethodName, value.getClass());
            return toTrueValueMethod.invoke(configInstance, value);
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Failed to convert command value to true value for config entry: " + name, e);
            return value;
        }
    }
    
    /**
     * Returns the internal map of code-entry names to their {@link ConfigEntry} annotation
     * metadata in registration order.
     *
     * <p>Callers should treat this map as read-only; mutating it directly may cause
     * inconsistencies with {@link #getConfigInstances()}.</p>
     *
     * @return the unmodifiable view of the config-annotation map
     */
    public static Map<String, ConfigEntry> getConfigAnnotations() {
        return configAnnotations;
    }

    /**
     * Returns the internal map of code-entry names to their owning {@link ConfigReader}
     * instances in registration order.
     *
     * <p>Callers should treat this map as read-only; mutating it directly may cause
     * inconsistencies with {@link #getConfigAnnotations()}.</p>
     *
     * @return the unmodifiable view of the config-instance map
     */
    public static Map<String, ConfigReader> getConfigInstances() {
        return configInstances;
    }
}
