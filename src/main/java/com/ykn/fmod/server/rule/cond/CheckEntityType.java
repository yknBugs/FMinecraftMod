/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.UUID;
import java.util.function.BiConsumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder;

import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * A {@link SourceCondition} that tests whether a specific entity (identified by UUID)
 * has a given entity type.
 *
 * <p>The condition searches across all loaded worlds for an entity with the provided UUID.
 * It evaluates to {@code false} when the entity is not found in any loaded world.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "entity":     {"variable": "targetId"},
 *   "type": {"constant": "minecraft:creeper"}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class CheckEntityType implements SourceCondition {

    private final String name;

    private final RuleParameter<UUID> entity;

    private final RuleParameter<ResourceLocation> entityType;

    public CheckEntityType(String name, RuleParameter<UUID> entity, RuleParameter<ResourceLocation> entityType) {
        this.name = name;
        this.entity = entity;
        this.entityType = entityType;
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        UUID entityId = this.entity.resolve(context, UUID.class);
        ResourceLocation entityType = this.entityType.resolve(context, ResourceLocation.class);

        if (entityId == null && entityType == null) {
            return true;
        }

        if (entityId == null || entityType == null) {
            return false;
        }

        for (ServerLevel world : context.getServer().getAllLevels()) {
            Entity found = world.getEntity(entityId);
            if (found != null) {
                return entityType.equals(EntityType.getKey(found.getType()));
            }
        }

        return false;
    }

    @Override
    public String getType() {
        return "CheckEntityType";
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new CheckEntityType(name, this.entity, this.entityType);
    }

    @Override
    public Component render() {
        return Util.parseTranslatableText("fmod.rule.condition.checkentitytype", this.getName(), this.getType(),
            this.entity.render(), this.entityType.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("entity", RuleParameter.toJson(entity, e -> new JsonPrimitive(e.toString())));
        json.add("type", RuleParameter.toJson(entityType, e -> new JsonPrimitive(e.toString())));
        return json;
    }

    public static JsonObject toJson(CheckEntityType condition) {
        return condition.toJson();
    }

    public static CheckEntityType fromJson(JsonObject json) {
        RuleParameter<UUID> entity = RuleParameter.fromJson(json, "entity", e -> UUID.fromString(e.getAsString()));
        RuleParameter<ResourceLocation> entityType = RuleParameter.fromJson(json, "type", e -> new ResourceLocation(e.getAsString()));
        return new CheckEntityType(json.get("name").getAsString(), entity, entityType);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> conditionConsumer) {
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = RecursiveCommandBuilder.builder((arguments, ctx) -> {
                try {
                    String name = StringArgumentType.getString(ctx, "name");
                    RuleParameter<UUID> entityParameter = RuleParameter.fromCommandContext("entity", "var.entity", () -> {
                        Entity entity = EntityArgument.getEntity(ctx, "entity");
                        return entity.getUUID();
                    }, arguments, ctx);
                    RuleParameter<ResourceLocation> entityTypeParameter = RuleParameter.fromCommandContext("type", "var.type", () -> {
                        return ResourceLocationArgument.getId(ctx, "type");
                    }, arguments, ctx);
                    CheckEntityType condition = new CheckEntityType(name, entityParameter, entityTypeParameter);
                    conditionConsumer.accept(ctx, condition);
                } catch (CommandRuntimeException e) {
                    throw e;
                } catch (CommandSyntaxException e) {
                    throw new CommandRuntimeException(ComponentUtils.fromMessage(e.getRawMessage()));
                } catch (Exception e) {
                    Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit", e);
                    throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
                }
                return Command.SINGLE_SUCCESS;
            })
            .add("entity", "var.entity", () -> Commands.argument("entity", EntityArgument.entity()))
            .add("type", "var.type", () -> Commands.argument("type", ResourceLocationArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES))
            .build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.then(commandTree);
    }

}
