/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ykn.fmod.server.base.command.RuleComponentSuggestion;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.core.RuleParameter;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;

/**
 * Recursively constructs nested Brigadier command trees for rule component parameters.
 *
 * <h3>Problem</h3>
 * <p>Each {@link com.ykn.fmod.server.rule.core.RuleParameter} in a condition or action can be
 * specified in three modes:
 * <ul>
 *   <li>{@code const}  – only a constant value is provided</li>
 *   <li>{@code var}    – only a variable (event-map) name is provided</li>
 *   <li>{@code mix}    – both a variable name and a constant fallback are provided</li>
 * </ul>
 * For {@code n} parameters, naively enumerating every combination produces O(3<sup>n</sup>) paths.
 * {@code RecursiveCommandBuilder} handles this combinatorial explosion by building the command
 * tree dynamically through recursion.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
 * builder.executes((arguments, ctx) -> {
 *         RuleParameter<UUID> entityId = builder.resolveParameter(0, arguments, ctx);
 *         RuleParameter<ResourceLocation> dimension = builder.resolveParameter(1, arguments, ctx);
 *         // ... build the component and call the consumer
 *     })
 *     .add("entity",   "var.entity",   "fmod.rule.condition.foo.param.entity.name",   "fmod.rule.condition.foo.param.entity.desc",   ParamKind.ENTITY)
 *     .add("dimension", "var.dimension", "fmod.rule.condition.foo.param.dimension.name", "fmod.rule.condition.foo.param.dimension.desc", ParamKind.DIMENSION);
 * RequiredArgumentBuilder<CommandSourceStack, ?> node = builder.build(Commands.argument("name", StringArgumentType.string()));
 * // attach builder.usageExecutor(...) on the outer type-literal so running the type with no
 * // further arguments prints parameter help instead of a bare Brigadier syntax error
 * }</pre>
 *
 * <p>Construction is two-phase ({@link #builder()} then {@link #executes(ThrowingBiConsumer)})
 * rather than a single factory call, specifically so the executor lambda can capture the
 * {@code builder} local variable itself and call {@link #resolveParameter(int, Set,
 * CommandContext)} on it - a lambda can't capture a variable that is still being initialized by
 * the very call it's an argument to. {@link #executes(ThrowingBiConsumer)} also wraps the action
 * with the standard try/catch/{@code Command.SINGLE_SUCCESS} handling every condition/action
 * previously duplicated by hand, so the lambda body only needs to contain its own logic.
 *
 * <p>The command tree depth still grows exponentially with the number of parameters,
 * but the amount of code required grows linearly with the number of added parameters.
 *
 * @see com.ykn.fmod.server.rule.core.RuleParameter#fromCommandContext
 * @see ParamKind
 * @see RuleRegistry
 */
public class RecursiveCommandBuilder {

    //////////////////////////////////////////////
    ///
    ///  For Action and Condition, their parameters command has similar structures:
    ///
    ///  <parent node>
    ///   - const
    ///      - (const value: any possible command argument type) <- Can be the next parent node if another parameter follows
    ///          - [future node]
    ///   - variable
    ///      - (variable name: string argument type) <- Can be the next parent node if another parameter follows
    ///          - [future node]
    ///   - mix
    ///      - (variable name: string argument type)
    ///          - (const value: any possible command argument type) <- Can be the next parent node if another parameter follows
    ///              - [future node]
    ///
    ///  Example:
    ///  f rule edit "rule" action "name" const @p variable "targetPosition" const 5 mix "feedbackMessage" "No target found"
    ///  f rule edit <ruleName> action <actionName> <entity param> <position param> <radius param> <message param>
    ///  Manually enumerating each possible path has an exponential growth of code
    ///  We need to use recursion to build the command nodes
    ///
    //////////////////////////////////////////////

    private final List<RuleComponentArgument> arguments;

    private BiFunction<Set<String>, CommandContext<CommandSourceStack>, Integer> commandExecutor;

    private RecursiveCommandBuilder() {
        this.arguments = new ArrayList<>();
    }

