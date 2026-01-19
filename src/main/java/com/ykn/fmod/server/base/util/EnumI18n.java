package com.ykn.fmod.server.base.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class EnumI18n {

    public static MutableText getMessageLocationI18n(MessageLocation type) {
        switch (type) {
            case NONE:
                return Util.parseTranslateableText("fmod.message.type.none").formatted(Formatting.RED);
            case CHAT:
                return Util.parseTranslateableText("fmod.message.type.chat").formatted(Formatting.GREEN);
            case ACTIONBAR:
                return Util.parseTranslateableText("fmod.message.type.actionbar").formatted(Formatting.YELLOW);
            default:
                return Text.literal(type.toString());
        }
    }

    public static MutableText getMessageReceiverI18n(MessageReceiver method) {
        switch (method) {
            case ALL:
                return Util.parseTranslateableText("fmod.message.type.toall").formatted(Formatting.YELLOW);
            case OP:
                return Util.parseTranslateableText("fmod.message.type.toop").formatted(Formatting.GOLD);
            case SELFOP:
                return Util.parseTranslateableText("fmod.message.type.toselfop").formatted(Formatting.YELLOW);
            case TEAMOP:
                return Util.parseTranslateableText("fmod.message.type.toteamop").formatted(Formatting.YELLOW);
            case TEAM:
                return Util.parseTranslateableText("fmod.message.type.toteam").formatted(Formatting.GREEN);
            case SELF:
                return Util.parseTranslateableText("fmod.message.type.toself").formatted(Formatting.GREEN);
            case NONE:
                return Util.parseTranslateableText("fmod.message.type.none").formatted(Formatting.RED);
            default:
                return Text.literal(method.toString());
        }
    }

    public static MutableText getBooleanValueI18n(boolean value) {
        if (value) {
            return Util.parseTranslateableText("options.on").formatted(Formatting.GREEN);
        } else {
            return Util.parseTranslateableText("options.off").formatted(Formatting.RED);
        }
    }
}
