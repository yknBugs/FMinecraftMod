package com.ykn.fmod.server.base.command;

import org.slf4j.Logger;

import com.mojang.brigadier.Command;
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

        String markdownTest = "```java\n" +
                "public class HelloWorld {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"```Hello!```\");\n" +
                "    }\n" +
                "}\n" +
                "```\n" +
                "另一段代码\n" +
                "```python\n" +
                "print('Hello from Python!')\n" +
                "```\n" +
                "无语言标记的代码块\n" +
                "`````\n" +
                "```\n" +
                "echo 'Hello Shell'\n" +
                "````\n" +
                "````````\n"+
                "最后一段代码\n" +
                "```cpp\n" +
                "#include <iostream>\n" +
                "int main() {\n" +
                "    std::cout << \"Hello, World!\" << std::endl;\n" +
                "    return 0;\n" +
                "}\n" +
                "```\n" + 
                "以上是测试用的`Markdown`文本，包含多个代码块。\n" + 
                "公式测试 $\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}$ \n" + 
                "## 标题测试\n" +
                "加粗**粗文本**测试\n" +
                "斜体*斜文本*测试，还有_另一种_写法\n" +
                "~~删除线~~测试\n" +
                "超链接[链接](https://127.0.0.1)\n" +
                "强调`重要文字`测试\n" +
                "- 枚举列表测试\n" +
                "- 这是列表的第二行\n" +
                "转义\\*字符\\*测试\n" + 
                "转义\\**字符(这里是斜体)\\**测试2\n" + 
                "转义\\~~字符\\~~测试3\n" +
                "转义\\`字符\\`测试4\n" + 
                "转义\\[字符\\]\\(字符\\)测试5\n" +
                "转义\\$字符\\$测试6";

        context.getSource().sendFeedback(() -> MarkdownToTextConverter.parseMarkdownToText(markdownTest), false);
        return null;
    }

    public CommandRegistrater(Logger logger) {
        this.logger = logger;
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
                );
                dispatcher.register(CommandManager.literal("f").redirect(fModCommandNode));
            });

            return true;
        } catch (Exception e) {
            logger.error("FMinectaftMod: Unable to register command.", e);
            return false;
        }
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
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runReloadCommand(CommandContext<ServerCommandSource> context) {
        try {
            Util.loadServerConfig();
            context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.reload.success"), false);
        } catch (Exception e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.reload.error"));
        }
        return Command.SINGLE_SUCCESS;
    }
}
