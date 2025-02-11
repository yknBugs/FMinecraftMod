package com.ykn.fmod.server.base.command;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.ykn.fmod.server.base.data.GptData;
import com.ykn.fmod.server.base.util.EnumI18n;
import com.ykn.fmod.server.base.util.GptHelper;
import com.ykn.fmod.server.base.util.MarkdownToTextConverter;
import com.ykn.fmod.server.base.util.MessageMethod;
import com.ykn.fmod.server.base.util.MessageType;
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

    private int runGptNewCommand(String text, CommandContext<ServerCommandSource> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getName());
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.newConversation(text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            Thread thread = new Thread(gptHelper);
            thread.setDaemon(true);
            context.getSource().sendFeedback(() -> Text.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Text.literal(text)), true);
            thread.start();
            // if (context.getSource().getPlayer() != null) {
            //     // Other source would have already logged the message
            //     logger.info("<{}> {}", context.getSource().getDisplayName().getString(), text);
            // }
        } catch (URISyntaxException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            logger.error("FMinectaftMod: Unexpected error when executing command /f gpt new", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        } catch (NullPointerException e) {
            logger.error("FMinectaftMod: Unexpected error when executing command /f gpt new", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGptReplyCommand(String text, CommandContext<ServerCommandSource> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getName());
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.reply(text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            Thread thread = new Thread(gptHelper);
            thread.setDaemon(true);
            context.getSource().sendFeedback(() -> Text.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Text.literal(text)), true);
            thread.start();
        } catch (URISyntaxException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            logger.error("FMinectaftMod: Unexpected error when executing command /f gpt reply", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        } catch (NullPointerException e) {
            logger.error("FMinectaftMod: Unexpected error when executing command /f gpt reply", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGptRegenerateCommand(CommandContext<ServerCommandSource> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getName());
            int gptDataLength = gptData.getHistorySize();
            if (gptDataLength == 0) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.nohistory"));
            }
            String text = gptData.getPostMessages(gptDataLength - 1);
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.regenerate(url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            Thread thread = new Thread(gptHelper);
            thread.setDaemon(true);
            context.getSource().sendFeedback(() -> Text.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Text.literal(text)), true);
            thread.start();
        } catch (URISyntaxException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            logger.error("FMinectaftMod: Unexpected error when executing command /f gpt regenerate", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        } catch (NullPointerException e) {
            logger.error("FMinectaftMod: Unexpected error when executing command /f gpt regenerate", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGptEditCommand(int index, String text, CommandContext<ServerCommandSource> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getName());
            int gptDataLength = gptData.getHistorySize();
            if (gptDataLength == 0) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.nohistory"));
            }
            // The Command argument index begins from 1, the source code index begins from 0
            if (index <= 0 || index > gptDataLength) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.historyindexerror", index, gptDataLength));
            }
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.editHistory(index - 1, text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            Thread thread = new Thread(gptHelper);
            thread.setDaemon(true);
            context.getSource().sendFeedback(() -> Text.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Text.literal(text)), true);
            thread.start();
        } catch (URISyntaxException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            logger.error("FMinectaftMod: Unexpected error when executing command /f gpt edit " + index, e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        } catch (NullPointerException e) {
            logger.error("FMinectaftMod: Unexpected error when executing command /f gpt edit " + index, e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGptHistoryCommand(int index, CommandContext<ServerCommandSource> context) {
        try {
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getName());
            final int gptDataLength = gptData.getHistorySize();
            if (gptDataLength == 0) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.nohistory"));
            }
            if (index == 0) {
                index = gptDataLength;
            }
            if (index < 0 || index > gptDataLength) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.historyindexerror", index, gptDataLength));
            }
            final int finalIndex = index;
            final String postMessage = gptData.getPostMessages(index - 1);
            final String model = gptData.getGptModels(index - 1);
            final Text receivedMessage = gptData.getResponseTexts(index - 1);
            context.getSource().sendFeedback(() -> Text.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Text.literal(postMessage)), false);
            context.getSource().sendFeedback(() -> Text.literal("<").append(model.isBlank() ? "GPT" : model).append("> ").append(receivedMessage), false);
            context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.gpt.history", finalIndex, gptDataLength), false);
        } catch (NullPointerException e) {
            logger.error("FMinectaftMod: Unexpected error when executing command /f gpt show " + index, e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
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
                        .then(CommandManager.literal("new")
                            .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(context -> {return runGptNewCommand(StringArgumentType.getString(context, "message"), context);})
                            ))
                        .then(CommandManager.literal("reply")
                            .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(context -> {return runGptReplyCommand(StringArgumentType.getString(context, "message"), context);})
                            ))
                        .then(CommandManager.literal("regenerate").executes(context -> {return runGptRegenerateCommand(context);}))
                        .then(CommandManager.literal("edit")
                            .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                    .executes(context -> {return runGptEditCommand(IntegerArgumentType.getInteger(context, "index"), StringArgumentType.getString(context, "message"), context);})
                                )
                            )
                        )
                        .then(CommandManager.literal("history")
                            .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> {return runGptHistoryCommand(IntegerArgumentType.getInteger(context, "index"), context);})
                            )
                            .executes(context -> {return runGptHistoryCommand(0, context);})
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
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageType.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageType.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageType.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("entityDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("bossDeathMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageType.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageType.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageType.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("bossDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("namedMobDeathMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageType.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageType.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageType.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("namedMobDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("killerDeathMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageType.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageType.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageType.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("killerDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("bossMaxHealthThreshold")
                            .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(0))
                                .executes(context -> {return runOptionsCommand("bossMaxHealthThreshold", DoubleArgumentType.getDouble(context, "value"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("bossMaxHealthThreshold", null, context);})
                        )
                        .then(CommandManager.literal("playerDeathCoord")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageMethod.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageMethod.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageMethod.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageMethod.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageMethod.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageMethod.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageMethod.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("playerDeathCoord", null, context);})
                        )
                        .then(CommandManager.literal("projectileHitsEntity")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageMethod.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageMethod.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageMethod.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageMethod.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageMethod.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageMethod.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageMethod.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("projectileHitsEntity", null, context);})
                        )
                        .then(CommandManager.literal("projectileBeingHit")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageMethod.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageMethod.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageMethod.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageMethod.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageMethod.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageMethod.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageMethod.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("projectileBeingHit", null, context);})
                        )
                        .then(CommandManager.literal("informAFK")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("informAFK", MessageMethod.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("informAFK", MessageMethod.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("informAFK", MessageMethod.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("informAFK", MessageMethod.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("informAFK", MessageMethod.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("informAFK", MessageMethod.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("informAFK", MessageMethod.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("informAFK", null, context);})
                        )
                        .then(CommandManager.literal("informAFKThreshold")
                            .then(CommandManager.argument("value", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("informAFKThreshold", IntegerArgumentType.getInteger(context, "value"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("informAFKThreshold", null, context);})
                        )
                        .then(CommandManager.literal("broadcastAFK")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("broadcastAFK", MessageMethod.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("broadcastAFK", MessageMethod.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageMethod.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageMethod.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageMethod.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("broadcastAFK", MessageMethod.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("broadcastAFK", MessageMethod.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("broadcastAFK", null, context);})
                        )
                        .then(CommandManager.literal("broadcastAFKThreshold")
                            .then(CommandManager.argument("value", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("broadcastAFKThreshold", IntegerArgumentType.getInteger(context, "value"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("broadcastAFKThreshold", null, context);})
                        )
                        .then(CommandManager.literal("backFromAFK")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("backFromAFK", MessageMethod.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("backFromAFK", MessageMethod.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("backFromAFK", MessageMethod.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("backFromAFK", MessageMethod.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("backFromAFK", MessageMethod.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("backFromAFK", MessageMethod.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("backFromAFK", MessageMethod.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("backFromAFK", null, context);})
                        )
                        .then(CommandManager.literal("biomeChangeMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageMethod.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageMethod.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageMethod.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageMethod.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageMethod.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageMethod.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageMethod.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("biomeChangeMessage", null, context);})
                        )
                        .then(CommandManager.literal("biomeChangeDelay")
                            .then(CommandManager.argument("value", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("biomeChangeDelay", IntegerArgumentType.getInteger(context, "value"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("biomeChangeDelay", null, context);})
                        )
                        .then(CommandManager.literal("gptUrl")
                            .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                .executes(context -> {return runOptionsCommand("gptUrl", StringArgumentType.getString(context, "url"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptUrl", null, context);})
                        )
                        .then(CommandManager.literal("gptAccessTokens")
                            .then(CommandManager.argument("tokens", StringArgumentType.greedyString())
                                .executes(context -> {return runOptionsCommand("gptAccessTokens", StringArgumentType.getString(context, "tokens"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptAccessTokens", null, context);})
                        )
                        .then(CommandManager.literal("gptModel")
                            .then(CommandManager.argument("model", StringArgumentType.greedyString())
                                .executes(context -> {return runOptionsCommand("gptModel", StringArgumentType.getString(context, "model"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptModel", null, context);})
                        )
                        .then(CommandManager.literal("gptSystemPrompts")
                            .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                                .executes(context -> {return runOptionsCommand("gptSystemPrompts", StringArgumentType.getString(context, "prompt"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptSystemPrompts", null, context);})
                        )
                        .then(CommandManager.literal("gptTemperature")
                            .then(CommandManager.argument("temperature", DoubleArgumentType.doubleArg(0, 1))
                                .executes(context -> {return runOptionsCommand("gptTemperature", DoubleArgumentType.getDouble(context, "temperature"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptTemperature", null, context);})
                        )
                        .then(CommandManager.literal("gptTimeout")
                            .then(CommandManager.argument("timeout", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("gptTimeout", IntegerArgumentType.getInteger(context, "timeout"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptTimeout", null, context);})
                        )
                    )
                );
                dispatcher.register(CommandManager.literal("f")
                    .requires(source -> source.hasPermissionLevel(0))
                    .executes(context -> {return runFModCommand(context);})
                    .redirect(fModCommandNode)
                );
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
                        final MutableText text = EnumI18n.getMessageTypeI18n(Util.serverConfig.getEntityDeathMessageType());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.entdeathmsg", text), false);
                    } else {
                        Util.serverConfig.setEntityDeathMessageType((MessageType) value);
                        final MutableText text = EnumI18n.getMessageTypeI18n((MessageType) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.entdeathmsg", text), true);
                    }
                    break;
                case "bossDeathMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageTypeI18n(Util.serverConfig.getBossDeathMessageType());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcbossdeath", text), false);
                    } else {
                        Util.serverConfig.setBossDeathMessageType((MessageType) value);
                        final MutableText text = EnumI18n.getMessageTypeI18n((MessageType) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcbossdeath", text), true);
                    }
                    break;
                case "namedMobDeathMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageTypeI18n(Util.serverConfig.getNamedEntityDeathMessageType());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.nameddeath", text), false);
                    } else {
                        Util.serverConfig.setNamedEntityDeathMessageType((MessageType) value);
                        final MutableText text = EnumI18n.getMessageTypeI18n((MessageType) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.nameddeath", text), true);
                    }
                    break;
                case "killerDeathMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageTypeI18n(Util.serverConfig.getKillerEntityDeathMessageType());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bckillerdeath", text), false);
                    } else {
                        Util.serverConfig.setKillerEntityDeathMessageType((MessageType) value);
                        final MutableText text = EnumI18n.getMessageTypeI18n((MessageType) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bckillerdeath", text), true);
                    }
                    break;
                case "bossMaxHealthThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bossmaxhp", Util.serverConfig.getBossMaxHpThreshold()), false);
                    } else {
                        Util.serverConfig.setBossMaxHpThreshold((double) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bossmaxhp", value), true);
                    }
                    break;
                case "playerDeathCoord":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageMethodI18n(Util.serverConfig.getPlayerDeathCoordMethod());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcdeathcoord", text), false);
                    } else {
                        Util.serverConfig.setPlayerDeathCoordMethod((MessageMethod) value);
                        final MutableText text = EnumI18n.getMessageMethodI18n((MessageMethod) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcdeathcoord", text), true);
                    }
                    break;
                case "projectileHitsEntity":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageMethodI18n(Util.serverConfig.getProjectileHitOthersMethod());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.projhitting", text), false);
                    } else {
                        Util.serverConfig.setProjectileHitOthersMethod((MessageMethod) value);
                        final MutableText text = EnumI18n.getMessageMethodI18n((MessageMethod) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.projhitting", text), true);
                    }
                    break;
                case "projectileBeingHit":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageMethodI18n(Util.serverConfig.getProjectileBeingHitMethod());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.projbeinghit", text), false);
                    } else {
                        Util.serverConfig.setProjectileBeingHitMethod((MessageMethod) value);
                        final MutableText text = EnumI18n.getMessageMethodI18n((MessageMethod) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.projbeinghit", text), true);
                    }
                    break;
                case "informAFK":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageMethodI18n(Util.serverConfig.getInformAfkingMethod());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.informafk", text), false);
                    } else {
                        Util.serverConfig.setInformAfkingMethod((MessageMethod) value);
                        final MutableText text = EnumI18n.getMessageMethodI18n((MessageMethod) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.informafk", text), true);
                    }
                    break;
                case "informAFKThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.informafkthres", String.format("%.2f", Util.serverConfig.getInformAfkingThreshold() / 20.0)), false);
                    } else {
                        Util.serverConfig.setInformAfkingThreshold((int) value * 20);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.informafkthres", value), true);
                    }
                    break;
                case "broadcastAFK":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageMethodI18n(Util.serverConfig.getBroadcastAfkingMethod());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcafk", text), false);
                    } else {
                        Util.serverConfig.setBroadcastAfkingMethod((MessageMethod) value);
                        final MutableText text = EnumI18n.getMessageMethodI18n((MessageMethod) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcafk", text), true);
                    }
                    break;
                case "broadcastAFKThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcafkthres", String.format("%.2f", Util.serverConfig.getBroadcastAfkingThreshold() / 20.0)), false);
                    } else {
                        Util.serverConfig.setBroadcastAfkingThreshold((int) value * 20);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcafkthres", value), true);
                    }
                    break;
                case "backFromAFK":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageMethodI18n(Util.serverConfig.getStopAfkingMethod());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.stopafk", text), false);
                    } else {
                        Util.serverConfig.setStopAfkingMethod((MessageMethod) value);
                        final MutableText text = EnumI18n.getMessageMethodI18n((MessageMethod) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.stopafk", text), true);
                    }
                    break;
                case "biomeChangeMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageMethodI18n(Util.serverConfig.getChangeBiomeMethod());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.changebiome", text), false);
                    } else {
                        Util.serverConfig.setChangeBiomeMethod((MessageMethod) value);
                        final MutableText text = EnumI18n.getMessageMethodI18n((MessageMethod) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.changebiome", text), true);
                    }
                    break;
                case "biomeChangeDelay":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.biomedelay", String.format("%.2f", Util.serverConfig.getChangeBiomeDelay() / 20.0)), false);
                    } else {
                        Util.serverConfig.setChangeBiomeDelay((int) value * 20);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.biomedelay", value), true);
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
                case "gptSystemPrompts":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gptsysprompt", Util.serverConfig.getGptSystemPrompt()), false);
                    } else {
                        Util.serverConfig.setGptSystemPrompt((String) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gptsysprompt", value), true);
                    }
                    break;
                case "gptTemperature":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gpttemperature", Util.serverConfig.getGptTemperature()), false);
                    } else {
                        double temperature = (double) value;
                        Util.serverConfig.setGptTemperature(temperature);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gpttemperature", value), true);
                    }
                    break;
                case "gptTimeout":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gpttimeout", (int) (Util.serverConfig.getGptServerTimeout() / 1000)), false);
                    } else {
                        int timeout = (int) value;
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
