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
 * A flexible type conversion utility that safely adapts arbitrary Java objects to common types.
 * <p>
 * This class provides a fluent API for converting objects to different data types with graceful
 * fallbacks and sensible defaults. It is particularly useful when dealing with dynamically typed
 * data from configuration files, command arguments, or deserialized sources.
 * <p>
 * <b>Casting Rules:</b>
 * <ul>
 * <li><b>String:</b> Available for all kinds of objects. Implements a sophisticated toString()
 *     that handles Minecraft-specific types like Entity, ItemStack, Block, and Text.</li>
 * <li><b>Double, Boolean, List, Vec3d, Vec2f:</b> One single object can only be successfully cast to
 *     one of these types. Null values are returned if conversion is not possible.</li>
 * </ul>
 * <p>
 * <b>Recommended Casting Sequence:</b> Null → Vec3d → Vec2f → List → Double → Boolean → String
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 *     Vec3d position = TypeAdaptor.parse(someObject).asVec3d();
 *     if (position != null) {
 *         // successfully converted to Vec3d
 *     }
 * </pre>
 */
public class TypeAdaptor {

    /** 
     * The object being adapted for type conversion. 
     */
    private Object o;

    /**
     * Constructs a new TypeAdaptor wrapping the specified object.
     *
     * @param o the object to adapt; may be null
     */
    private TypeAdaptor(Object o) {
        this.o = o;
    }

    /**
     * Creates a new TypeAdaptor instance for the given object.
     * <p>
     * This is the primary factory method for creating adaptor instances. It provides a clean,
     * fluent API for type conversions.
     *
     * @param o the object to adapt; may be null
     * @return a new TypeAdaptor wrapping the specified object
     */
    public static TypeAdaptor parse(Object o) {
        return new TypeAdaptor(o);
    }

    /**
     * Converts the wrapped object to a String representation.
     * <p>
     * This method never returns null and provides intelligent handling of Minecraft-specific types:
     * <ul>
     * <li>null → empty string ""</li>
     * <li>String → returned as-is</li>
     * <li>Text → getString() is called</li>
     * <li>Entity → entity display name is retrieved</li>
     * <li>ItemStack → item display name is retrieved</li>
     * <li>Block → block name is retrieved</li>
     * <li>Any other type → toString() is called</li>
     * </ul>
     *
     * @return the string representation of the wrapped object; never null, empty string for null values
     */
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

