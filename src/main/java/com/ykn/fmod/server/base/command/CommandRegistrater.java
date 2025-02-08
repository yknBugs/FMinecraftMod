package com.ykn.fmod.server.base.command;

import java.net.URI;

import org.slf4j.Logger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.ykn.fmod.server.base.util.GptHelper;
import com.ykn.fmod.server.base.util.MarkdownToTextConverter;
import com.ykn.fmod.server.base.util.Util;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class CommandRegistrater {

    private Logger logger;

    private Object devFunction(CommandContext<ServerCommandSource> context) {
        // This function is used for development purposes. Execute command /f dev to run this function.
        // This function should be removed in the final release.

        String markdownTest = "C++ Syntax Highlight Test\n" + 
        "```cpp\n" +
        "#include <string>\n" +
        "#include <vector>\n" +
        "#include <algorithm>\n\n" +
        "int main() {\n" +
        "    std::vector<std::string> s;\n" + 
        "    std::sort(s.begin(), s.end(), [&s](auto& a, auto& b) {\n" + 
        "        a = s;\n" +
        "    });\n" +
        "}\n" +
        "```\n" +
        "End of Test";

        context.getSource().sendFeedback(() -> MarkdownToTextConverter.parseMarkdownToText(markdownTest), false);
        return null;
    }

    public CommandRegistrater(Logger logger) {
        this.logger = logger;
    }

    
    private int runFModCommand(CommandContext<ServerCommandSource> context) {
        try {
            MutableText commandFeedback = Util.parseTranslateableText("fmod.misc.version", Util.getMinecraftVersion(), Util.getModVersion(), Util.getModAuthors());
            context.getSource().sendFeedback(() -> commandFeedback, false);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.version.error"));
        }
    }

    private int runDevCommand(CommandContext<ServerCommandSource> context) {
        try {
            context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.dev.start"), false);
            Object result = devFunction(context);
            context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.dev.end", result == null ? "null" : result.toString()), false);
        } catch (Exception e) {
            try {
                context.getSource().sendFeedback(() -> Text.literal(e.getMessage()), false);
                context.getSource().sendFeedback(() -> Text.literal(e.getStackTrace().toString()), false);
            } catch (Exception exception) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.dev.error"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGptCommand(String text, CommandContext<ServerCommandSource> context) {
        try {
            context.getSource().sendFeedback(() -> Text.of(text), false);
            GptHelper gptHelper = new GptHelper(text, context);
            gptHelper.setURL(Util.serverConfig.getGptUrl());
            Thread thread = new Thread(gptHelper);
            thread.setDaemon(true);
            thread.start();
            if (context.getSource().getPlayer() != null) {
                // Other source would have already logged the message
                logger.info(text);
            }
        } catch (Exception e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runReloadCommand(CommandContext<ServerCommandSource> context) {
        try {
            Util.loadServerConfig();
            context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.reload.success"), true);
        } catch (Exception e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.reload.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    public boolean registerCommand() {
        try {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                final LiteralCommandNode<ServerCommandSource> fModCommandNode = dispatcher.register(CommandManager.literal("fminecraftmod")
                    .requires(source -> source.hasPermissionLevel(0))
                    .executes(context -> {return runFModCommand(context);})
                    .then(CommandManager.literal("dev")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {return runDevCommand(context);})
                    )
                    .then(CommandManager.literal("gpt")
                        .requires(source -> source.hasPermissionLevel(3))
                        .then(CommandManager.argument("text", StringArgumentType.string())
                            .executes(context -> {return runGptCommand(StringArgumentType.getString(context, "text"), context);})
                        )
                    )
                    .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {return runReloadCommand(context);})
                    )
                    .then(CommandManager.literal("options")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.literal("serverTranslation")
                            .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(context -> {return runOptionsCommand("serverTranslation", BoolArgumentType.getBool(context, "value"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("serverTranslation", null, context);})
                        )
                        .then(CommandManager.literal("entityDeathMessage")
                            .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(context -> {return runOptionsCommand("entityDeathMessage", BoolArgumentType.getBool(context, "value"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("entityDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("bossDeathMessage")
                            .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(context -> {return runOptionsCommand("bossDeathMessage", BoolArgumentType.getBool(context, "value"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("bossDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("namedMobDeathMessage")
                            .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(context -> {return runOptionsCommand("namedMobDeathMessage", BoolArgumentType.getBool(context, "value"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("namedMobDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("killerDeathMessage")
                            .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(context -> {return runOptionsCommand("killerDeathMessage", BoolArgumentType.getBool(context, "value"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("killerDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("bossMaxHealthThreshold")
                            .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> {return runOptionsCommand("bossMaxHealthThreshold", DoubleArgumentType.getDouble(context, "value"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("bossMaxHealthThreshold", null, context);})
                        )
                        .then(CommandManager.literal("playerDeathCoord")
                            .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(context -> {return runOptionsCommand("playerDeathCoord", BoolArgumentType.getBool(context, "value"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("playerDeathCoord", null, context);})
                        )
                        .then(CommandManager.literal("gptUrl")
                            .then(CommandManager.argument("url", StringArgumentType.string())
                                .executes(context -> {return runOptionsCommand("gptUrl", StringArgumentType.getString(context, "url"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptUrl", null, context);})
                        )
                        .then(CommandManager.literal("gptAccessTokens")
                            .then(CommandManager.argument("tokens", StringArgumentType.string())
                                .executes(context -> {return runOptionsCommand("gptAccessTokens", StringArgumentType.getString(context, "tokens"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptAccessTokens", null, context);})
                        )
                        .then(CommandManager.literal("gptModel")
                            .then(CommandManager.argument("model", StringArgumentType.string())
                                .executes(context -> {return runOptionsCommand("gptModel", StringArgumentType.getString(context, "model"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptModel", null, context);})
                        )
                        .then(CommandManager.literal("gptTemperature")
                            .then(CommandManager.argument("temperature", DoubleArgumentType.doubleArg())
                                .executes(context -> {return runOptionsCommand("gptTemperature", DoubleArgumentType.getDouble(context, "temperature"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptTemperature", null, context);})
                        )
                        .then(CommandManager.literal("gptTimeout")
                            .then(CommandManager.argument("timeout", IntegerArgumentType.integer())
                                .executes(context -> {return runOptionsCommand("gptTimeout", IntegerArgumentType.getInteger(context, "timeout"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptTimeout", null, context);})
                        )
                    )
                );
                dispatcher.register(CommandManager.literal("f").redirect(fModCommandNode));
            });

            return true;
        } catch (Exception e) {
            logger.error("FMinectaftMod: Unable to register command.", e);
            return false;
        }
    }

    private int runOptionsCommand(String options, Object value, CommandContext<ServerCommandSource> context) {
        try {
            switch (options) {
                case "serverTranslation":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.translate", Util.serverConfig.isEnableServerTranslation()), false);
                    } else {
                        Util.serverConfig.setEnableServerTranslation((boolean) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.translate", value), true);
                    }
                    break;
                case "entityDeathMessage":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.entdeathmsg", Util.serverConfig.isEnableEntityDeathMsg()), false);
                    } else {
                        Util.serverConfig.setEnableEntityDeathMsg((boolean) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.entdeathmsg", value), true);
                    }
                    break;
                case "bossDeathMessage":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcbossdeath", Util.serverConfig.isBcBossDeathMsg()), false);
                    } else {
                        Util.serverConfig.setBcBossDeathMsg((boolean) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcbossdeath", value), true);
                    }
                    break;
                case "namedMobDeathMessage":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.nameddeath", Util.serverConfig.isNamedMobDeathMsg()), false);
                    } else {
                        Util.serverConfig.setNamedMobDeathMsg((boolean) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.nameddeath", value), true);
                    }
                    break;
                case "killerDeathMessage":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bckillerdeath", Util.serverConfig.isKillerEntityDeathMsg()), false);
                    } else {
                        Util.serverConfig.setKillerEntityDeathMsg((boolean) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bckillerdeath", value), true);
                    }
                    break;
                case "bossMaxHealthThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bossmaxhp", Util.serverConfig.getBossMaxHpThreshold()), false);
                    } else {
                        if (((double) value) < 0) {
                            throw new CommandException(Util.parseTranslateableText("fmod.command.options.negativemaxhp", value));
                        }
                        Util.serverConfig.setBossMaxHpThreshold((double) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bossmaxhp", value), true);
                    }
                    break;
                case "playerDeathCoord":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcdeathcoord", Util.serverConfig.isBcPlayerDeathCoord()), false);
                    } else {
                        Util.serverConfig.setBcPlayerDeathCoord((boolean) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcdeathcoord", value), true);
                    }
                    break;
                case "gptUrl":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gpturl", Util.serverConfig.getGptUrl()), false);
                    } else {
                        try {
                            new URI((String) value).toURL();
                        } catch (Exception e) {
                            throw new CommandException(Util.parseTranslateableText("fmod.command.options.invalidurl", value));
                        }
                        Util.serverConfig.setGptUrl((String) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gpturl", value), true);
                    }
                    break;
                case "gptAccessTokens":
                    if (value == null) {
                        final String secureTokens = Util.serverConfig.getSecureGptAccessTokens();
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gptkey", secureTokens), false);
                    } else {
                        String token = (String) value;
                        Util.serverConfig.setGptAccessTokens(token);
                        // For security reasons, we don't want to show the full token in the log, only show the first 5 and the last 5 characters
                        final String secureTokens = Util.serverConfig.getSecureGptAccessTokens();
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gptkey", secureTokens), true);
                    }
                    break;
                case "gptModel":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gptmodel", Util.serverConfig.getGptModel()), false);
                    } else {
                        Util.serverConfig.setGptModel((String) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gptmodel", value), true);
                    }
                    break;
                case "gptTemperature":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gpttemperature", Util.serverConfig.getGptTemperature()), false);
                    } else {
                        double temperature = (double) value;
                        if (temperature < 0) {
                            throw new CommandException(Util.parseTranslateableText("fmod.command.options.negativetemperature", value));
                        }
                        if (temperature > 1) {
                            throw new CommandException(Util.parseTranslateableText("fmod.command.options.largetemperature", value));
                        }
                        Util.serverConfig.setGptTemperature(temperature);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gpttemperature", value), true);
                    }
                    break;
                case "gptTimeout":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gpttimeout", (int) (Util.serverConfig.getGptServerTimeout() / 1000)), false);
                    } else {
                        int timeout = (int) value;
                        if (timeout < 0) {
                            throw new CommandException(Util.parseTranslateableText("fmod.command.options.negativetimeout", value));
                        }
                        Util.serverConfig.setGptServerTimeout(timeout * 1000);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gpttimeout", value), true);
                    }
                    break;
                default:
                    throw new CommandException(Util.parseTranslateableText("fmod.command.options.unknownoption", options));
            }
            if (value != null) {
                Util.saveServerConfig();
            }
        } catch (ClassCastException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.options.classcast", value, options, e.getMessage()));
        }
        return Command.SINGLE_SUCCESS;
    }
}
