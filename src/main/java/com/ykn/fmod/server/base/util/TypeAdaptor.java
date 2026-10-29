/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

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
 *     that handles Minecraft-specific types like Entity, ItemStack, Block, and Component.</li>
 * <li><b>Double, Boolean, List, Vec3, Vec2:</b> Conversions are independent and may overlap.
 *     Null is returned when a conversion fails; {@link #autoCast()} selects the first success.</li>
 * </ul>
 * <p>
 * <b>Recommended Casting Sequence:</b> Null → Vec3 → Vec2 → List → Double → Boolean → String
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 *     Vec3 position = TypeAdaptor.parse(someObject).asVec3d();
 *     if (position != null) {
 *         // successfully converted to Vec3
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
     * <li>Component → getString() is called</li>
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
        } else if (o instanceof Component component) {
            return component.getString();
        } else if (o instanceof Entity entity) {
            return entity.getDisplayName().getString();
        } else if (o instanceof ItemStack itemStack) {
            return itemStack.getHoverName().getString();
        } else if (o instanceof Block block) {
            return block.getName().getString();
        } else {
            return o.toString();
        }
    }

    /**
     * Converts registry-backed objects to their identifier, without using display names.
     * <p>
     * Entity and entity type use the entity type id; {@link ItemStack} and item use the item id;
     * block state, block entity and block use the block id; levels and chunks use their dimension.
     * Keys, keyed holders, advancements, fluids, effects, potions and sounds expose their own
     * identifiers. Text is parsed as an id.
     * <p>
     * Null, malformed text and unsupported objects return null. This does not validate the id
     * against the registry a particular port expects, and does not change {@link #autoCast()}
     * precedence.
     *
     * @return the identifier, or null when no conversion is available
     */
    @Nullable
    public ResourceLocation asResourceLocation() {
        if (o == null) {
            return null;
        }
        if (o instanceof ResourceLocation id) {
            return id;
        }
        if (o instanceof ResourceKey<?> key) {
            return key.location();
        }
        if (o instanceof Holder<?> holder) {
            return holder.unwrapKey().map(ResourceKey::location).orElse(null);
        }
        if (o instanceof Entity entity) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        }
        if (o instanceof EntityType<?> type) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(type);
        }
        if (o instanceof ItemStack stack) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem());
        }
        if (o instanceof Item item) {
            return BuiltInRegistries.ITEM.getKey(item);
        }
        if (o instanceof BlockState state) {
            return BuiltInRegistries.BLOCK.getKey(state.getBlock());
        }
        if (o instanceof BlockEntity blockEntity) {
            return BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
        }
        if (o instanceof Block block) {
            return BuiltInRegistries.BLOCK.getKey(block);
        }
        if (o instanceof Level level) {
            return level.dimension().location();
        }
        if (o instanceof LevelChunk chunk) {
            return chunk.getLevel().dimension().location();
        }
        if (o instanceof Advancement advancement) {
            return advancement.getId();
        }
        if (o instanceof FluidState fluidState) {
            return BuiltInRegistries.FLUID.getKey(fluidState.getType());
        }
        if (o instanceof Fluid fluid) {
            return BuiltInRegistries.FLUID.getKey(fluid);
        }
        if (o instanceof MobEffectInstance effectInstance) {
            return BuiltInRegistries.MOB_EFFECT.getKey(effectInstance.getEffect());
        }
        if (o instanceof MobEffect effect) {
            return BuiltInRegistries.MOB_EFFECT.getKey(effect);
        }
        if (o instanceof Potion potion) {
            return BuiltInRegistries.POTION.getKey(potion);
        }
        if (o instanceof SoundEvent sound) {
            return sound.getLocation();
        }
        if (o instanceof CharSequence || o instanceof Component) {
            String text = this.asString().strip();
            return text.isEmpty() ? null : ResourceLocation.tryParse(text);
        }
        return null;
    }

    /**
     * Converts the wrapped object to a Boolean value using flexible parsing rules.
     * <p>
     * Returns null if conversion is not possible for the given object type. The following
     * conversions are supported:
     * <ul>
     * <li>null → null</li>
     * <li>Boolean → returned as-is</li>
     * <li>Trimmed string representations (case-insensitive):
     *     <ul>
     *     <li>True values: "yes", "true", "on", "enable", "open"</li>
     *     <li>False values: "no", "false", "off", "disable", "close"</li>
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
     * <li>Other objects → trimmed string representation parsed as a double; null if parsing fails</li>
     * </ul>
     *
     * @return the double value, or null if the object cannot be converted to a double
     */
    @Nullable
    public Double asDouble() {
        if (o == null) {
            return null;
        } else if (o instanceof Number number) {
            return number.doubleValue();
        } else if (o instanceof ItemStack itemStack) {
            return (double) itemStack.getCount();
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
     * Converts the wrapped object to a Vec3 (3D vector) value.
     * <p>
     * Returns null if conversion is not possible. The following conversions are supported:
     * <ul>
     * <li>null → null</li>
     * <li>Vec3 → returned as-is</li>
     * <li>Vec3i → converted to Vec3 with integer coordinates</li>
     * <li>Entity → the entity's position ({@code position()}) is returned</li>
     * <li>HitResult → the hit location ({@code getLocation()}) is returned</li>
     * <li>String format "(x, y, z)" → parsed into Vec3 coordinates; null if parsing fails</li>
     * </ul>
     * <p>
     * Parsing the string representation allows flexible whitespace. For example,
     * "(1, 2, 3)", "( 1 , 2 , 3 )", and "(1.5, -2.3, 0)" are all valid.
     *
     * @return the Vec3 vector, or null if the object cannot be converted to a Vec3
     */
    @Nullable
    public Vec3 asVec3d() {
        if (o == null) {
            return null;
        } else if (o instanceof Vec3) {
            return (Vec3) o;
        } else if (o instanceof Vec3i vec3i) {
            return new Vec3(vec3i.getX(), vec3i.getY(), vec3i.getZ());
        } else if (o instanceof Entity entity) {
            return entity.position();
        } else if (o instanceof HitResult hit) {
            return hit.getLocation();
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
                        return new Vec3(x, y, z);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Converts the wrapped object to a Vec2 (2D float vector) value.
     * <p>
     * Returns null if conversion is not possible. The following conversions are supported:
     * <ul>
     * <li>null → null</li>
     * <li>Vec2 → returned as-is</li>
     * <li>String format "(x, y)" → parsed into Vec2 coordinates; null if parsing fails</li>
     * </ul>
     * <p>
     * Parsing the string representation allows flexible whitespace. For example,
     * "(1.5, 2.5)" and "( 1.5 , 2.5 )" are both valid.
     *
     * @return the Vec2 vector, or null if the object cannot be converted to a Vec2
     */
    @Nullable
    public Vec2 asVec2f() {
        if (o == null) {
            return null;
        } else if (o instanceof Vec2) {
            return (Vec2) o;
        } else {
            String str = this.asString().strip();
            if (str.startsWith("(") && str.endsWith(")")) {
                str = str.substring(1, str.length() - 1).strip();
                String[] parts = str.split(",");
                if (parts.length == 2) {
                    try {
                        float x = Float.parseFloat(parts[0].strip());
                        float y = Float.parseFloat(parts[1].strip());
                        return new Vec2(x, y);
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
     *     is auto-cast (see {@link #autoCast()}); nested lists and embedded commas are not supported</li>
     * </ul>
     * <p>
     * When parsing from string format, each element is white-space trimmed and then auto-cast
     * to its most appropriate type. Splitting is by comma without nesting or quoting support;
     * an empty bracket pair currently produces one empty-string element.
     *
     * @return a list of objects, or null if the object cannot be converted to a list
     */
    @Nullable
    public List<Object> asList() {
        if (o == null) {
            return null;
        } else if (o instanceof List) {
            return new ArrayList<>((List<?>) o);
        } else if (o instanceof Iterable<?> itr) {
            List<Object> list = new ArrayList<>();
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
     * <li>Vec3 (3D vector)</li>
     * <li>Vec2 (2D float vector)</li>
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
     *     Object result2 = TypeAdaptor.parse("(1, 2, 3)").autoCast(); // Returns Vec3
     *     Object result3 = TypeAdaptor.parse("hello").autoCast();     // Returns "hello" (String)
     * </pre>
     *
     * @return the first successful conversion, null for the exact string "null", or the
     *         string representation as a fallback (a null input becomes an empty string)
     */
    @Nullable
    public Object autoCast() {
        String s = this.asString();
        if (s == null || "null".equals(s)) {
            return null;
        }
        Vec3 vec3d = this.asVec3d();
        if (vec3d != null) {
            return vec3d;
        }
        Vec2 vec2f = this.asVec2f();
        if (vec2f != null) {
            return vec2f;
        }
        List<Object> list = this.asList();
        if (list != null) {
            return list;
        }
        Double d = this.asDouble();
        if (d != null) {
            return d;
        }
        Boolean b = this.asBoolean();
        if (b != null) {
            return b;
        }
        return s;
    }

    /**
     * Collapses a list into a single object or null.
     * 
     * <p>This method attempts to reduce a list to its simplest form:
     * <ul>
     *   <li>If the list is null, returns null</li>
     *   <li>If the list contains exactly one element, returns that element</li>
     *   <li>If the list is empty, returns null</li>
     *   <li>Otherwise, returns the converted list</li>
     * </ul>
     *
     * 
     * @return the collapsed result - a single object, a converted list, or null
     * @see #asList()
     */
    @Nullable
    public Object collapseList() {
        List<Object> list = this.asList();
        if (list == null) {
            return null;
        } else if (list.size() == 1) {
            return list.get(0);
        } else if (list.isEmpty()) {
            return null;
        } else {
            return list;
        }
    }
}   
