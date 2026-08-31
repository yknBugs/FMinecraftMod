/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.List;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
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
 *   "dimension": {"constant": "minecraft:overworld"},
 *   "position":  {"variable": "pos"},
 *   "block":     {"constant": "minecraft:stone"},
 *   "default":   {"constant": false}
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

    public static final String TYPE = "CheckBlockType";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.blocktype.summary")
        .add(ParamKind.DIMENSION, "fmod.rule.condition.blocktype.param.dimension.name", "fmod.rule.condition.blocktype.param.dimension.desc", "dimension", "var.dimension")
        .add(ParamKind.POSITION, "fmod.rule.condition.blocktype.param.position.name", "fmod.rule.condition.blocktype.param.position.desc", "position", "var.position")
        .add(ParamKind.BLOCK_ID, "fmod.rule.condition.blocktype.param.block.name", "fmod.rule.condition.blocktype.param.block.desc", "block", "var.block")
        .add(ParamKind.BOOLEAN, "fmod.rule.condition.blocktype.param.default.name", "fmod.rule.condition.blocktype.param.default.desc", "default", "var.default");

    @SuppressWarnings("unchecked")
    public CheckBlockType(String name, List<RuleParameter<?>> values) {
        this.name = name;
        this.dimension = (RuleParameter<ResourceLocation>) values.get(0);
        this.position = (RuleParameter<Vec3>) values.get(1);
        this.block = (RuleParameter<ResourceLocation>) values.get(2);
        this.defaultValue = (RuleParameter<Boolean>) values.get(3);
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        ResourceLocation dimension = this.dimension.resolve(context, ResourceLocation.class);
        Vec3 position = this.position.resolve(context, Vec3.class);
        ResourceLocation block = this.block.resolve(context, ResourceLocation.class);
        Boolean defaultValue = this.defaultValue.resolve(context, Boolean.class);

        if (dimension == null) {
            warnNullInput(context, PARAM_METADATA.get(0));
            return defaultValue != null && defaultValue;
        }
        if (position == null) {
            warnNullInput(context, PARAM_METADATA.get(1));
            return defaultValue != null && defaultValue;
        }
        if (block == null) {
            warnNullInput(context, PARAM_METADATA.get(2));
            return defaultValue != null && defaultValue;
        }

        ServerLevel world = context.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (world == null) {
            return defaultValue != null && defaultValue;
        }

        BlockPos blockPos = BlockPos.containing(position);
        if (!world.isInWorldBounds(blockPos) || !world.hasChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()))) {
            addWarning(context, Util.parseTranslatableText("fmod.rule.condition.blocktype.unloaded", blockPos.toShortString(), dimension.toString()));
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
        return new CheckBlockType(name, getParameterValues());
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.dimension, this.position, this.block, this.defaultValue);
    }

}
