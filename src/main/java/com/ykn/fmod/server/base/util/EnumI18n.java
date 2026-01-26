/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class EnumI18n {

    public static MutableComponent getMessageLocationI18n(MessageLocation type) {
        switch (type) {
            case NONE:
                return Util.parseTranslatableText("fmod.message.type.none").withStyle(ChatFormatting.RED);
            case CHAT:
                return Util.parseTranslatableText("fmod.message.type.chat").withStyle(ChatFormatting.GREEN);
            case ACTIONBAR:
                return Util.parseTranslatableText("fmod.message.type.actionbar").withStyle(ChatFormatting.YELLOW);
            default:
                return Component.literal(type.toString());
        }
    }

    public static MutableComponent getMessageReceiverI18n(MessageReceiver method) {
        switch (method) {
            case ALL:
                return Util.parseTranslatableText("fmod.message.type.toall").withStyle(ChatFormatting.YELLOW);
            case OP:
                return Util.parseTranslatableText("fmod.message.type.toop").withStyle(ChatFormatting.GOLD);
            case SELFOP:
                return Util.parseTranslatableText("fmod.message.type.toselfop").withStyle(ChatFormatting.YELLOW);
            case TEAMOP:
                return Util.parseTranslatableText("fmod.message.type.toteamop").withStyle(ChatFormatting.YELLOW);
            case TEAM:
                return Util.parseTranslatableText("fmod.message.type.toteam").withStyle(ChatFormatting.GREEN);
            case SELF:
                return Util.parseTranslatableText("fmod.message.type.toself").withStyle(ChatFormatting.GREEN);
            case NONE:
                return Util.parseTranslatableText("fmod.message.type.none").withStyle(ChatFormatting.RED);
            default:
                return Component.literal(method.toString());
        }
    }

    public static MutableComponent getBooleanValueI18n(boolean value) {
        if (value) {
            return Util.parseTranslatableText("options.on").withStyle(ChatFormatting.GREEN);
        } else {
            return Util.parseTranslatableText("options.off").withStyle(ChatFormatting.RED);
        }
    }
}