    private RequiredArgumentBuilder<CommandSourceStack, ?> addTailAfterArgument(RequiredArgumentBuilder<CommandSourceStack, ?> parentArgument, int index, Set<String> branchHasArgument) {
        if (index >= arguments.size()) {
            return parentArgument.executes(ctx -> {return commandExecutor.apply(branchHasArgument, ctx);});
        }
        return parentArgument.then(addTailAfterConstLiteral(Commands.literal("const"), index, branchHasArgument))
            .then(addTailAfterVariableLiteral(Commands.literal("var"), index, branchHasArgument))
            .then(addTailAfterMixLiteral(Commands.literal("mix"), index, branchHasArgument));
    }

    private LiteralArgumentBuilder<CommandSourceStack> addTailAfterConstLiteral(LiteralArgumentBuilder<CommandSourceStack> constLiteral, int index, Set<String> branchHasArgument) {
        Set<String> newBranchHasArgument = new HashSet<>(branchHasArgument);
        newBranchHasArgument.add(arguments.get(index).getConstArgumentName());
        return constLiteral.then(addTailAfterArgument(arguments.get(index).getConstArgumentNode(), index + 1, newBranchHasArgument));
    }

    private LiteralArgumentBuilder<CommandSourceStack> addTailAfterVariableLiteral(LiteralArgumentBuilder<CommandSourceStack> variableLiteral, int index, Set<String> branchHasArgument) {
        RuleComponentArgument argument = arguments.get(index);
        Class<?> expectedType = argument.getKind() == null ? null : argument.getKind().valueType();
        SuggestionProvider<CommandSourceStack> variableSuggestion = RuleComponentSuggestion.suggestVariable(false, 3, expectedType);
        Set<String> newBranchHasArgument = new HashSet<>(branchHasArgument);
        newBranchHasArgument.add(argument.getVariableArgumentName());
        return variableLiteral.then(addTailAfterArgument(Commands.argument(argument.getVariableArgumentName(), StringArgumentType.string()).suggests(variableSuggestion), index + 1, newBranchHasArgument));
    }

    private LiteralArgumentBuilder<CommandSourceStack> addTailAfterMixLiteral(LiteralArgumentBuilder<CommandSourceStack> mixLiteral, int index, Set<String> branchHasArgument) {
        RuleComponentArgument argument = arguments.get(index);
        Class<?> expectedType = argument.getKind() == null ? null : argument.getKind().valueType();
        SuggestionProvider<CommandSourceStack> variableSuggestion = RuleComponentSuggestion.suggestVariable(false, 3, expectedType);
        Set<String> newBranchHasArgument = new HashSet<>(branchHasArgument);
        newBranchHasArgument.add(argument.getVariableArgumentName());
        newBranchHasArgument.add(argument.getConstArgumentName());
        return mixLiteral.then(Commands.argument(argument.getVariableArgumentName(), StringArgumentType.string())
                .suggests(variableSuggestion)
                .then(addTailAfterArgument(argument.getConstArgumentNode(), index + 1, newBranchHasArgument))
            );
    }

    /**
     * Creates a new, empty builder. Call {@link #executes(BiFunction)} before (or after) adding
     * parameters via {@link #add} to register the command's execution logic.
     *
     * @return a new {@code RecursiveCommandBuilder}
     */
    public static RecursiveCommandBuilder builder() {
        return new RecursiveCommandBuilder();
    }

    /**
     * Registers the function called when the fully-built Brigadier command is executed, with full
     * manual control over the returned Brigadier result code. Prefer {@link
     * #executes(ThrowingBiConsumer)} for the common case; this overload remains as an escape hatch
     * for commands that need a return value other than {@link Command#SINGLE_SUCCESS} or {@code 0},
     * or custom error handling instead of the standard try/catch wrapping.
     *
     * @param commandExecutor a function called when the Brigadier command is executed;
     *                        receives the set of argument names present in the parsed command
     *                        and the command context
     * @return {@code this}, for method chaining
     */
    public RecursiveCommandBuilder executes(BiFunction<Set<String>, CommandContext<CommandSourceStack>, Integer> commandExecutor) {
        this.commandExecutor = commandExecutor;
        return this;
    }

