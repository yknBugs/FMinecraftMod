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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

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
 * RequiredArgumentBuilder<ServerCommandSource, ?> node =
 *     RecursiveCommandBuilder
 *         .builder((arguments, ctx) -> {
 *             RuleParameter<UUID> entityId = RuleParameter.fromCommandContext(
 *                 "entity", "var.entity", () -> EntityArgumentType.getEntity(ctx, "entity").getUuid(),
 *                 arguments, ctx);
 *             // ... build the component and call the consumer
 *             return Command.SINGLE_SUCCESS;
 *         })
 *         .add("entity",   "var.entity",   () -> CommandManager.argument("entity",   EntityArgumentType.entity()))
 *         .add("dimension", "var.dimension", () -> CommandManager.argument("dimension", DimensionArgumentType.dimension()))
 *         .build(CommandManager.argument("name", StringArgumentType.string()));
 * }</pre>
 *
 * <p>The command tree depth still grows exponentially with the number of parameters, 
 * but the amount of code required grows linearly with the number of added parameters.
 *
 * @see com.ykn.fmod.server.rule.core.RuleParameter#fromCommandContext
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

    private final BiFunction<Set<String>, CommandContext<ServerCommandSource>, Integer> commandExecutor;

    private RecursiveCommandBuilder(BiFunction<Set<String>, CommandContext<ServerCommandSource>, Integer> commandExecutor) {
        this.arguments = new ArrayList<>();
        this.commandExecutor = commandExecutor;
    }

    private RequiredArgumentBuilder<ServerCommandSource, ?> addTailAfterArgument(RequiredArgumentBuilder<ServerCommandSource, ?> parentArgument, int index, Set<String> branchHasArgument) {
        if (index >= arguments.size()) {
            return parentArgument.executes(ctx -> {return commandExecutor.apply(branchHasArgument, ctx);});
        }
        return parentArgument.then(addTailAfterConstLiteral(CommandManager.literal("const"), index, branchHasArgument))
            .then(addTailAfterVariableLiteral(CommandManager.literal("var"), index, branchHasArgument))
            .then(addTailAfterMixLiteral(CommandManager.literal("mix"), index, branchHasArgument));
    }

    private LiteralArgumentBuilder<ServerCommandSource> addTailAfterConstLiteral(LiteralArgumentBuilder<ServerCommandSource> constLiteral, int index, Set<String> branchHasArgument) {
        Set<String> newBranchHasArgument = new HashSet<>(branchHasArgument);
        newBranchHasArgument.add(arguments.get(index).getConstArgumentName());
        return constLiteral.then(addTailAfterArgument(arguments.get(index).getConstArgumentNode(), index + 1, newBranchHasArgument));
    }

    private LiteralArgumentBuilder<ServerCommandSource> addTailAfterVariableLiteral(LiteralArgumentBuilder<ServerCommandSource> variableLiteral, int index, Set<String> branchHasArgument) {
        Set<String> newBranchHasArgument = new HashSet<>(branchHasArgument);
        newBranchHasArgument.add(arguments.get(index).getVariableArgumentName());
        return variableLiteral.then(addTailAfterArgument(CommandManager.argument(arguments.get(index).getVariableArgumentName(), StringArgumentType.string()), index + 1, newBranchHasArgument));
    }

    private LiteralArgumentBuilder<ServerCommandSource> addTailAfterMixLiteral(LiteralArgumentBuilder<ServerCommandSource> mixLiteral, int index, Set<String> branchHasArgument) {
        Set<String> newBranchHasArgument = new HashSet<>(branchHasArgument);
        newBranchHasArgument.add(arguments.get(index).getVariableArgumentName());
        newBranchHasArgument.add(arguments.get(index).getConstArgumentName());
        return mixLiteral.then(CommandManager.argument(arguments.get(index).getVariableArgumentName(), StringArgumentType.string())
                .then(addTailAfterArgument(arguments.get(index).getConstArgumentNode(), index + 1, newBranchHasArgument))
            );
    }

    /**
     * Creates a new builder with the given command executor.
     *
     * @param commandExecutor a function called when the Brigadier command is executed;
     *                        receives the set of argument names present in the parsed command
     *                        and the command context
     * @return a new {@code RecursiveCommandBuilder}
     */
    public static RecursiveCommandBuilder builder(BiFunction<Set<String>, CommandContext<ServerCommandSource>, Integer> commandExecutor) {
        return new RecursiveCommandBuilder(commandExecutor);
    }

    /**
     * Registers a new parameter that may appear in the command in {@code const}, {@code var}, or
     * {@code mix} mode.
     *
     * @param constArgumentName          argument name used to retrieve the constant value from the context
     * @param variableArgumentName       argument name used to retrieve the variable name from the context
     * @param constArgumentNodeSupplier  supplier that produces the Brigadier argument node for the constant value;
     *                                   called once per unique command path that includes the constant
     * @return {@code this}, for method chaining
     */
    public RecursiveCommandBuilder add(String constArgumentName, String variableArgumentName, Supplier<RequiredArgumentBuilder<ServerCommandSource, ?>> constArgumentNodeSupplier) {
        this.arguments.add(new RuleComponentArgument(constArgumentName, variableArgumentName, constArgumentNodeSupplier));
        return this;
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
    public LiteralArgumentBuilder<ServerCommandSource> build(LiteralArgumentBuilder<ServerCommandSource> commandNode) {
        if (arguments.isEmpty()) {
            return commandNode.executes(ctx -> {return commandExecutor.apply(new HashSet<>(), ctx);});
        }
        return commandNode.then(addTailAfterConstLiteral(CommandManager.literal("const"), 0, new HashSet<>()))
            .then(addTailAfterVariableLiteral(CommandManager.literal("var"), 0, new HashSet<>()))
            .then(addTailAfterMixLiteral(CommandManager.literal("mix"), 0, new HashSet<>()));
    }

    /**
     * Overload of {@link #build(LiteralArgumentBuilder)} for a {@link RequiredArgumentBuilder} root.
     *
     * @param commandNode the parent required-argument node
     * @return the modified {@code commandNode}
     */
    public RequiredArgumentBuilder<ServerCommandSource, ?> build(RequiredArgumentBuilder<ServerCommandSource, ?> commandNode) {
        return addTailAfterArgument(commandNode, 0, new HashSet<>());
    }

    /**
     * Holds the metadata for a single parameter registered with
     * {@link RecursiveCommandBuilder#add(String, String, Supplier)}.
     */
    public static class RuleComponentArgument {

        private final String constArgumentName;

        private final String variableArgumentName;

        private final Supplier<RequiredArgumentBuilder<ServerCommandSource, ?>> constArgumentNodeSupplier;

        public RuleComponentArgument(String constArgumentName, String variableArgumentName, Supplier<RequiredArgumentBuilder<ServerCommandSource, ?>> constArgumentNodeSupplier) {
            this.constArgumentName = constArgumentName;
            this.variableArgumentName = variableArgumentName;
            this.constArgumentNodeSupplier = constArgumentNodeSupplier;
        }

        public String getConstArgumentName() {
            return constArgumentName;
        }

        public String getVariableArgumentName() {
            return variableArgumentName;
        }

        public RequiredArgumentBuilder<ServerCommandSource, ?> getConstArgumentNode() {
            return constArgumentNodeSupplier.get();
        }
    }
}
