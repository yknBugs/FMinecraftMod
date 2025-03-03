package com.ykn.fmod.server.base.util;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;

public class EnumI18n {

    public static MutableText getMessageLocationI18n(MessageLocation type) {
        switch (type) {
            case NONE:
                return Util.parseTranslateableText("fmod.message.type.none");
            case CHAT:
                return Util.parseTranslateableText("fmod.message.type.chat");
            case ACTIONBAR:
                return Util.parseTranslateableText("fmod.message.type.actionbar");
            default:
                return new LiteralText(type.toString());
        }
    }

    public static MutableText getMessageReceiverI18n(MessageReceiver method) {
        switch (method) {
            case ALL:
                return Util.parseTranslateableText("fmod.message.type.toall");
            case OP:
                return Util.parseTranslateableText("fmod.message.type.toop");
            case SELFOP:
                return Util.parseTranslateableText("fmod.message.type.toselfop");
            case TEAMOP:
                return Util.parseTranslateableText("fmod.message.type.toteamop");
            case TEAM:
                return Util.parseTranslateableText("fmod.message.type.toteam");
            case SELF:
                return Util.parseTranslateableText("fmod.message.type.toself");
            case NONE:
                return Util.parseTranslateableText("fmod.message.type.none");
            default:
                return new LiteralText(method.toString());
        }
    }

}
