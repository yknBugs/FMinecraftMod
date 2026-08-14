/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.core.RuleParameter;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Generic machinery driven purely by a {@link RequiredParamMetadata}, shared by every
 * {@link com.ykn.fmod.server.rule.core.SourceCondition}/{@link com.ykn.fmod.server.rule.core.RuleAction}
 * whose parameters fully describe its state - which is every built-in type except one with a
 * privileged, non-parameter field (see {@link com.ykn.fmod.server.rule.action.ExecuteCommandAction}).
 *
 * <p>Used internally by {@link RuleRegistry#registerCondition} / {@link RuleRegistry#registerAction}
 * for the common case, and directly by classes that need to compose only part of this machinery
 * around a privileged field.
 *
 * @see RuleRegistry
 */
public final class RuleComponentFactory {

    /**
     * Reads a component's declared parameter values from its JSON {@code "value"} object, in
     * {@code metadata} order, via each entry's {@link ParamKind#fromJson(com.google.gson.JsonElement)}.
     *
     * @param json     the full component JSON object (containing a {@code "value"} member)
     * @param metadata the component type's declared parameters
     * @return the parameter values, in {@code metadata} order
     */
    public static List<RuleParameter<?>> readValues(JsonObject json, RequiredParamMetadata metadata) {
        List<RuleParameter<?>> values = new ArrayList<>(metadata.size());
        for (RequiredParamMetadata.Entry entry : metadata.getArgumentList()) {
            values.add(RuleParameter.fromJson(json, entry.constArgName, entry.kind::fromJson));
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private static <T> void addValueJson(JsonObject json, RequiredParamMetadata.Entry entry, RuleParameter<?> value) {
        ParamKind<T> kind = (ParamKind<T>) entry.kind;
        json.add(entry.constArgName, RuleParameter.toJson((RuleParameter<T>) value, kind::toJson));
    }

    /**
     * Serializes a component's parameter values to a JSON {@code "value"} object, in
     * {@code metadata} order, via each entry's {@link ParamKind#toJson}.
     *
     * @param metadata the component type's declared parameters
     * @param values   the parameter values, in {@code metadata} order
     * @return the serialized {@code "value"} object
     */
    public static JsonObject toValueJson(RequiredParamMetadata metadata, List<RuleParameter<?>> values) {
        JsonObject json = new JsonObject();
        List<RequiredParamMetadata.Entry> entries = metadata.getArgumentList();
        for (int i = 0; i < entries.size(); i++) {
            addValueJson(json, entries.get(i), values.get(i));
        }
        return json;
    }

    /**
     * Builds the standard {@code translate(renderKey, name, type, param1.render(), param2.render(), ...)}
     * component every built-in condition/action's {@code render()} used to hand-write.
     *
     * @param metadata the component type's declared parameters (supplies the render i18n key)
     * @param name     the component instance's name
     * @param type     the component's registered type string
     * @param values   the parameter values, in {@code metadata} order
     * @return the rendered component
     */
    public static Component render(RequiredParamMetadata metadata, String name, String type, List<RuleParameter<?>> values) {
        Object[] args = new Object[2 + values.size()];
        args[0] = name;
        args[1] = type;
        for (int i = 0; i < values.size(); i++) {
            args[2 + i] = values.get(i).render();
        }
        return Util.parseTranslatableText(metadata.getRenderI18nKey(), args);
    }

    /**
     * Resolves every declared parameter from a parsed command, in {@code metadata} order, via
     * {@link RecursiveCommandBuilder#resolveParameter}.
     *
     * @param builder   the builder the parameters were added to (via {@link RecursiveCommandBuilder#addAll})
     * @param metadata  the component type's declared parameters
     * @param arguments the set of argument names present in the parsed command
     * @param ctx       the Brigadier command context
     * @return the resolved parameter values, in {@code metadata} order
     * @throws CommandSyntaxException if resolving any parameter throws
     */
    public static List<RuleParameter<?>> resolveValues(RecursiveCommandBuilder builder, RequiredParamMetadata metadata,
            Set<String> arguments, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<RuleParameter<?>> values = new ArrayList<>(metadata.size());
        for (int i = 0; i < metadata.size(); i++) {
            values.add(builder.resolveParameter(i, arguments, ctx));
        }
        return values;
    }

    /**
     * Builds the standard {@code /f rule edit ... <type> <name> ...} command sub-tree every
     * built-in condition/action's {@code buildCommand(...)} used to hand-write: a
     * {@link RecursiveCommandBuilder} wired with every entry in {@code metadata}, whose execution
     * resolves all parameter values and hands them to {@code factory} along with the parsed name.
     *
     * @param type     the component type's registered identifier, used for usage-help output
     * @param metadata the component type's declared parameters
     * @param node     the parent literal node (the bare type name) to attach the tree to
     * @param factory  builds the final component from its parsed name and resolved parameter values
     * @param consumer invoked with the finished component when the command executes; contravariant
     *                 in {@code T} so a {@code BiConsumer<..., RuleCondition>} can be passed even
     *                 when {@code factory} produces the narrower {@code SourceCondition}
     * @param <T>      the component type ({@link com.ykn.fmod.server.rule.core.RuleCondition} or
     *                 {@link com.ykn.fmod.server.rule.core.RuleAction})
     * @return the modified {@code node}
     */
    public static <T> LiteralArgumentBuilder<CommandSourceStack> buildCommand(String type, RequiredParamMetadata metadata,
            LiteralArgumentBuilder<CommandSourceStack> node, BiFunction<String, List<RuleParameter<?>>, T> factory,
            BiConsumer<CommandContext<CommandSourceStack>, ? super T> consumer) {
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                List<RuleParameter<?>> values = resolveValues(builder, metadata, arguments, ctx);
                T component = factory.apply(name, values);
                consumer.accept(ctx, component);
            })
            .addAll(metadata);
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return node.executes(builder.usageExecutor(type, metadata.getSummaryI18nKey())).then(commandTree);
    }
}
