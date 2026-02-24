/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import com.ykn.fmod.server.flow.logic.EventNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;

/**
 * A flow node that triggers when an entity dies.
 * Inputs: None
 * Outputs:
 * 1. LivingEntity - The entity that died.
 * 2. DamageType - The type of damage that caused the death.
 * 3. Entity - The entity that killed the living entity. (e.g. arrow)
 * 4. Entity - Source of damage (e.g. shooter of arrow)
 * 5. Vec3d - Position of the damage source.
 * 6. Text - The death message.
 * Branches: 1 (Next node)
 */
public class EntityDeathEventNode extends EventNode {

    public EntityDeathEventNode(long id, String name) {
        super(id, name, 0, 6, 1);
        this.type = "EntityDeathEventNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.deathevt.title.name", "fmod.node.deathevt.title.feat")
            .output("fmod.node.deathevt.output.victim.name", "fmod.node.deathevt.output.victim.feat", "fmod.node.deathevt.output.victim.type")
            .output("fmod.node.deathevt.output.damage.name", "fmod.node.deathevt.output.damage.feat", "fmod.node.deathevt.output.damage.type")
            .output("fmod.node.deathevt.output.attacker.name", "fmod.node.deathevt.output.attacker.feat", "fmod.node.deathevt.output.attacker.type")
            .output("fmod.node.deathevt.output.source.name", "fmod.node.deathevt.output.source.feat", "fmod.node.deathevt.output.source.type")
            .output("fmod.node.deathevt.output.position.name", "fmod.node.deathevt.output.position.feat", "fmod.node.deathevt.output.position.type")
            .output("fmod.node.deathevt.output.message.name", "fmod.node.deathevt.output.message.feat", "fmod.node.deathevt.output.message.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }
}
