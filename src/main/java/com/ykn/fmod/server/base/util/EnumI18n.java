package com.ykn.fmod.server.base.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class EnumI18n {

    public static MutableText getMessageTypeI18n(MessageType type) {
        switch (type) {
            case NONE:
                return Util.parseTranslateableText("fmod.message.type.none");
            case CHAT:
                return Util.parseTranslateableText("fmod.message.type.chat");
            case ACTIONBAR:
                return Util.parseTranslateableText("fmod.message.type.actionbar");
            default:
                return Text.literal(type.toString());
        }
    }

}