    /**
     * Registers the action performed when the fully-built Brigadier command is executed, wrapped
     * with the standard error handling every condition/action {@code buildCommand} previously
     * duplicated by hand: a thrown {@link CommandSyntaxException} is reported to the command
     * source via its own message, any other exception is logged and reported as a generic error,
     * and both cases return {@code 0}; otherwise {@link Command#SINGLE_SUCCESS} is returned
     * automatically once {@code action} completes.
     *
     * @param action the command's logic; receives the set of argument names present in the parsed
     *               command and the command context, and may throw {@link CommandSyntaxException}
     *               (e.g. propagated from {@link #resolveParameter})
     * @return {@code this}, for method chaining
     */
    public RecursiveCommandBuilder executes(ThrowingBiConsumer<Set<String>, CommandContext<CommandSourceStack>, CommandSyntaxException> action) {
        this.commandExecutor = (arguments, ctx) -> {
            try {
                action.accept(arguments, ctx);
            } catch (CommandSyntaxException e) {
                ctx.getSource().sendFailure(ComponentUtils.fromMessage(e.getRawMessage()));
                return 0;
            } catch (Exception e) {
                Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit", e);
                ctx.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
                return 0;
            }
            return Command.SINGLE_SUCCESS;
        };
        return this;
    }

    /**
     * Registers a new parameter of the given {@link ParamKind}, which may appear in the command in
     * {@code const}, {@code var}, or {@code mix} mode. The kind's node factory builds the constant
     * value's Brigadier node.
     *
     * @param constArgumentName    argument name used to retrieve the constant value from the context
     * @param variableArgumentName argument name used to retrieve the variable name from the context
     * @param nameKey               i18n key for this parameter's short display name, used in usage output
     * @param descKey               i18n key for this parameter's description, used in usage output,
     *                              as the tooltip on its variable-name completions, and (when the
     *                              kind has no built-in suggestions) as the const-node hint tooltip
     * @param kind                  the parameter's declared shape
     * @param <T>                   the parameter's value type
     * @return {@code this}, for method chaining
     */
    public <T> RecursiveCommandBuilder add(String constArgumentName, String variableArgumentName, String nameKey, String descKey, ParamKind<T> kind) {
        Supplier<RequiredArgumentBuilder<CommandSourceStack, ?>> nodeSupplier = () -> {
            RequiredArgumentBuilder<CommandSourceStack, ?> node = kind.buildNode(constArgumentName);
            return node;
        };
        this.arguments.add(new RuleComponentArgument(constArgumentName, variableArgumentName, nameKey, descKey, nodeSupplier, kind));
        return this;
    }

    /**
     * Registers a new parameter of the given {@link ParamKind}, using a {@link RequiredParamMetadata.Entry}
     * to supply all argument names, i18n keys, and the kind itself.
     *
     * @param entry the parameter's metadata
     * @return {@code this}, for method chaining
     */
    public RecursiveCommandBuilder add(RequiredParamMetadata.Entry entry) {
        return this.add(entry.constArgName, entry.varArgName, entry.nameI18nKey, entry.descI18nKey, entry.kind);
    }

    /**
     * Registers a new parameter of the given {@link ParamKind}, overriding its default (or
     * hint-only) suggester with {@code customSuggester}. Used for parameters whose completions are
     * specific to that particular condition/action rather than to the kind in general - e.g. a
     * chat-message field suggesting {@code ${var:...}} placeholders, or a flow-name field
     * suggesting registered flow names.
     *
     * @param constArgumentName    argument name used to retrieve the constant value from the context
     * @param variableArgumentName argument name used to retrieve the variable name from the context
     * @param nameKey               i18n key for this parameter's short display name, used in usage output
     * @param descKey               i18n key for this parameter's description, used in usage output and
     *                              as the tooltip on its variable-name completions
     * @param kind                  the parameter's declared shape
     * @param customSuggester       suggester attached to the const node in place of the kind's default
     * @param <T>                   the parameter's value type
     * @return {@code this}, for method chaining
     */
    public <T> RecursiveCommandBuilder add(String constArgumentName, String variableArgumentName, String nameKey, String descKey, ParamKind<T> kind, SuggestionProvider<CommandSourceStack> customSuggester) {
        Supplier<RequiredArgumentBuilder<CommandSourceStack, ?>> nodeSupplier = () -> kind.buildNode(constArgumentName).suggests(customSuggester);
        this.arguments.add(new RuleComponentArgument(constArgumentName, variableArgumentName, nameKey, descKey, nodeSupplier, kind));
        return this;
    }

