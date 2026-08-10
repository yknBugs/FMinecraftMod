/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.rule.tool.ThrowingBiFunction;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * A closed taxonomy of parameter "shapes" usable in a {@link RecursiveCommandBuilder} {@code const}
 * node, pairing the Brigadier argument node to build with the extractor that reads the resolved
 * value back out of a {@link CommandContext}.
 *
 * <p>{@link #valueType()} additionally lets {@link RecursiveCommandBuilder} filter {@code var}/
 * {@code mix} variable-name suggestions down to variables whose declared type
 * ({@link com.ykn.fmod.server.rule.core.RuleEvent#variablesType()}) actually matches this kind.
 *
 * @param <T> the Java type this kind resolves to
 * @see RecursiveCommandBuilder
 */
public final class ParamKind<T> {

    private final Function<String, RequiredArgumentBuilder<CommandSourceStack, ?>> nodeFactory;

    private final ThrowingBiFunction<CommandContext<CommandSourceStack>, String, T, CommandSyntaxException> extractor;

    private final boolean hasBuiltinSuggestions;

    private final Class<T> valueType;

    private ParamKind(Function<String, RequiredArgumentBuilder<CommandSourceStack, ?>> nodeFactory,
            ThrowingBiFunction<CommandContext<CommandSourceStack>, String, T, CommandSyntaxException> extractor,
            boolean hasBuiltinSuggestions, Class<T> valueType) {
        this.nodeFactory = nodeFactory;
        this.extractor = extractor;
        this.hasBuiltinSuggestions = hasBuiltinSuggestions;
        this.valueType = valueType;
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
    public Class<T> valueType() {
        return valueType;
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
        true, UUID.class);

    /**
     * A single online player selector.
     * Resolves to the player's {@link UUID}.
     *
     * <p>Has built-in player-name suggestions.
     */
    public static final ParamKind<UUID> PLAYER = new ParamKind<>(
        name -> Commands.argument(name, EntityArgument.player()),
        (ctx, name) -> EntityArgument.getPlayer(ctx, name).getUUID(),
        true, UUID.class);

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
        true, (Class<List<UUID>>) (Class<?>) List.class);

    /**
     * A 3D coordinate (e.g. {@code ~ ~ ~}, {@code 0 64 0}).
     * Resolves to a {@link Vec3}.
     *
     * <p>Has built-in coordinate suggestions.
     */
    public static final ParamKind<Vec3> POSITION = new ParamKind<>(
        name -> Commands.argument(name, Vec3Argument.vec3()),
        (ctx, name) -> Vec3Argument.getVec3(ctx, name),
        true, Vec3.class);

    /**
     * A dimension identifier (e.g. {@code minecraft:overworld}).
     * Resolves to the dimension's {@link ResourceLocation}.
     *
     * <p>Has built-in dimension-name suggestions.
     */
    public static final ParamKind<ResourceLocation> DIMENSION = new ParamKind<>(
        name -> Commands.argument(name, DimensionArgument.dimension()),
        (ctx, name) -> DimensionArgument.getDimension(ctx, name).dimension().location(),
        true, ResourceLocation.class);

    /**
     * A block identifier (e.g. {@code minecraft:stone}).
     * Resolves to a {@link ResourceLocation}.
     *
     * <p>No built-in suggestions.
     */
    public static final ParamKind<ResourceLocation> BLOCK_ID = new ParamKind<>(
        name -> Commands.argument(name, ResourceLocationArgument.id()),
        (ctx, name) -> ResourceLocationArgument.getId(ctx, name),
        false, ResourceLocation.class);

    /**
     * An entity type identifier (e.g. {@code minecraft:creeper}).
     * Resolves to a {@link ResourceLocation}.
     *
     * <p>Has built-in suggestions from vanilla's summonable-entity list.
     */
    public static final ParamKind<ResourceLocation> ENTITY_TYPE_ID = new ParamKind<>(
        name -> Commands.argument(name, ResourceLocationArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES),
        (ctx, name) -> ResourceLocationArgument.getId(ctx, name),
        true, ResourceLocation.class);

    /**
     * A boolean value ({@code true} or {@code false}).
     * Resolves to a {@link Boolean}.
     *
     * <p>Has built-in {@code true/false} suggestions.
     */
    public static final ParamKind<Boolean> BOOLEAN = new ParamKind<>(
        name -> Commands.argument(name, BoolArgumentType.bool()),
        (ctx, name) -> BoolArgumentType.getBool(ctx, name),
        true, Boolean.class);

    /**
     * An unbounded integer value.
     * Resolves to an {@link Integer}.
     *
     * <p>No built-in suggestions. Use {@link #intAtLeast(int)} when a minimum value is required.
     */
    public static final ParamKind<Integer> INT = new ParamKind<>(
        name -> Commands.argument(name, IntegerArgumentType.integer()),
        (ctx, name) -> IntegerArgumentType.getInteger(ctx, name),
        false, Integer.class);

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
            name -> Commands.argument(name, IntegerArgumentType.integer(min)),
            (ctx, name) -> IntegerArgumentType.getInteger(ctx, name),
            false, Integer.class);
    }

    /**
     * An unbounded double-precision floating-point value.
     * Resolves to a {@link Double}.
     *
     * <p>No built-in suggestions. Use {@link #doubleAtLeast(double)} when a minimum value is required.
     */
    public static final ParamKind<Double> DOUBLE = new ParamKind<>(
        name -> Commands.argument(name, DoubleArgumentType.doubleArg()),
        (ctx, name) -> DoubleArgumentType.getDouble(ctx, name),
        false, Double.class);

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
            name -> Commands.argument(name, DoubleArgumentType.doubleArg(min)),
            (ctx, name) -> DoubleArgumentType.getDouble(ctx, name),
            false, Double.class);
    }

    /**
     * A single-word string value (no whitespace).
     * Resolves to a {@link String}.
     *
     * <p>No built-in suggestions. Use {@link #GREEDY_STRING} when the value may contain spaces.
     */
    public static final ParamKind<String> STRING = new ParamKind<>(
        name -> Commands.argument(name, StringArgumentType.string()),
        (ctx, name) -> StringArgumentType.getString(ctx, name),
        false, String.class);

    /**
     * A free-form string value consuming the rest of the input (may contain whitespace).
     * Resolves to a {@link String}.
     *
     * <p>No built-in suggestions. Can be combined with a custom suggester (e.g. placeholder
     * completion) via {@link RecursiveCommandBuilder#add(String, String, String, String,
     * ParamKind, com.mojang.brigadier.suggestion.SuggestionProvider)}.
     */
    public static final ParamKind<String> GREEDY_STRING = new ParamKind<>(
        name -> Commands.argument(name, StringArgumentType.greedyString()),
        (ctx, name) -> StringArgumentType.getString(ctx, name),
        false, String.class);

    /**
     * A string value that is parsed and auto-cast through {@link TypeAdaptor#parse(String)}
     * into the most appropriate Java type ({@link Integer}, {@link Double}, {@link Boolean},
     * {@link String}, etc.).
     *
     * <p>Resolves to an {@link Object} - callers should use the actual runtime type
     * returned by {@link RuleParameter#getValue(Object...)}. No built-in suggestions.
     */
    public static final ParamKind<Object> AUTO = new ParamKind<>(
        name -> Commands.argument(name, StringArgumentType.string()),
        (ctx, name) -> TypeAdaptor.parse(StringArgumentType.getString(ctx, name)).autoCast(),
        false, Object.class);
}
