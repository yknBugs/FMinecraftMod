/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

/**
 * Utility class for adapting and parsing different object types.
 * Class Cast Rules:
 * - String: Available for all kinds of objects.
 * - Double, Boolean, Vec3d, Vec2f: One single object can only be cast to one of these types.
 * Recommended try cast sequence: Null -> Vec3d -> Vec2f -> List -> Double -> Boolean -> String
 */
public class TypeAdaptor {

    private Object o;

    private TypeAdaptor(Object o) {
        this.o = o;
    }

    public static TypeAdaptor parse(Object o) {
        return new TypeAdaptor(o);
    }

    @NotNull
    public String asString() {
        if (o == null) {
            return "";
        } else if (o instanceof String) {
            return (String) o;
        } else if (o instanceof Text) {
            Text textObj = (Text) o;
            return textObj.getString();
        } else if (o instanceof Entity) {
            Entity entity = (Entity) o;
            return entity.getDisplayName().getString();
        } else if (o instanceof ItemStack) {
            ItemStack itemStack = (ItemStack) o;
            return itemStack.getName().getString();
        } else if (o instanceof Block) {
            Block block = (Block) o;
            return block.getName().getString();
        } else {
            return o.toString();
        }
    }

    @Nullable
    public Boolean asBoolean() {
        if (o == null) {
            return null;
        } else if (o instanceof Boolean) {
            return (Boolean) o;
        } else {
            String str = this.asString().strip();
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
            String str = this.asString().strip();
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    @Nullable
    public Vec3d asVec3d() {
        if (o == null) {
            return null;
        } else if (o instanceof Vec3d) {
            return (Vec3d) o;
        } else if (o instanceof Vec3i) {
            Vec3i vec3i = (Vec3i) o;
            Vec3d vec3d = new Vec3d(vec3i.getX(), vec3i.getY(), vec3i.getZ());
            return vec3d;
        } else if (o instanceof Entity) {
            Entity entity = (Entity) o;
            return entity.getPos();
        } else {
            // String will usually be in format (x, y, z) in Minecraft
            String str = this.asString().strip();
            if (str.startsWith("(") && str.endsWith(")")) {
                str = str.substring(1, str.length() - 1).strip();
                String[] parts = str.split(",");
                if (parts.length == 3) {
                    try {
                        double x = Double.parseDouble(parts[0].strip());
                        double y = Double.parseDouble(parts[1].strip());
                        double z = Double.parseDouble(parts[2].strip());
                        return new Vec3d(x, y, z);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    @Nullable
    public Vec2f asVec2f() {
        if (o == null) {
            return null;
        } else if (o instanceof Vec2f) {
            return (Vec2f) o;
        } else {
            String str = this.asString().strip();
            if (str.startsWith("(") && str.endsWith(")")) {
                str = str.substring(1, str.length() - 1).strip();
                String[] parts = str.split(",");
                if (parts.length == 2) {
                    try {
                        float x = Float.parseFloat(parts[0].strip());
                        float y = Float.parseFloat(parts[1].strip());
                        return new Vec2f(x, y);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }     
            return null;       
        }
    }

    @Nullable
    public List<Object> asList() {
        if (o == null) {
            return null;
        } else if (o instanceof List) {
            return new ArrayList<>((List<?>) o);
        } else if (o instanceof Iterable) {
            List<Object> list = new ArrayList<>();
            Iterable<?> itr = (Iterable<?>) o;
            for (Object item : itr) {
                list.add(item);
            }
            return list;
        } else {
            // List will usually be in format [a, b, c] in Java toString method
            String str = this.asString().strip();
            if (str.startsWith("[") && str.endsWith("]")) {
                str = str.substring(1, str.length() - 1).strip();
                String[] parts = str.split(",");
                List<Object> list = new ArrayList<>();
                for (String part : parts) {
                    list.add(TypeAdaptor.parse(part.strip()).autoCast());
                }
                return list;
            } 
            return null;
        }
    }

    @Nullable
    public Object autoCast() {
        String s = this.asString();
        Vec3d vec3d = this.asVec3d();
        Vec2f vec2f = this.asVec2f();
        List<Object> list = this.asList();
        Double d = this.asDouble();
        Boolean b = this.asBoolean();
        if (s == null || "null".equals(s)) {
            return null;
        } else if (vec3d != null) {
            return vec3d;
        } else if (vec2f != null) {
            return vec2f;
        } else if (list != null) {
            return list;
        } else if (d != null) {
            return d;
        } else if (b != null) {
            return b;
        } else {
            return s;
        }
    }
}   