    /**
     * Registers a new parameter of the given {@link ParamKind}, using a {@link RequiredParamMetadata.Entry}
     * to supply all argument names, i18n keys, and the kind itself, and overriding its default (or
     * hint-only) suggester with {@code customSuggester}.
     *
     * @param entry the parameter's metadata
     * @param customSuggester       suggester attached to the const node in place of the kind's default
     * @return {@code this}, for method chaining
     */
    public RecursiveCommandBuilder add(RequiredParamMetadata.Entry entry, SuggestionProvider<CommandSourceStack> customSuggester) {
        return this.add(entry.constArgName, entry.varArgName, entry.nameI18nKey, entry.descI18nKey, entry.kind, customSuggester);
    }

    // /**
    //  * Registers a new parameter that may appear in the command in {@code const}, {@code var}, or
    //  * {@code mix} mode, using a fully custom Brigadier node supplier. Prefer {@link #add(String,
    //  * String, String, String, ParamKind)} for parameters that fit a declared {@link ParamKind};
    //  * this overload remains as an escape hatch for shapes the closed taxonomy doesn't cover.
    //  * Arguments added this way must be resolved manually (via {@link RuleParameter#fromCommandContext})
    //  * rather than through {@link #resolveParameter(int, Set, CommandContext)}.
    //  *
    //  * @param constArgumentName          argument name used to retrieve the constant value from the context
    //  * @param variableArgumentName       argument name used to retrieve the variable name from the context
    //  * @param nameKey                    i18n key for this parameter's short display name, used in usage output
    //  * @param descKey                    i18n key for this parameter's description, used in usage output and
    //  *                                    as the tooltip on its variable-name completions
    //  * @param constArgumentNodeSupplier  supplier that produces the Brigadier argument node for the constant value;
    //  *                                   called once per unique command path that includes the constant
    //  * @return {@code this}, for method chaining
    //  */
    // public RecursiveCommandBuilder add(String constArgumentName, String variableArgumentName, String nameKey, String descKey, Supplier<RequiredArgumentBuilder<CommandSourceStack, ?>> constArgumentNodeSupplier) {
    //     this.arguments.add(new RuleComponentArgument(constArgumentName, variableArgumentName, nameKey, descKey, constArgumentNodeSupplier, null));
    //     return this;
    // }

    /**
     * Registers a new parameter from a {@link RequiredParamMetadata} instance, which may contain
     * multiple entries. Each entry is added in order.
     *
     * @param params the metadata describing the parameters to add
     * @return {@code this}, for method chaining
     */
    public RecursiveCommandBuilder addAll(RequiredParamMetadata params) {
        for (RequiredParamMetadata.Entry entry : params.getArgumentList()) {
            this.add(entry);
        }
        return this;
    }

    /**
     * Resolves the {@link RuleParameter} for the parameter registered at {@code index} (in
     * {@link #add} call order), reading whichever of the constant/variable arguments the player
     * actually supplied. Only valid for parameters added via one of the {@link ParamKind}-based
     * {@code add} overloads.
     *
     * @param index     the 0-based position of the parameter, in {@code add} call order
     * @param arguments the set of argument names present in the parsed command (as passed to the
     *                  {@link #executes(BiFunction)} function)
     * @param ctx       the Brigadier command context
     * @param <T>       the parameter's value type; must match the type of the {@link ParamKind}
     *                  registered at {@code index}
     * @return the resolved {@code RuleParameter}
     * @throws CommandSyntaxException if extracting the constant value throws
     * @throws IllegalStateException  if the argument at {@code index} was added via the raw-{@link Supplier} overload
     */
    @SuppressWarnings("unchecked")
    public <T> RuleParameter<T> resolveParameter(int index, Set<String> arguments, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        RuleComponentArgument argument = this.arguments.get(index);
        ParamKind<T> kind = (ParamKind<T>) argument.getKind();
        if (kind == null) {
            throw new IllegalStateException("resolveParameter requires a ParamKind-based argument at index " + index);
        }
        return RuleParameter.fromCommandContext(argument.getConstArgumentName(), argument.getVariableArgumentName(),
            () -> kind.extract(ctx, argument.getConstArgumentName()), arguments, ctx);
    }

