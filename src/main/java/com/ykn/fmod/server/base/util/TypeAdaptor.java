package com.ykn.fmod.server.base.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for adapting and parsing different object types.
 * Class Cast Rules:
 * - String: Available for all kinds of objects.
 * - Double, Boolean, Vec3d, Vec2f: One single object can only be cast to one of these types.
 * Recommended try cast sequence: Null -> Vec3d -> Vec2f -> Double -> Boolean -> String
 */
public class TypeAdaptor {

    private Object o;

    private TypeAdaptor(Object o) {
        this.o = o;
    }

    public static TypeAdaptor parse(Object o) {
        return new TypeAdaptor(o);
    }

    /**
     * Parses an object into a string representation.
     * @return the string representation of the object; returns an empty string if the object is null
     */
    @Nonnull
    public String asString() {
        if (o == null) {
            return "";
        } else if (o instanceof String) {
            return (String) o;
        } else if (o instanceof Component) {
            Component component = (Component) o;
            return component.getString();
        } else if (o instanceof Entity) {
            Entity entity = (Entity) o;
            return entity.getDisplayName().getString();
        } else if (o instanceof ItemStack) {
            ItemStack itemStack = (ItemStack) o;
            return itemStack.getHoverName().getString();
        } else if (o instanceof Block) {
            Block block = (Block) o;
            return block.getName().getString();
        } else {
            return o.toString();
        }
    }

    /**
     * Parses an object into a Boolean representation.
     * @return the Boolean representation of the object; returns null if parsing fails
     */
    @Nullable
    public Boolean asBoolean() {
        if (o == null) {
            return null;
        } else if (o instanceof Boolean) {
            return (Boolean) o;
        } else {
            String str = this.asString().trim();
            if ("yes".equalsIgnoreCase(str) || "true".equalsIgnoreCase(str) || "on".equalsIgnoreCase(str) ||
                "enable".equalsIgnoreCase(str) || "y".equalsIgnoreCase(str) || "open".equalsIgnoreCase(str)) {
                return true;
            } else if ("no".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str) || "off".equalsIgnoreCase(str) ||
                       "disable".equalsIgnoreCase(str) || "n".equalsIgnoreCase(str) || "close".equalsIgnoreCase(str)) {
                return false;
            } else {
                return null;
            }
        }
    }
    
    /**
     * Parses an object into a Double representation.
     * @return the Double representation of the object; returns null if parsing fails
     */
    @Nullable
    public Double asDouble() {
        if (o == null) {
            return null;
        } else if (o instanceof Number) {
            Number number = (Number) o;
            return number.doubleValue();
        } else if (o instanceof ItemStack) {
            ItemStack itemStack = (ItemStack) o;
            double count = itemStack.getCount();
            return count;
        } else {
            String str = this.asString().trim();
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /**
     * Parses an object into a Vec3d representation.
     * @return the Vec3d representation of the object; returns null if parsing fails
     */
    @Nullable
    public Vec3 asVec3d() {
        if (o instanceof Vec3) {
            return (Vec3) o;
        } else if (o instanceof Vec3i) {
            Vec3i vec3i = (Vec3i) o;
            Vec3 vec3d = new Vec3(vec3i.getX(), vec3i.getY(), vec3i.getZ());
            return vec3d;
        } else if (o instanceof Entity) {
            Entity entity = (Entity) o;
            return entity.position();
        } else {
            // String will usually be in format (x, y, z) in Minecraft
            String str = this.asString().trim();
            str = str.replace("(", "").replace(")", "");
            String[] parts = str.split(",");
            if (parts.length == 3) {
                try {
                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    double z = Double.parseDouble(parts[2].trim());
                    return new Vec3(x, y, z);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * Parses an object into a Vec2f representation.
     * @return the Vec2f representation of the object; returns null if parsing fails
     */
    @Nullable
    public Vec2 asVec2f() {
        if (o instanceof Vec2) {
            return (Vec2) o;
        } else {
            String str = this.asString().trim();
            str = str.replace("(", "").replace(")", "");
            String[] parts = str.split(",");
            if (parts.length == 2) {
                try {
                    float x = Float.parseFloat(parts[0].trim());
                    float y = Float.parseFloat(parts[1].trim());
                    return new Vec2(x, y);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
    }
}   
