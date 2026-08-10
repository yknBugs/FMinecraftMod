/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * A {@link SourceCondition} that tests whether the block at a given world position
 * matches a specific block identifier.
 *
 * <p>When the target chunk is not loaded, or when the position is outside the build
 * limit, the condition evaluates to the value of the {@code defaultValue} parameter.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "dimension":    {"constant": "minecraft:overworld"},
 *   "position":     {"variable": "pos"},
 *   "block":        {"constant": "minecraft:stone"},
 *   "defaultValue": {"constant": false}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class CheckBlockType implements SourceCondition {

    private final String name;

    private final RuleParameter<ResourceLocation> dimension;

    private final RuleParameter<Vec3> position;

    private final RuleParameter<ResourceLocation> block;

    private final RuleParameter<Boolean> defaultValue;

    private static final String TYPE = "CheckBlockType";

    private static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.blocktype.summary")
        .add(ParamKind.DIMENSION, "fmod.rule.condition.blocktype.param.dimension.name", "fmod.rule.condition.blocktype.param.dimension.desc", "dimension", "var.dimension")
        .add(ParamKind.POSITION, "fmod.rule.condition.blocktype.param.position.name", "fmod.rule.condition.blocktype.param.position.desc", "position", "var.position")
        .add(ParamKind.BLOCK_ID, "fmod.rule.condition.blocktype.param.block.name", "fmod.rule.condition.blocktype.param.block.desc", "block", "var.block")
        .add(ParamKind.BOOLEAN, "fmod.rule.condition.blocktype.param.default.name", "fmod.rule.condition.blocktype.param.default.desc", "default", "var.default");

    public CheckBlockType(String name, RuleParameter<ResourceLocation> dimension, RuleParameter<Vec3> position, RuleParameter<ResourceLocation> block, RuleParameter<Boolean> defaultValue) {
        this.name = name;
        this.dimension = dimension;
        this.position = position;
        this.block = block;
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        ResourceLocation dimension = this.dimension.resolve(context, ResourceLocation.class);
        Vec3 position = this.position.resolve(context, Vec3.class);
        ResourceLocation block = this.block.resolve(context, ResourceLocation.class);
        Boolean defaultValue = this.defaultValue.resolve(context, Boolean.class);

        if (dimension == null || position == null || block == null) {
            return defaultValue != null && defaultValue;
        }

        ServerLevel world = context.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (world == null) {
            return defaultValue != null && defaultValue;
        }

        BlockPos blockPos = BlockPos.containing(position);
        if (!world.isInWorldBounds(blockPos) || !world.hasChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()))) {
            return defaultValue != null && defaultValue;
        }

        ResourceLocation actualBlockId = BuiltInRegistries.BLOCK.getKey(world.getBlockState(blockPos).getBlock());
        return block.equals(actualBlockId);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public RequiredParamMetadata getParameters() {
        return PARAM_METADATA;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new CheckBlockType(name, this.dimension, this.position, this.block, this.defaultValue);
    }

    @Override
    public Component render() {
        return Util.parseTranslatableText("fmod.rule.condition.blocktype", this.getName(), this.getType(),
            this.dimension.render(), this.position.render(), this.block.render(), this.defaultValue.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("dimension", RuleParameter.toJson(dimension, e -> new JsonPrimitive(e.toString())));
        json.add("position", RuleParameter.toJson(position, e -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", e.x);
            obj.addProperty("y", e.y);
            obj.addProperty("z", e.z);
            return obj;
        }));
        json.add("block", RuleParameter.toJson(block, e -> new JsonPrimitive(e.toString())));
        json.add("defaultValue", RuleParameter.toJson(defaultValue, JsonPrimitive::new));
        return json;
    }

    public static JsonObject toJson(CheckBlockType condition) {
        return condition.toJson();
    }

    public static CheckBlockType fromJson(JsonObject json) {
        RuleParameter<ResourceLocation> dimension = RuleParameter.fromJson(json, "dimension", e -> {
            String s = e.getAsString();
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl == null) {
                Util.LOGGER.warn("Invalid ResourceLocation in CheckBlockType dimension: {}", s);
                rl = new ResourceLocation("minecraft", "overworld");
            }
            return rl;
        });
        RuleParameter<Vec3> position = RuleParameter.fromJson(json, "position", e -> {
            JsonObject obj = e.getAsJsonObject();
            double x = obj.get("x").getAsDouble();
            double y = obj.get("y").getAsDouble();
            double z = obj.get("z").getAsDouble();
            return new Vec3(x, y, z);
        });
        RuleParameter<ResourceLocation> block = RuleParameter.fromJson(json, "block", e -> {
            String s = e.getAsString();
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl == null) {
                Util.LOGGER.warn("Invalid ResourceLocation in CheckBlockType block: " + s + ". Defaulting to minecraft:bedrock.");
                rl = new ResourceLocation("minecraft", "bedrock");
            }
            return rl;
        });
        RuleParameter<Boolean> defaultValue = RuleParameter.fromJson(json, "defaultValue", JsonElement::getAsBoolean);
        return new CheckBlockType(json.get("name").getAsString(), dimension, position, block, defaultValue);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> conditionConsumer) {
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                RuleParameter<ResourceLocation> dimensionParameter = builder.resolveParameter(0, arguments, ctx);
                RuleParameter<Vec3> positionParameter = builder.resolveParameter(1, arguments, ctx);
                RuleParameter<ResourceLocation> blockParameter = builder.resolveParameter(2, arguments, ctx);
                RuleParameter<Boolean> defaultValueParameter = builder.resolveParameter(3, arguments, ctx);
                CheckBlockType condition = new CheckBlockType(name, dimensionParameter, positionParameter, blockParameter, defaultValueParameter);
                conditionConsumer.accept(ctx, condition);
            })
            .addAll(PARAM_METADATA);
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.executes(builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey())).then(commandTree);
    }

}
