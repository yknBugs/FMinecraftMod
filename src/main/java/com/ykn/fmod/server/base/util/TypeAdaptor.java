package com.ykn.fmod.server.base.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.text.Text;

public class TypeAdaptor {

    /**
     * Parses an object into a string representation.
     *
     * @param obj the object to be parsed; can be null
     * @return the string representation of the object; returns an empty string if the object is null
     */
    @NotNull
    public static String parseStringLikeObject(@Nullable Object obj) {
        if (obj == null) {
            return "";
        } else if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof Text) {
            Text textObj = (Text) obj;
            return textObj.getString();
        } else {
            return obj.toString();
        }
    }

    /**
     * Parses an object into a Boolean representation.
     *
     * @param obj the object to be parsed; can be null
     * @return the Boolean representation of the object; returns null if parsing fails
     */
    @Nullable
    public static Boolean parseBooleanLikeObject(@Nullable Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof Number) {
            Number num = (Number) obj;
            return num.intValue() != 0;
        } else {
            String str = parseStringLikeObject(obj);
            if ("yes".equalsIgnoreCase(str) || "true".equalsIgnoreCase(str) || "1".equals(str) || "on".equalsIgnoreCase(str) ||
                "enable".equalsIgnoreCase(str) || "y".equalsIgnoreCase(str) || "open".equalsIgnoreCase(str)) {
                return true;
            } else if ("no".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str) || "0".equals(str) || "off".equalsIgnoreCase(str) ||
                       "disable".equalsIgnoreCase(str) || "n".equalsIgnoreCase(str) || "close".equalsIgnoreCase(str)) {
                return false;
            } else {
                return null;
            }
        }
    }
    
    /**
     * Parses an object into a Double representation.
     *
     * @param obj the object to be parsed; can be null
     * @return the Double representation of the object; returns null if parsing fails
     */
    @Nullable
    public static Double parseNumberLikeObject(@Nullable Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Number) {
            Number number = (Number) obj;
            return number.doubleValue();
        } else {
            String str = parseStringLikeObject(obj);
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
