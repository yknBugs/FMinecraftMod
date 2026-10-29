/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleEvent;
import com.ykn.fmod.server.rule.tool.RuleRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * The catalog of rule event types.
 *
 * <p>Every rule event is a pure declaration: it names the variables that
 * {@code com.ykn.fmod.server.rule.tool.RuleEventHooks} puts into the rule context before the rule
 * runs. There is no per-event class - a new event is one more {@code define} call.
 *
 * <p>A declaration covers the public variables only. Each event also carries hidden
 * {@code __...__} variables holding the live game objects, which the rule system uses to reach the
 * native values (for example when a rule action starts a flow). Hidden variables are not validated,
 * not rendered and not documented by the GUI.
 *
 * <p>Variable types are boxed types on purpose: {@link RuleEvent#validateVariables} uses
 * {@code instanceof} against them, which can never succeed for a primitive.
 */
public final class RuleEvents {

    private RuleEvents() {
    }

    /**
     * One declared variable.
     *
     * @param name     the variable name used in rule parameter bindings
     * @param type     the boxed runtime type the dispatcher supplies
     * @param nullable whether the dispatcher may pass {@code null} for it
     */
    public record Field(String name, Class<?> type, boolean nullable) {
    }

    /**
     * One event type: its type id plus the variables it declares.
     *
     * <p>The type id is the value stored in the {@code "event"} field of a {@code .rule} file, so it
     * must stay stable across releases.
     */
    public record Definition(String type, List<Field> fields) implements RuleEvent {

        public Definition {
            fields = List.copyOf(fields);
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public Set<String> variablesList() {
            Set<String> required = new LinkedHashSet<>();
            for (Field field : fields) {
                if (!field.nullable()) {
                    required.add(field.name());
                }
            }
            return Collections.unmodifiableSet(required);
        }

        @Override
        public Map<String, Class<?>> variablesType() {
            Map<String, Class<?>> types = new LinkedHashMap<>();
            for (Field field : fields) {
                types.put(field.name(), field.type());
            }
            return Collections.unmodifiableMap(types);
        }

        @Override
        public Component render() {
            MutableComponent text = Util.parseTranslatableText(key()).copy();
            text.append("\n").append(Util.parseTranslatableText("fmod.rule.event.variables"));
            for (Field field : fields) {
                if (field.name().startsWith("__")) {
                    continue;
                }
                text.append("\n").append(field.name()).append(" (").append(typeLabel(field.type())).append(")");
            }
            return text;
        }

        /**
         * The localized name of a variable's boxed type, so the tooltip can never drift away from the
         * declared variables the way a hand-written list did.
         *
         * @param type the boxed runtime type of a variable
         * @return the translated label, or the class's simple name for an unmapped type
         */
        private static String typeLabel(Class<?> type) {
            String name = switch (type.getSimpleName()) {
                case "Double" -> "number";
                case "Integer" -> "integer";
                case "Long" -> "long";
                case "Boolean" -> "boolean";
                case "String" -> "string";
                case "UUID" -> "uuid";
                case "Vec3" -> "vec3";
                case "Vec2" -> "vec2";
                case "ResourceLocation" -> "identifier";
                case "CompoundTag" -> "nbt";
                default -> "object";
            };
            return Util.parseTranslatableText("fmod.rule.vartype." + name).getString();
        }

        /**
         * Returns the translation key describing this event, in the form
         * {@code fmod.rule.event.<type without the Event suffix, lowercased>}.
         *
         * @return the translation key of this event
         */
        public String key() {
            String name = type.endsWith("Event") ? type.substring(0, type.length() - "Event".length()) : type;
            return "fmod.rule.event." + name.toLowerCase(Locale.ROOT);
        }
    }

    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();

    static {
        // Events that predate the catalog. Their variable lists are frozen: rules written against
        // them must keep working unchanged, including the rotation vectors being Vec2(pitch, yaw).
        define("PlayerJoinEvent",
                field("player", UUID.class),
                field("x", Double.class),
                field("y", Double.class),
                field("z", Double.class),
                field("pitch", Double.class),
                field("yaw", Double.class),
                field("position", Vec3.class),
                field("rotation", Vec2.class),
                field("health", Double.class),
                field("dimension", ResourceLocation.class),
                optional("biome", ResourceLocation.class),
                field("name", String.class),
                field("cansleep", Boolean.class));
        define("EntityDamageEvent",
                field("entity", UUID.class),
                field("damage", Double.class),
                optional("cause", UUID.class),
                optional("source", UUID.class),
                field("x", Double.class),
                field("y", Double.class),
                field("z", Double.class),
                field("position", Vec3.class),
                field("dimension", ResourceLocation.class),
                optional("biome", ResourceLocation.class),
                field("pitch", Double.class),
                field("yaw", Double.class),
                field("rotation", Vec2.class),
                field("message", String.class),
                field("exhaustion", Double.class),
                field("health", Double.class),
                field("name", String.class));
        define("EntityDeathEvent",
                field("entity", UUID.class),
                optional("cause", UUID.class),
                optional("source", UUID.class),
                field("x", Double.class),
                field("y", Double.class),
                field("z", Double.class),
                field("position", Vec3.class),
                field("dimension", ResourceLocation.class),
                optional("biome", ResourceLocation.class),
                field("message", String.class),
                field("name", String.class));
        define("ProjectileHitEntityEvent",
                field("entity", UUID.class),
                field("shooter", UUID.class),
                field("projectile", UUID.class),
                field("x", Double.class),
                field("y", Double.class),
                field("z", Double.class),
                field("position", Vec3.class),
                field("dimension", ResourceLocation.class),
                optional("biome", ResourceLocation.class),
                field("pitch", Double.class),
                field("yaw", Double.class),
                field("rotation", Vec2.class),
                field("sx", Double.class),
                field("sy", Double.class),
                field("sz", Double.class),
                field("sposition", Vec3.class),
                field("sdimension", ResourceLocation.class),
                field("sbiome", ResourceLocation.class),
                field("spitch", Double.class),
                field("syaw", Double.class),
                field("srotation", Vec2.class),
                field("distance", Double.class),
                field("health", Double.class),
                field("name", String.class),
                field("sname", String.class));
        define("ServerTickEvent",
                field("tick", Integer.class));
        define("PlayerTickEvent",
                field("tick", Integer.class),
                field("player", UUID.class),
                field("x", Double.class),
                field("y", Double.class),
                field("z", Double.class),
                field("position", Vec3.class),
                field("dimension", ResourceLocation.class),
                optional("biome", ResourceLocation.class),
                field("pitch", Double.class),
                field("yaw", Double.class),
                field("rotation", Vec2.class),
                field("health", Double.class),
                field("name", String.class),
                field("afk", Integer.class),
                field("lastpitch", Double.class),
                field("lastyaw", Double.class),
                field("lastrotation", Vec2.class),
                optional("lastbiome", ResourceLocation.class),
                field("lastdimension", ResourceLocation.class),
                optional("cansleep", Boolean.class));

        // Player events. Every one of them carries the same player block and the same two leading
        // fields, so the dispatcher can fill them from one helper.
        playerEvent("PlayerBreakBlockEvent", fields(blockBlock()));
        playerEvent("PlayerPlaceBlockEvent",
                fields(blockBlock(), itemBlock(), new Field[] { field("hand", String.class) }));
        playerEvent("PlayerInteractBlockEvent",
                fields(blockBlock(), itemBlock(), new Field[] {
                    field("hand", String.class),
                    field("face", String.class),
                    field("hitposition", Vec3.class)
                }));
        playerEvent("PlayerInteractEntityEvent",
                fields(itemBlock(), entityBlock(true), new Field[] {
                    field("target", UUID.class),
                    field("targettype", ResourceLocation.class),
                    field("hand", String.class),
                    optional("hitposition", Vec3.class)
                }));
        playerEvent("PlayerLeaveEvent");
        playerEvent("PlayerAfkEvent",
                field("afk", Long.class));
        playerEvent("PlayerReturnEvent",
                field("afk", Long.class));
        playerEvent("PlayerDimensionChangeEvent",
                field("fromdimension", ResourceLocation.class),
                field("todimension", ResourceLocation.class));
        playerEvent("PlayerTeleportEvent",
                fields(coordinates("fromx", "fromy", "fromz"), coordinates("tox", "toy", "toz"), new Field[] {
                    optional("fromdimension", ResourceLocation.class),
                    field("fromposition", Vec3.class),
                    field("toposition", Vec3.class)
                }));
        playerEvent("PlayerItemUseEvent",
                field("item", ResourceLocation.class),
                field("count", Integer.class),
                field("itemnbt", CompoundTag.class),
                field("hand", String.class));
        playerEvent("PlayerItemPickupEvent",
                field("item", ResourceLocation.class),
                field("count", Integer.class),
                field("itemnbt", CompoundTag.class),
                field("itementity", UUID.class));
        playerEvent("PlayerCraftEvent",
                field("item", ResourceLocation.class),
                field("count", Integer.class),
                field("itemnbt", CompoundTag.class));
        playerEvent("PlayerEnchantEvent",
                field("item", ResourceLocation.class),
                field("count", Integer.class),
                field("itemnbt", CompoundTag.class),
                field("cost", Integer.class));
        playerEvent("PlayerTradeEvent",
                field("item", ResourceLocation.class),
                field("count", Integer.class),
                field("itemnbt", CompoundTag.class),
                optional("merchant", UUID.class),
                field("offer", CompoundTag.class));
        playerEvent("PlayerSleepEvent",
                fields(coordinates("tx", "ty", "tz"), new Field[] { field("bedposition", Vec3.class) }));
        playerEvent("PlayerVehicleEnterEvent",
                field("vehicle", UUID.class),
                field("vehicletype", ResourceLocation.class));
        playerEvent("PlayerVehicleLeaveEvent",
                field("vehicle", UUID.class),
                field("vehicletype", ResourceLocation.class));
        playerEvent("PlayerAdvancementEvent",
                field("advancement", ResourceLocation.class),
                field("criterion", String.class));
        playerEvent("PlayerLevelUpEvent",
                field("oldlevel", Integer.class),
                field("newlevel", Integer.class),
                field("delta", Integer.class));
        playerEvent("PlayerHungerChangeEvent",
                field("oldhunger", Integer.class),
                field("newhunger", Integer.class),
                field("saturation", Double.class));
        playerEvent("ContainerOpenEvent",
                fields(containerBlock(), new Field[] {
                    field("containerid", Integer.class),
                    optional("containertype", ResourceLocation.class)
                }));
        playerEvent("ContainerCloseEvent",
                fields(containerBlock(), new Field[] {
                    field("containerid", Integer.class),
                    optional("containertype", ResourceLocation.class)
                }));

        // World events: no player, so they only carry the dimension, the tick and their own fields.
        worldEvent("EntitySpawnEvent",
                fields(coordinates("x", "y", "z"), new Field[] {
                    field("entity", UUID.class),
                    field("entitytype", ResourceLocation.class),
                    field("position", Vec3.class),
                    field("rotation", Vec2.class),
                    field("velocity", Vec3.class)
                }));
        worldEvent("ExplosionEvent",
                fields(coordinates("x", "y", "z"), new Field[] {
                    field("position", Vec3.class),
                    field("radius", Double.class),
                    field("fire", Boolean.class),
                    optional("source", UUID.class),
                    field("blocks", List.class)
                }));
        worldEvent("WeatherChangeEvent",
                field("raining", Boolean.class),
                field("thundering", Boolean.class),
                field("oldraining", Boolean.class),
                field("oldthundering", Boolean.class));
        worldEvent("LightningStrikeEvent",
                fields(coordinates("x", "y", "z"), new Field[] {
                    field("entity", UUID.class),
                    field("position", Vec3.class),
                    optional("cause", UUID.class)
                }));
        worldEvent("ChunkLoadEvent",
                field("chunkx", Integer.class),
                field("chunkz", Integer.class),
                field("position", Vec3.class));
        worldEvent("ChunkUnloadEvent",
                field("chunkx", Integer.class),
                field("chunkz", Integer.class),
                field("position", Vec3.class));
    }

    /** Flattens several field blocks into one varargs list. */
    private static Field[] fields(Field[]... blocks) {
        List<Field> all = new ArrayList<>();
        for (Field[] block : blocks) {
            all.addAll(Arrays.asList(block));
        }
        return all.toArray(new Field[0]);
    }

    /** The item an event is about: its id, its count, its NBT and nothing live. */
    private static Field[] itemBlock() {
        return new Field[] {
            field("item", ResourceLocation.class),
            field("count", Integer.class),
            field("itemnbt", CompoundTag.class)
        };
    }

    /**
     * The block a container was opened on. Not every menu has one (the player's own crafting grid,
     * for example), so every field here is optional.
     */
    private static Field[] containerBlock() {
        return new Field[] {
            optional("block", ResourceLocation.class),
            optional("blockposition", Vec3.class),
            optional("bx", Double.class),
            optional("by", Double.class),
            optional("bz", Double.class)
        };
    }
    private static Field field(String name, Class<?> type) {
        return new Field(name, type, false);
    }

    private static Field optional(String name, Class<?> type) {
        return new Field(name, type, true);
    }

    private static void define(String type, Field... fields) {
        put(type, Arrays.asList(fields));
    }

    /**
     * The block of variables every player event carries: who the player is, where they are (both as
     * a vector and as separate components), how they look, and their live state. The hidden
     * __player__/__world__/__name__ aliases are added by the dispatcher and are intentionally not
     * part of the metadata.
     */
    private static void playerEvent(String type, Field... fields) {
        List<Field> all = new ArrayList<>();
        all.add(field("dimension", ResourceLocation.class));
        all.add(field("tick", Integer.class));
        all.add(field("player", UUID.class));
        all.add(field("name", String.class));
        all.add(field("position", Vec3.class));
        all.add(field("rotation", Vec2.class));
        all.add(field("velocity", Vec3.class));
        all.add(field("health", Double.class));
        all.add(field("hunger", Integer.class));
        all.add(field("level", Integer.class));
        all.addAll(Arrays.asList(playerCoordinates()));
        all.addAll(Arrays.asList(fields));
        put(type, all);
    }

    /**
     * The separate components of a player's position and view, plus the two values rules keep asking
     * for that only a live player can answer.
     */
    private static Field[] playerCoordinates() {
        return new Field[] {
            field("x", Double.class),
            field("y", Double.class),
            field("z", Double.class),
            field("pitch", Double.class),
            field("yaw", Double.class),
            optional("biome", ResourceLocation.class),
            field("cansleep", Boolean.class)
        };
    }

    /**
     * Splits a position into the three components a rule usually needs, under the given names so an
     * event can carry several positions without colliding.
     */
    private static Field[] coordinates(String x, String y, String z) {
        return new Field[] { field(x, Double.class), field(y, Double.class), field(z, Double.class) };
    }

    /**
     * The block a block-interaction event happened on: its id, its vector position and the same
     * position split into bx/by/bz for rules that only need one axis.
     */
    private static Field[] blockBlock() {
        List<Field> fields = new ArrayList<>();
        fields.add(field("block", ResourceLocation.class));
        fields.add(field("blockposition", Vec3.class));
        fields.addAll(Arrays.asList(coordinates("bx", "by", "bz")));
        fields.add(field("blocknbt", CompoundTag.class));
        return fields.toArray(new Field[0]);
    }

    /**
     * The entity an event is about. A player event already owns the unprefixed names, so a prefixed
     * variant carries the entity's position and name as ex/ey/ez, eposition, ebiome and ename.
     */
    private static Field[] entityBlock(boolean prefixed) {
        String x = prefixed ? "ex" : "x";
        String y = prefixed ? "ey" : "y";
        String z = prefixed ? "ez" : "z";
        List<Field> fields = new ArrayList<>();
        fields.add(field("entity", UUID.class));
        fields.addAll(Arrays.asList(coordinates(x, y, z)));
        fields.add(field(prefixed ? "eposition" : "position", Vec3.class));
        fields.add(optional(prefixed ? "ebiome" : "biome", ResourceLocation.class));
        fields.add(field(prefixed ? "ename" : "name", String.class));
        return fields.toArray(new Field[0]);
    }
    private static void worldEvent(String type, Field... fields) {
        List<Field> all = new ArrayList<>();
        all.add(field("dimension", ResourceLocation.class));
        all.add(field("tick", Integer.class));
        all.addAll(Arrays.asList(fields));
        put(type, all);
    }

    private static void put(String type, List<Field> fields) {
        DEFINITIONS.put(type, new Definition(type, fields));
    }

    /**
     * Returns the declaration registered under {@code type}.
     *
     * @param type the event type id stored in a {@code .rule} file
     * @return the matching declaration
     * @throws NullPointerException if no event is registered under that id
     */
    public static Definition get(String type) {
        return Objects.requireNonNull(DEFINITIONS.get(type), type);
    }

    /**
     * Returns every declaration, in registration order.
     *
     * @return an unmodifiable view of all declarations
     */
    public static List<Definition> all() {
        return Collections.unmodifiableList(new ArrayList<>(DEFINITIONS.values()));
    }

    /**
     * Registers every event declaration with {@link RuleRegistry}.
     *
     * <p>Called once from {@link RuleRegistry#registerDefault()}, before any rule is loaded.
     */
    public static void register() {
        for (Definition definition : all()) {
            RuleRegistry.register(definition.type(), () -> definition);
        }
    }
}
