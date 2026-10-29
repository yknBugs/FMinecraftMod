/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ykn.fmod.server.base.util.ItemComponents;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.rule.tool.RuleNbt;
import com.ykn.fmod.server.rule.tool.ThrowingBiFunction;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * A parameter "shape" usable in a {@link com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder} {@code const} node, pairing the
 * Brigadier argument node to build, the extractor that reads the resolved value back out of a
 * {@link CommandContext}, and a JSON codec for persisting the constant value in a {@code .rule}
 * file.
 *
 * <p>The constructor is public and every field is final, so third-party code can declare its own
 * {@code ParamKind}s the same way the built-in constants below do - there is no closed taxonomy to
 * extend. A custom kind used by a condition/action's {@link RequiredParamMetadata} works
 * automatically with JSON (de)serialization and command-line editing via
 * {@link com.ykn.fmod.server.rule.tool.RuleComponentFactory}; GUI editing (the client-side rule
 * editor) additionally requires registering a widget builder with the client-side
 * {@code ParamWidgetRegistry}, since server code cannot invoke client code.
 *
 * <p>{@link #valueType()} additionally lets {@link com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder} filter {@code var}/
 * {@code mix} variable-name suggestions down to variables whose declared type
 * ({@link com.ykn.fmod.server.rule.core.RuleEvent#variablesType()}) actually matches this kind.
 *
 * @param <T> the Java type this kind resolves to
 * @see com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder
 */
public final class ParamKind<T> {

    private final Function<String, RequiredArgumentBuilder<CommandSourceStack, ?>> nodeFactory;

    private final ThrowingBiFunction<CommandContext<CommandSourceStack>, String, T, CommandSyntaxException> extractor;

    private final boolean hasBuiltinSuggestions;

    private final Class<T> valueType;

    private final Function<T, JsonElement> jsonSerializer;

    private final Function<JsonElement, T> jsonDeserializer;

    /**
     * Constructs a new {@code ParamKind}.
     *
     * @param nodeFactory        builds the Brigadier argument node for this kind's constant value
     * @param extractor          extracts the resolved value from a command context
     * @param hasBuiltinSuggestions whether {@code nodeFactory} already attaches a real suggester
     * @param valueType          the boxed Java type this kind resolves to
     * @param jsonSerializer     converts a value of this kind to a {@link JsonElement} for persistence
     * @param jsonDeserializer   converts a persisted {@link JsonElement} back to a value of this kind;
     *                           may throw an unchecked exception on malformed input, which callers
     *                           (e.g. {@link RuleParameter#fromJson}) already catch and log
     */
    public ParamKind(Function<String, RequiredArgumentBuilder<CommandSourceStack, ?>> nodeFactory,
            ThrowingBiFunction<CommandContext<CommandSourceStack>, String, T, CommandSyntaxException> extractor,
            boolean hasBuiltinSuggestions, Class<T> valueType,
            Function<T, JsonElement> jsonSerializer, Function<JsonElement, T> jsonDeserializer) {
        this.nodeFactory = nodeFactory;
        this.extractor = extractor;
        this.hasBuiltinSuggestions = hasBuiltinSuggestions;
        this.valueType = valueType;
        this.jsonSerializer = jsonSerializer;
        this.jsonDeserializer = jsonDeserializer;
    }

    /**
     * Builds the Brigadier argument node for this kind's constant value.
     *
     * @param argName the argument name to register
     * @return the constructed node
     */
    public RequiredArgumentBuilder<CommandSourceStack, ?> buildNode(String argName) {
        return nodeFactory.apply(argName);
    }

    /**
     * Extracts the resolved value from the given command context.
     *
     * @param ctx     the command context
     * @param argName the argument name to read
     * @return the extracted value
     * @throws CommandSyntaxException if the underlying Brigadier extractor throws
     */
    public T extract(CommandContext<CommandSourceStack> ctx, String argName) throws CommandSyntaxException {
        return extractor.apply(ctx, argName);
    }

    /**
     * Serializes a value of this kind to a {@link JsonElement} for persistence in a {@code .rule} file.
     *
     * @param value the value to serialize
     * @return the serialized JSON element
     */
    public JsonElement toJson(T value) {
        return jsonSerializer.apply(value);
    }

    /**
     * Deserializes a persisted {@link JsonElement} back into a value of this kind.
     *
     * @param element the JSON element to deserialize
     * @return the deserialized value
     */
    public T fromJson(JsonElement element) {
        return jsonDeserializer.apply(element);
    }

    /**
     * Whether {@link #buildNode(String)} already attaches a real, non-empty suggester (e.g. a
     * vanilla entity/dimension/boolean completion list).
     *
     * @return {@code true} if this kind's node has built-in suggestions
     */
    public boolean hasBuiltinSuggestions() {
        return hasBuiltinSuggestions;
    }

    /**
     * The boxed Java type this kind resolves to, used to filter {@code var}/{@code mix}
     * variable-name suggestions to type-compatible event variables.
     *
     * @return the value type
     */
    public Class<?> valueType() {
        return this == ITEM_COMPONENTS ? DataComponentPatch.class : valueType;
    }

    /**
     * A single entity selector (e.g. {@code @p}, {@code @e[type=cow]}).
     * Resolves to the entity's {@link UUID}.
     *
     * <p>Has built-in entity-name suggestions.
     */
    public static final ParamKind<UUID> ENTITY = new ParamKind<>(
        name -> Commands.argument(name, EntityArgument.entity()),
        (ctx, name) -> EntityArgument.getEntity(ctx, name).getUUID(),
        true, UUID.class,
        v -> new JsonPrimitive(v.toString()), e -> UUID.fromString(e.getAsString()));

    /**
     * A single online player selector.
     * Resolves to the player's {@link UUID}.
     *
     * <p>Has built-in player-name suggestions.
     */
    public static final ParamKind<UUID> PLAYER = new ParamKind<>(
        name -> Commands.argument(name, EntityArgument.player()),
        (ctx, name) -> EntityArgument.getPlayer(ctx, name).getUUID(),
        true, UUID.class,
        v -> new JsonPrimitive(v.toString()), e -> UUID.fromString(e.getAsString()));

    /**
     * A player selector that may match multiple players (e.g. {@code @a}).
     * Resolves to a {@link List} of {@link UUID}s.
     *
     * <p>Has built-in player-name suggestions.
     */
    @SuppressWarnings("unchecked")
    public static final ParamKind<List<UUID>> PLAYERS = new ParamKind<>(
        name -> Commands.argument(name, EntityArgument.players()),
        (ctx, name) -> EntityArgument.getPlayers(ctx, name).stream().map(ServerPlayer::getUUID).collect(Collectors.toList()),
        true, (Class<List<UUID>>) (Class<?>) List.class,
        list -> {
            JsonArray array = new JsonArray();
            for (UUID uuid : list) {
                array.add(uuid.toString());
            }
            return array;
        },
        e -> {
            List<UUID> uuids = new ArrayList<>();
            for (JsonElement elem : e.getAsJsonArray()) {
                uuids.add(UUID.fromString(elem.getAsString()));
            }
            return uuids;
        });

    /**
     * A 3D coordinate (e.g. {@code ~ ~ ~}, {@code 0 64 0}).
     * Resolves to a {@link Vec3}.
     *
     * <p>Has built-in coordinate suggestions.
     */
    public static final ParamKind<Vec3> POSITION = new ParamKind<>(
        name -> Commands.argument(name, Vec3Argument.vec3()), Vec3Argument::getVec3, true, Vec3.class,
        v -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", v.x);
            obj.addProperty("y", v.y);
            obj.addProperty("z", v.z);
            return obj;
        },
        e -> {
            JsonObject obj = e.getAsJsonObject();
            return new Vec3(obj.get("x").getAsDouble(), obj.get("y").getAsDouble(), obj.get("z").getAsDouble());
        });

    private static ResourceLocation parseResourceLocation(JsonElement e) {
        String s = e.getAsString();
        ResourceLocation rl = ResourceLocation.tryParse(s);
        if (rl == null) {
            throw new IllegalArgumentException("Invalid ResourceLocation: " + s);
        }
        return rl;
    }

    /**
     * An absolute vector, as used for velocities.
     * Resolves to a {@link Vec3}, serialized as {@code x}/{@code y}/{@code z}.
     *
     * <p>Relative and local coordinates are rejected, because a velocity has no anchor to be
     * relative to. Has built-in coordinate suggestions.
     */
    public static final ParamKind<Vec3> VECTOR = new ParamKind<>(
        name -> Commands.argument(name, Vec3Argument.vec3(false)),
        (ctx, name) -> {
            Coordinates coordinates = Vec3Argument.getCoordinates(ctx, name);
            if (coordinates.isXRelative() || coordinates.isYRelative() || coordinates.isZRelative()) {
                throw new IllegalArgumentException("Vector components must be absolute");
            }
            return coordinates.getPosition(ctx.getSource());
        },
        true, Vec3.class, POSITION::toJson, POSITION::fromJson);

    /**
     * A rotation in degrees, stored as {@code Vec2(pitch, yaw)}.
     * Resolves to a {@link Vec2}.
     *
     * <p>Vanilla command syntax is yaw then pitch, so the text is read in that order. Pitch is
     * validated by the consuming action, which rejects values outside {@code [-90, 90]}.
     */
    public static final ParamKind<Vec2> ROTATION = new ParamKind<>(
        name -> Commands.argument(name, RotationArgument.rotation()),
        (ctx, name) -> RotationArgument.getRotation(ctx, name).getRotation(ctx.getSource()),
        false, Vec2.class,
        v -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("pitch", v.x);
            obj.addProperty("yaw", v.y);
            return obj;
        },
        e -> {
            JsonObject obj = e.getAsJsonObject();
            return new Vec2(obj.get("pitch").getAsFloat(), obj.get("yaw").getAsFloat());
        });

    /**
     * A dimension identifier (e.g. {@code minecraft:overworld}).
     * Resolves to the dimension's {@link ResourceLocation}.
     *
     * <p>Has built-in dimension-name suggestions.
     */
    public static final ParamKind<ResourceLocation> DIMENSION = new ParamKind<>(
        name -> Commands.argument(name, DimensionArgument.dimension()),
        (ctx, name) -> DimensionArgument.getDimension(ctx, name).dimension().location(),
        true, ResourceLocation.class,
        v -> new JsonPrimitive(v.toString()), ParamKind::parseResourceLocation);

    /**
     * A block identifier (e.g. {@code minecraft:stone}).
     * Resolves to a {@link ResourceLocation}.
     *
     * <p>No built-in suggestions.
     */
    public static final ParamKind<ResourceLocation> BLOCK_ID = new ParamKind<>(
        name -> Commands.argument(name, ResourceLocationArgument.id()), ResourceLocationArgument::getId,
        false, ResourceLocation.class,
        v -> new JsonPrimitive(v.toString()), ParamKind::parseResourceLocation);

    /**
     * An entity type identifier (e.g. {@code minecraft:creeper}).
     * Resolves to a {@link ResourceLocation}.
     *
     * <p>Has built-in suggestions from vanilla's summonable-entity list.
     */
    public static final ParamKind<ResourceLocation> ENTITY_TYPE_ID = new ParamKind<>(
        name -> Commands.argument(name, ResourceLocationArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES), ResourceLocationArgument::getId,
        true, ResourceLocation.class,
        v -> new JsonPrimitive(v.toString()), ParamKind::parseResourceLocation);

    /**
     * A resource identifier without registry-specific validation or suggestions
     * (e.g. an item or biome id).
     * Resolves to a {@link ResourceLocation}.
     *
     * <p>No built-in suggestions.
     */
    public static final ParamKind<ResourceLocation> RESOURCE_ID = new ParamKind<>(
        name -> Commands.argument(name, ResourceLocationArgument.id()), ResourceLocationArgument::getId,
        false, ResourceLocation.class,
        v -> new JsonPrimitive(v.toString()), ParamKind::parseResourceLocation);

    /**
     * Any NBT value, written as SNBT.
     * Resolves to a {@link Tag}.
     *
     * <p>No built-in suggestions. A resolved tag is borrowed: copy it before modifying it.
     */
    public static final ParamKind<Tag> NBT = new ParamKind<>(
        name -> Commands.argument(name, NbtTagArgument.nbtTag()), NbtTagArgument::getNbtTag,
        false, Tag.class,
        v -> new JsonPrimitive(v.toString()), e -> RuleNbt.parse(e.getAsString()));

    /**
     * A compound-root NBT value, written as SNBT.
     * Resolves to a {@link CompoundTag}.
     *
     * <p>No built-in suggestions. A resolved tag is borrowed: copy it before modifying it.
     */
    public static final ParamKind<CompoundTag> NBT_COMPOUND = new ParamKind<>(
        name -> Commands.argument(name, CompoundTagArgument.compoundTag()), CompoundTagArgument::getCompoundTag,
        false, CompoundTag.class,
        v -> new JsonPrimitive(v.toString()), e -> RuleNbt.compound(e.getAsString()));

    /**
     * Item component constants retain their SNBT until evaluation supplies the world's registries.
     * Resolved variables and node outputs use native {@link DataComponentPatch} values.
     *
     * <p>No built-in suggestions.
     */
    public static final ParamKind<Object> ITEM_COMPONENTS = new ParamKind<>(
        name -> Commands.argument(name, CompoundTagArgument.compoundTag()),
        (ctx, name) -> new ItemComponents.Constant(CompoundTagArgument.getCompoundTag(ctx, name).toString()),
        false, Object.class,
        v -> new JsonPrimitive(((ItemComponents.Constant) v).snbt()),
        e -> new ItemComponents.Constant(e.getAsString()));

    /**
     * A vanilla NBT path, kept as text so a variable can supply it.
     * Resolves to a {@link String}.
     *
     * <p>No built-in suggestions.
     */
    public static final ParamKind<String> NBT_PATH = new ParamKind<>(
        name -> Commands.argument(name, NbtPathArgument.nbtPath()),
        (ctx, name) -> NbtPathArgument.getPath(ctx, name).toString(),
        false, String.class,
        JsonPrimitive::new, e -> RuleNbt.pathText(e.getAsString()));

    /**
     * A boolean value ({@code true} or {@code false}).
     * Resolves to a {@link Boolean}.
     *
     * <p>Has built-in {@code true/false} suggestions.
     */
    public static final ParamKind<Boolean> BOOLEAN = new ParamKind<>(
        name -> Commands.argument(name, BoolArgumentType.bool()), BoolArgumentType::getBool,
        true, Boolean.class,
        JsonPrimitive::new, JsonElement::getAsBoolean);

    /**
     * An unbounded integer value.
     * Resolves to an {@link Integer}.
     *
     * <p>No built-in suggestions. Use {@link #intAtLeast(int)} when a minimum value is required.
     */
    public static final ParamKind<Integer> INT = new ParamKind<>(
        name -> Commands.argument(name, IntegerArgumentType.integer()), IntegerArgumentType::getInteger,
        false, Integer.class,
        JsonPrimitive::new, JsonElement::getAsInt);

    /**
     * An integer value with a lower bound.
     * Resolves to an {@link Integer} greater than or equal to {@code min}.
     *
     * <p>No built-in suggestions.
     *
     * @param min the minimum allowed value (inclusive)
     * @return a new {@code ParamKind} with the given lower bound
     */
    public static ParamKind<Integer> intAtLeast(int min) {
        return new ParamKind<>(
            name -> Commands.argument(name, IntegerArgumentType.integer(min)), IntegerArgumentType::getInteger,
            false, Integer.class,
            JsonPrimitive::new, JsonElement::getAsInt);
    }

    /**
     * An unbounded double-precision floating-point value.
     * Resolves to a {@link Double}.
     *
     * <p>No built-in suggestions. Use {@link #doubleAtLeast(double)} when a minimum value is required.
     */
    public static final ParamKind<Double> DOUBLE = new ParamKind<>(
        name -> Commands.argument(name, DoubleArgumentType.doubleArg()), DoubleArgumentType::getDouble,
        false, Double.class,
        JsonPrimitive::new, JsonElement::getAsDouble);

    /**
     * A double-precision floating-point value with a lower bound.
     * Resolves to a {@link Double} greater than or equal to {@code min}.
     *
     * <p>No built-in suggestions.
     *
     * @param min the minimum allowed value (inclusive)
     * @return a new {@code ParamKind} with the given lower bound
     */
    public static ParamKind<Double> doubleAtLeast(double min) {
        return new ParamKind<>(
            name -> Commands.argument(name, DoubleArgumentType.doubleArg(min)), DoubleArgumentType::getDouble,
            false, Double.class,
            JsonPrimitive::new, JsonElement::getAsDouble);
    }

    /**
     * A string value accepting one word or a quoted string containing whitespace.
     * Resolves to a {@link String}.
     *
     * <p>No built-in suggestions. Use {@link #GREEDY_STRING} to consume the rest of the input.
     */
    public static final ParamKind<String> STRING = new ParamKind<>(
        name -> Commands.argument(name, StringArgumentType.string()), StringArgumentType::getString,
        false, String.class,
        JsonPrimitive::new, JsonElement::getAsString);

    /**
     * A free-form string value consuming the rest of the input (may contain whitespace).
     * Resolves to a {@link String}.
     *
     * <p>No built-in suggestions. Can be combined with a custom suggester (e.g. placeholder
     * completion) via {@link com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder#add(String, String, String, String,
     * ParamKind, com.mojang.brigadier.suggestion.SuggestionProvider)}.
     */
    public static final ParamKind<String> GREEDY_STRING = new ParamKind<>(
        name -> Commands.argument(name, StringArgumentType.greedyString()), StringArgumentType::getString,
        false, String.class,
        JsonPrimitive::new, JsonElement::getAsString);

    // private static final DynamicCommandExceptionType INVALID_VARIABLE_NAME = new DynamicCommandExceptionType(
    //     value -> Util.parseTranslatableText("fmod.rule.param.variablename.invalid", value));

    /**
     * A single-word string used as a variable write target.
     * Resolves to a {@link String}. The extractor does not validate
     * {@link RuleCondition#NAME_PATTERN}; components must validate a resolved target name
     * before writing it, including names obtained through a variable-bound parameter.
     *
     * <p>No built-in suggestions.
     */
    public static final ParamKind<String> VARIABLE_NAME = new ParamKind<>(
        name -> Commands.argument(name, StringArgumentType.word()),
        (ctx, name) -> {
            String value = StringArgumentType.getString(ctx, name);
            // if (!RuleCondition.NAME_PATTERN.matcher(value).matches()) {
            //     throw INVALID_VARIABLE_NAME.create(value);
            // }
            return value;
        },
        false, String.class,
        JsonPrimitive::new, JsonElement::getAsString);

    /**
     * A string value that is parsed and auto-cast through {@link TypeAdaptor#parse(Object)} and {@link TypeAdaptor#autoCast()}
     * into the most appropriate Java type ({@link Double}, {@link Boolean},
     * {@link String}, etc.).
     *
     * <p>Resolves to an {@link Object} - callers should use the actual runtime type
     * returned by {@link RuleParameter#resolve(RuleContext, Class)}. No built-in suggestions.
     *
     * <p>Persistence keeps the JSON scalar type (see {@link #autoToJson(Object)}), so a number
     * stays a number across a save and reload instead of coming back as text.
     */
    public static final ParamKind<Object> AUTO = new ParamKind<>(
        name -> Commands.argument(name, StringArgumentType.string()),
        (ctx, name) -> TypeAdaptor.parse(StringArgumentType.getString(ctx, name)).autoCast(),
        false, Object.class,
        ParamKind::autoToJson, ParamKind::autoFromJson);

    /**
     * Serializes an auto value, keeping its JSON scalar type.
     *
     * <p>Writing every value through {@link TypeAdaptor#asString()} turns {@code 3.5} into the JSON
     * string {@code "3.5"}, and reading that back through {@link JsonElement#toString()} keeps the
     * quotes Gson added, so the round trip degrades a number into text. Numbers and booleans are
     * therefore written as their own JSON types; everything else (vectors, lists, text) keeps the
     * text form it has always used.
     *
     * @param value the value to serialize, possibly null
     * @return the JSON element to store
     */
    private static JsonElement autoToJson(Object value) {
        if (value instanceof Number number) {
            return new JsonPrimitive(number);
        }
        if (value instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        }
        return new JsonPrimitive(TypeAdaptor.parse(value).asString());
    }

    /**
     * Reads an auto value written by {@link #autoToJson(Object)}.
     *
     * <p>Primitives are read through {@link JsonPrimitive#getAsString()} so a quoted JSON string
     * loses its quotes before {@link TypeAdaptor#autoCast()} inspects it.
     *
     * @param element the stored JSON element, possibly null
     * @return the value, or {@code null} for a JSON null
     */
    private static Object autoFromJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return primitive.getAsDouble();
            }
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            return TypeAdaptor.parse(primitive.getAsString()).autoCast();
        }
        return TypeAdaptor.parse(element.getAsString()).autoCast();
    }
}