    /**
     * Builds a {@link Command} that prints usage help - the type's own name/summary followed by
     * every registered parameter's name and description - instead of executing the component.
     *
     * <p>Intended to be attached via {@code .executes(...)} directly on the bare type-literal
     * node (e.g. {@code EqualsTo}), so that running {@code /f rule edit <rule> cond add EqualsTo}
     * with no further arguments prints parameter help instead of Brigadier's generic "incomplete
     * command" error. Renders as a normal chat message, so it works identically for players and
     * on the server console.
     *
     * @param typeName       the component type's registered identifier (e.g. {@code "EqualsTo"});
     *                       not translated, since it doubles as the literal command/JSON type name
     * @param typeSummaryKey i18n key for a one-line description of what the type does
     * @return a {@link Command} that sends the usage text and returns {@link Command#SINGLE_SUCCESS}
     */
    public Command<CommandSourceStack> usageExecutor(String typeName, String typeSummaryKey) {
        return ctx -> {
            MutableComponent text = Component.literal(typeName).append(" - ").append(Util.parseTranslatableText(typeSummaryKey));
            for (RuleComponentArgument argument : arguments) {
                text = text.append("\n  ").append(Util.parseTranslatableText(argument.getNameKey())).append(": ").append(Util.parseTranslatableText(argument.getDescKey()));
            }
            MutableComponent finalText = text;
            ctx.getSource().sendSuccess(() -> finalText, false);
            return Command.SINGLE_SUCCESS;
        };
    }

    /**
     * Attaches the fully recursed sub-command tree to a {@link LiteralArgumentBuilder} root node
     * and returns it.
     *
     * <p>If no parameters were added, the root executes directly.
     * Otherwise the root gains three child literals ({@code const}, {@code var}, {@code mix})
     * for the first parameter, each of which recurses for the remaining parameters.
     *
     * @param commandNode the parent literal node to attach the tree to
     * @return the modified {@code commandNode}
     */
    public LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> commandNode) {
        if (arguments.isEmpty()) {
            return commandNode.executes(ctx -> {return commandExecutor.apply(new HashSet<>(), ctx);});
        }
        return commandNode.then(addTailAfterConstLiteral(Commands.literal("const"), 0, new HashSet<>()))
            .then(addTailAfterVariableLiteral(Commands.literal("var"), 0, new HashSet<>()))
            .then(addTailAfterMixLiteral(Commands.literal("mix"), 0, new HashSet<>()));
    }

    /**
     * Overload of {@link #build(LiteralArgumentBuilder)} for a {@link RequiredArgumentBuilder} root.
     *
     * @param commandNode the parent required-argument node
     * @return the modified {@code commandNode}
     */
    public RequiredArgumentBuilder<CommandSourceStack, ?> build(RequiredArgumentBuilder<CommandSourceStack, ?> commandNode) {
        return addTailAfterArgument(commandNode, 0, new HashSet<>());
    }

    /**
     * Holds the metadata for a single parameter registered with one of the
     * {@link RecursiveCommandBuilder#add} overloads.
     */
    public static class RuleComponentArgument {

        private final String constArgumentName;

        private final String variableArgumentName;

        private final String nameKey;

        private final String descKey;

        private final Supplier<RequiredArgumentBuilder<CommandSourceStack, ?>> constArgumentNodeSupplier;

        @Nullable
        private final ParamKind<?> kind;

        public RuleComponentArgument(String constArgumentName, String variableArgumentName, String nameKey, String descKey, Supplier<RequiredArgumentBuilder<CommandSourceStack, ?>> constArgumentNodeSupplier, @Nullable ParamKind<?> kind) {
            this.constArgumentName = constArgumentName;
            this.variableArgumentName = variableArgumentName;
            this.nameKey = nameKey;
            this.descKey = descKey;
            this.constArgumentNodeSupplier = constArgumentNodeSupplier;
            this.kind = kind;
        }

        public String getConstArgumentName() {
            return constArgumentName;
        }

        public String getVariableArgumentName() {
            return variableArgumentName;
        }

        public String getNameKey() {
            return nameKey;
        }

        public String getDescKey() {
            return descKey;
        }

        public RequiredArgumentBuilder<CommandSourceStack, ?> getConstArgumentNode() {
            return constArgumentNodeSupplier.get();
        }

        @Nullable
        public ParamKind<?> getKind() {
            return kind;
        }
    }
}