    /**
     * Converts the wrapped object to a Boolean value using flexible parsing rules.
     * <p>
     * Returns null if conversion is not possible for the given object type. The following
     * conversions are supported:
     * <ul>
     * <li>null → null</li>
     * <li>Boolean → returned as-is</li>
     * <li>String values (case-insensitive):
     *     <ul>
     *     <li>True values: "yes", "true", "on", "enable", "y", "open"</li>
     *     <li>False values: "no", "false", "off", "disable", "n", "close"</li>
     *     <li>Any other string → null</li>
     *     </ul>
     * </li>
     * </ul>
     *
     * @return the boolean value, or null if the object cannot be converted to a boolean
     */
    @Nullable
    public Boolean asBoolean() {
        if (o == null) {
            return null;
        } else if (o instanceof Boolean) {
            return (Boolean) o;
        } else {
            String str = this.asString().strip();
            if ("yes".equalsIgnoreCase(str) || "true".equalsIgnoreCase(str) || "on".equalsIgnoreCase(str) ||
                "enable".equalsIgnoreCase(str) || "open".equalsIgnoreCase(str)) {
                return true;
            } else if ("no".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str) || "off".equalsIgnoreCase(str) ||
                       "disable".equalsIgnoreCase(str) || "close".equalsIgnoreCase(str)) {
                return false;
            } else {
                return null;
            }
        }
    }
    
    /**
     * Converts the wrapped object to a Double value.
     * <p>
     * Returns null if conversion is not possible. The following conversions are supported:
     * <ul>
     * <li>null → null</li>
     * <li>Number → doubleValue() is called</li>
     * <li>ItemStack → item count is returned as a double</li>
     * <li>String → parsed as a double value; null if parsing fails</li>
     * </ul>
     *
     * @return the double value, or null if the object cannot be converted to a double
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
            String str = this.asString().strip();
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /**
     * Converts the wrapped object to a Vec3d (3D vector) value.
     * <p>
     * Returns null if conversion is not possible. The following conversions are supported:
     * <ul>
     * <li>null → null</li>
     * <li>Vec3d → returned as-is</li>
     * <li>Vec3i → converted to Vec3d with integer coordinates</li>
     * <li>Entity → the entity's position (getPos()) is returned</li>
     * <li>String format "(x, y, z)" → parsed into Vec3d coordinates; null if parsing fails</li>
     * </ul>
     * <p>
     * String parsing is case-insensitive and allows flexible whitespace. For example,
     * "(1, 2, 3)", "( 1 , 2 , 3 )", and "(1.5, -2.3, 0)" are all valid.
     *
     * @return the Vec3d vector, or null if the object cannot be converted to a Vec3d
     */
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

    /**
     * Converts the wrapped object to a Vec2f (2D float vector) value.
     * <p>
     * Returns null if conversion is not possible. The following conversions are supported:
     * <ul>
     * <li>null → null</li>
     * <li>Vec2f → returned as-is</li>
     * <li>String format "(x, y)" → parsed into Vec2f coordinates; null if parsing fails</li>
     * </ul>
     * <p>
     * String parsing is case-insensitive and allows flexible whitespace. For example,
     * "(1.5, 2.5)" and "( 1.5 , 2.5 )" are both valid.
     *
     * @return the Vec2f vector, or null if the object cannot be converted to a Vec2f
     */
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

    /**
     * Converts the wrapped object to a List of Objects.
     * <p>
     * Returns null if conversion is not possible. The following conversions are supported:
     * <ul>
     * <li>null → null</li>
     * <li>List → a new ArrayList copy is returned</li>
     * <li>Iterable → all items are collected into a new ArrayList</li>
     * <li>String format "[a, b, c]" → parsed into a list of elements, where each element
     *     is recursively auto-cast (see {@link #autoCast()}); null if parsing fails</li>
     * </ul>
     * <p>
     * When parsing from string format, each element is white-space trimmed and then auto-cast
     * to its most appropriate type. This allows for heterogeneous lists containing mixed types.
     *
     * @return a list of objects, or null if the object cannot be converted to a list
     */
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
                // We will not parse nested lists here
                String[] parts = str.split(",");
                List<Object> list = new ArrayList<>();
                for (String part : parts) {
                    if (part.strip().startsWith("[") && part.strip().endsWith("]")) {
                        // Avoid recursion
                        list.add(part.strip());
                    } else {
                        list.add(TypeAdaptor.parse(part.strip()).autoCast());
                    }       
                }
                return list;
            } 
            return null;
        }
    }

    /**
     * Automatically casts the wrapped object to its most appropriate type based on semantic analysis.
     * <p>
     * This method attempts to intelligently determine the best type for an object by testing
     * conversions in a specific priority order. The casting priority is:
     * <ol>
     * <li>null (if string is "null")</li>
     * <li>Vec3d (3D vector)</li>
     * <li>Vec2f (2D float vector)</li>
     * <li>List</li>
     * <li>Double (numeric)</li>
     * <li>Boolean</li>
     * <li>String (fallback, always succeeds)</li>
     * </ol>
     * <p>
     * The first successful conversion is returned. This method is useful for parsing
     * dynamically typed configuration values where the type is not known in advance.
     * <p>
     * <b>Example:</b>
     * <pre>
     *     Object result1 = TypeAdaptor.parse("123").autoCast();      // Returns 123.0 (Double)
     *     Object result2 = TypeAdaptor.parse("(1, 2, 3)").autoCast(); // Returns Vec3d
     *     Object result3 = TypeAdaptor.parse("hello").autoCast();     // Returns "hello" (String)
     * </pre>
     *
     * @return the auto-cast object in its most appropriate type, or null if the object is null
     *         and the string representation is "null"
     */
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
