/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;
import com.ykn.fmod.server.flow.tool.FlowConvert;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;

/**
 * A flow node that applies a status effect to a living entity.
 * An application the target rejects, or one that changes nothing, fails the node.
 * Inputs:
 * 1. Entity/UUID/String - Required living entity or UUID resolved on this server.
 * 2. ResourceLocation/String - Required registered effect identifier; native values use identifier adaptation.
 * 3. Number - Required positive integer duration in ticks (20 ticks per second).
 * 4. Number - Required integer from 0 to 255; 0 means level I.
 * 5. Boolean - True hides particles and the HUD icon, as with effect give. Null defaults to false.
 * Outputs: None
 * Branches: 1 (Next node)
 */
public class ApplyEffectNode extends FlowNode {
    
    public ApplyEffectNode(long id, String name) {
        super(id, name, 5, 0, 1, "ApplyEffectNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        NodeMetadata.Builder builder = NodeMetadata.builder("fmod.node.applyeffect.title.name", "fmod.node.applyeffect.title.feat");
        for (String port : List.of("entity", "id", "duration", "amplifier", "hideparticles")) {
            String key = "fmod.node.applyeffect.input." + port;
            builder.input(key + ".name", key + ".feat", key + ".type");
        }
        return builder.branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat").build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        Entity entity = FlowConvert.entity(inputs.get(0), context.getServer());
        if (!(entity instanceof LivingEntity living) || entity.isRemoved() || entity.level().getServer() != context.getServer()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.applyeffect.error.target"), null);
        }
        if (!living.addEffect(effect(inputs.get(1), inputs.get(2), inputs.get(3), inputs.get(4)))) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.applyeffect.error.rejected"), null);
        }
    }

    /**
     * Builds a fresh effect instance, validating all parameters before changing the target.
     */
    static MobEffectInstance effect(Object input, Object duration, Object amplifier, Object hideParticles) throws LogicException {
        ResourceLocation id = TypeAdaptor.parse(input).asResourceLocation();
        if (id == null || !BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.identifier", input), null);
        }
        int ticks = integer(duration, 1, Integer.MAX_VALUE, "Duration");
        int strength = integer(amplifier, 0, 255, "Amplifier");
        Boolean hide = hideParticles == null ? Boolean.FALSE : TypeAdaptor.parse(hideParticles).asBoolean();
        if (hide == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.boolean", hideParticles), null);
        }
        return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.getHolder(id).orElseThrow(), ticks, strength, false, !hide, !hide);
    }

    private static int integer(Object input, int min, int max, String name) throws LogicException {
        Double value = TypeAdaptor.parse(input).asDouble();
        if (value == null || !Double.isFinite(value) || value < min || value > max || value != Math.rint(value)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.applyeffect.error.integer", name, min, max), null);
        }
        return value.intValue();
    }
}
