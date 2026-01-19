package com.ykn.fmod.server.base.util;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class EnumI18n {

    public static MutableComponent getMessageLocationI18n(MessageLocation type) {
        switch (type) {
            case NONE:
                return Util.parseTranslateableText("fmod.message.type.none").withStyle(ChatFormatting.RED);
            case CHAT:
                return Util.parseTranslateableText("fmod.message.type.chat").withStyle(ChatFormatting.GREEN);
            case ACTIONBAR:
                return Util.parseTranslateableText("fmod.message.type.actionbar").withStyle(ChatFormatting.YELLOW);
            default:
                return Component.literal(type.toString());
        }
    }

    public static MutableComponent getMessageReceiverI18n(MessageReceiver method) {
        switch (method) {
            case ALL:
                return Util.parseTranslateableText("fmod.message.type.toall").withStyle(ChatFormatting.YELLOW);
            case OP:
                return Util.parseTranslateableText("fmod.message.type.toop").withStyle(ChatFormatting.GOLD);
            case SELFOP:
                return Util.parseTranslateableText("fmod.message.type.toselfop").withStyle(ChatFormatting.YELLOW);
            case TEAMOP:
                return Util.parseTranslateableText("fmod.message.type.toteamop").withStyle(ChatFormatting.YELLOW);
            case TEAM:
                return Util.parseTranslateableText("fmod.message.type.toteam").withStyle(ChatFormatting.GREEN);
            case SELF:
                return Util.parseTranslateableText("fmod.message.type.toself").withStyle(ChatFormatting.GREEN);
            case NONE:
                return Util.parseTranslateableText("fmod.message.type.none").withStyle(ChatFormatting.RED);
            default:
                return Component.literal(method.toString());
        }
    }

    public static MutableComponent getBooleanValueI18n(boolean value) {
        if (value) {
            return Util.parseTranslateableText("options.on").withStyle(ChatFormatting.GREEN);
        } else {
            return Util.parseTranslateableText("options.off").withStyle(ChatFormatting.RED);
        }
    }
}
