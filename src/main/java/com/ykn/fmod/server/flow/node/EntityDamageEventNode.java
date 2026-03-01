/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import com.ykn.fmod.server.flow.logic.EventNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;

/**
 * A flow node that triggers when an entity takes damage.
 * Inputs: None
 * Outputs:
 * 1. LivingEntity - The entity that took damage.
 * 2. double - The amount of damage taken.
 * 3. DamageType - The type of damage.
 * 4. Entity - The entity that caused the damage. (e.g. arrow)
 * 5. Entity - Source of damage (e.g. shooter of arrow)
 * 6. Vec3d - Position of the damage source.
 * Branches: 1 (Next node)
 */
public class EntityDamageEventNode extends EventNode {

    public EntityDamageEventNode(long id, String name) {
        super(id, name, 0, 6, 1, "EntityDamageEventNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.damageevt.title.name", "fmod.node.damageevt.title.feat")
            .output("fmod.node.damageevt.output.victim.name", "fmod.node.damageevt.output.victim.feat", "fmod.node.damageevt.output.victim.type")
            .output("fmod.node.damageevt.output.amount.name", "fmod.node.damageevt.output.amount.feat", "fmod.node.damageevt.output.amount.type")
            .output("fmod.node.damageevt.output.damage.name", "fmod.node.damageevt.output.damage.feat", "fmod.node.damageevt.output.damage.type")
            .output("fmod.node.damageevt.output.attacker.name", "fmod.node.damageevt.output.attacker.feat", "fmod.node.damageevt.output.attacker.type")
            .output("fmod.node.damageevt.output.source.name", "fmod.node.damageevt.output.source.feat", "fmod.node.damageevt.output.source.type")
            .output("fmod.node.damageevt.output.position.name", "fmod.node.damageevt.output.position.feat", "fmod.node.damageevt.output.position.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

}
