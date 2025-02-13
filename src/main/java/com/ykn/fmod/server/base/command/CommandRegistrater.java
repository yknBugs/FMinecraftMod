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
import com.ykn.fmod.server.base.util.MessageReceiver;
import com.ykn.fmod.server.base.util.MessageLocation;
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
                            .then(CommandManager.argument("enable", BoolArgumentType.bool())
                                .executes(context -> {return runOptionsCommand("serverTranslation", BoolArgumentType.getBool(context, "enable"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("serverTranslation", null, context);})
                        )
                        .then(CommandManager.literal("entityDeathMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("entityDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("bossDeathMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("bossDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("namedMobDeathMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("namedMobDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("killerDeathMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("killerDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("bossMaxHealthThreshold")
                            .then(CommandManager.argument("HP", DoubleArgumentType.doubleArg(0))
                                .executes(context -> {return runOptionsCommand("bossMaxHealthThreshold", DoubleArgumentType.getDouble(context, "HP"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("bossMaxHealthThreshold", null, context);})
                        )
                        .then(CommandManager.literal("playerDeathCoord")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("playerDeathCoord", null, context);})
                        )
                        .then(CommandManager.literal("projectileHitsEntity")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("projectileHitsEntity", null, context);})
                        )
                        .then(CommandManager.literal("projectileBeingHit")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("projectileBeingHit", null, context);})
                        )
                        .then(CommandManager.literal("informAFK")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("informAFK", null, context);})
                        )
                        .then(CommandManager.literal("informAFKThreshold")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("informAFKThreshold", IntegerArgumentType.getInteger(context, "seconds"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("informAFKThreshold", null, context);})
                        )
                        .then(CommandManager.literal("broadcastAFK")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("broadcastAFK", null, context);})
                        )
                        .then(CommandManager.literal("broadcastAFKThreshold")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("broadcastAFKThreshold", IntegerArgumentType.getInteger(context, "seconds"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("broadcastAFKThreshold", null, context);})
                        )
                        .then(CommandManager.literal("backFromAFK")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("backFromAFK", null, context);})
                        )
                        .then(CommandManager.literal("biomeChangeMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("biomeChangeMessage", null, context);})
                        )
                        .then(CommandManager.literal("biomeChangeDelay")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("biomeChangeDelay", IntegerArgumentType.getInteger(context, "seconds"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("biomeChangeDelay", null, context);})
                        )
                        .then(CommandManager.literal("bossFightMessageLocation")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("bossFightMessageLocation", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("bossFightMessageLocation", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("bossFightMessageLocation", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("bossFightMessageLocation", null, context);})
                        )
                        .then(CommandManager.literal("bossFightMessageReceiver")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("bossFightMessageReceiver", null, context);})
                        )
                        .then(CommandManager.literal("bossFightMessageInterval")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("bossFightMessageInterval", IntegerArgumentType.getInteger(context, "seconds"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("bossFightMessageInterval", null, context);})
                        )
                        .then(CommandManager.literal("monsterSurroundMessageLocation")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", null, context);})
                        )
                        .then(CommandManager.literal("monsterSurroundMessageReceiver")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", null, context);})
                        )
                        .then(CommandManager.literal("monsterSurroundMessageInterval")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("monsterSurroundMessageInterval", IntegerArgumentType.getInteger(context, "seconds"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("monsterSurroundMessageInterval", null, context);})
                        )
                        .then(CommandManager.literal("monsterNumberThreshold")
                            .then(CommandManager.argument("num", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("monsterNumberThreshold", IntegerArgumentType.getInteger(context, "num"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("monsterNumberThreshold", null, context);})
                        )
                        .then(CommandManager.literal("monsterDistanceThreshold")
                            .then(CommandManager.argument("meters", DoubleArgumentType.doubleArg(0))
                                .executes(context -> {return runOptionsCommand("monsterDistanceThreshold", DoubleArgumentType.getDouble(context, "meters"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("monsterDistanceThreshold", null, context);})
                        )
                        .then(CommandManager.literal("entityNumberWarning")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("entityNumberWarning", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("entityNumberWarning", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("entityNumberWarning", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("entityNumberWarning", null, context);})
                        )
                        .then(CommandManager.literal("entityNumberThreshold")
                            .then(CommandManager.argument("num", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("entityNumberThreshold", IntegerArgumentType.getInteger(context, "num"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("entityNumberThreshold", null, context);})
                        )
                        .then(CommandManager.literal("entityNumberCheckInterval")
                            .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1))
                                .executes(context -> {return runOptionsCommand("entityNumberCheckInterval", IntegerArgumentType.getInteger(context, "ticks"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("entityNumberCheckInterval", null, context);})
                        )
                        .then(CommandManager.literal("playerHurtMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("playerHurtMessage", null, context);})
                        )
                        .then(CommandManager.literal("hugeDamageThreshold")
                            .then(CommandManager.argument("percentage", DoubleArgumentType.doubleArg(0, 100))
                                .executes(context -> {return runOptionsCommand("hugeDamageThreshold", DoubleArgumentType.getDouble(context, "percentage"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("hugeDamageThreshold", null, context);})
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
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("gptTimeout", IntegerArgumentType.getInteger(context, "seconds"), context);})
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
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityDeathMessage());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.entdeathmsg", text), false);
                    } else {
                        Util.serverConfig.setEntityDeathMessage((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.entdeathmsg", text), true);
                    }
                    break;
                case "bossDeathMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossDeathMessage());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcbossdeath", text), false);
                    } else {
                        Util.serverConfig.setBossDeathMessage((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcbossdeath", text), true);
                    }
                    break;
                case "namedMobDeathMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getNamedEntityDeathMessage());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.nameddeath", text), false);
                    } else {
                        Util.serverConfig.setNamedEntityDeathMessage((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.nameddeath", text), true);
                    }
                    break;
                case "killerDeathMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getKillerEntityDeathMessage());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bckillerdeath", text), false);
                    } else {
                        Util.serverConfig.setKillerEntityDeathMessage((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
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
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerDeathCoord());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcdeathcoord", text), false);
                    } else {
                        Util.serverConfig.setPlayerDeathCoord((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcdeathcoord", text), true);
                    }
                    break;
                case "projectileHitsEntity":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileHitOthers());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.projhitting", text), false);
                    } else {
                        Util.serverConfig.setProjectileHitOthers((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.projhitting", text), true);
                    }
                    break;
                case "projectileBeingHit":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileBeingHit());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.projbeinghit", text), false);
                    } else {
                        Util.serverConfig.setProjectileBeingHit((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.projbeinghit", text), true);
                    }
                    break;
                case "informAFK":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getInformAfking());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.informafk", text), false);
                    } else {
                        Util.serverConfig.setInformAfking((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
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
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBroadcastAfking());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcafk", text), false);
                    } else {
                        Util.serverConfig.setBroadcastAfking((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
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
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getStopAfking());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.stopafk", text), false);
                    } else {
                        Util.serverConfig.setStopAfking((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.stopafk", text), true);
                    }
                    break;
                case "biomeChangeMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getChangeBiome());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.changebiome", text), false);
                    } else {
                        Util.serverConfig.setChangeBiome((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
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
                case "bossFightMessageLocation":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossFightMessageLocation());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bossfightloc", text), false);
                    } else {
                        Util.serverConfig.setBossFightMessageLocation((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bossfightloc", text), true);
                    }
                    break;
                case "bossFightMessageReceiver":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBossFightMessageReceiver());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bossfightreceiver", text), false);
                    } else {
                        Util.serverConfig.setBossFightMessageReceiver((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bossfightreceiver", text), true);
                    }
                    break;
                case "bossFightMessageInterval":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bossfightinterval", String.format("%.2f", Util.serverConfig.getBossFightInterval() / 20.0)), false);
                    } else {
                        Util.serverConfig.setBossFightInterval((int) value * 20);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bossfightinterval", value), true);
                    }
                    break;
                case "monsterSurroundMessageLocation":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getMonsterSurroundMessageLocation());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.monsterloc", text), false);
                    } else {
                        Util.serverConfig.setMonsterSurroundMessageLocation((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.monsterloc", text), true);
                    }
                    break;
                case "monsterSurroundMessageReceiver":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getMonsterSurroundMessageReceiver());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.monsterreceiver", text), false);
                    } else {
                        Util.serverConfig.setMonsterSurroundMessageReceiver((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.monsterreceiver", text), true);
                    }
                    break;
                case "monsterSurroundMessageInterval":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.monsterinterval", String.format("%.2f", Util.serverConfig.getMonsterSurroundInterval() / 20.0)), false);
                    } else {
                        Util.serverConfig.setMonsterSurroundInterval((int) value * 20);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.monsterinterval", value), true);
                    }
                    break;
                case "monsterNumberThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.monsternumber", Util.serverConfig.getMonsterNumberThreshold()), false);
                    } else {
                        Util.serverConfig.setMonsterNumberThreshold((int) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.monsternumber", value), true);
                    }
                    break;
                case "monsterDistanceThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.monsterdistance", Util.serverConfig.getMonsterDistanceThreshold()), false);
                    } else {
                        Util.serverConfig.setMonsterDistanceThreshold((double) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.monsterdistance", value), true);
                    }
                    break;
                case "entityNumberWarning":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityNumberWarning());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.entitywarning", text), false);
                    } else {
                        Util.serverConfig.setEntityNumberWarning((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.entitywarning", text), true);
                    }
                    break;
                case "entityNumberThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.entitynumber", Util.serverConfig.getEntityNumberThreshold()), false);
                    } else {
                        Util.serverConfig.setEntityNumberThreshold((int) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.entitynumber", value), true);
                    }
                    break;
                case "entityNumberCheckInterval":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.entityinterval", Util.serverConfig.getEntityNumberInterval()), false);
                    } else {
                        Util.serverConfig.setEntityNumberInterval((int) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.entityinterval", value), true);
                    }
                    break;
                case "playerHurtMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerSeriousHurt());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.playerhurt", text), false);
                    } else {
                        Util.serverConfig.setPlayerSeriousHurt((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.playerhurt", text), true);
                    }
                    break;
                case "hugeDamageThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.damagethres", Util.serverConfig.getPlayerHurtThreshold() * 100.0), false);
                    } else {
                        Util.serverConfig.setPlayerHurtThreshold((double) value / 100.0);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.damagethres", value), true);
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
