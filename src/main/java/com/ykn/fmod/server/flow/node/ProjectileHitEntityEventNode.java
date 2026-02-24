/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import com.ykn.fmod.server.flow.logic.EventNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;

/**
 * A flow node that triggers when a projectile hits an entity.
 * Inputs: None
 * Outputs:
 * 1. Entity - The projectile that hit the entity.
 * 2. Entity - The shooter of the projectile.
 * 3. Entity - The entity that was hit.
 * 4. Vec3d - The position of the hit.
 * 5. Double - The distance between the shooter and the hit entity.
 * Branches: 1 (Next node)
 */
public class ProjectileHitEntityEventNode extends EventNode {

    public ProjectileHitEntityEventNode(long id, String name) {
        super(id, name, 0, 5, 1);
        this.type = "ProjectileHitEntityEventNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.pentityevt.title.name", "fmod.node.pentityevt.title.feat")
            .output("fmod.node.pentityevt.output.projectile.name", "fmod.node.pentityevt.output.projectile.feat", "fmod.node.pentityevt.output.projectile.type")
            .output("fmod.node.pentityevt.output.shooter.name", "fmod.node.pentityevt.output.shooter.feat", "fmod.node.pentityevt.output.shooter.type")
            .output("fmod.node.pentityevt.output.entity.name", "fmod.node.pentityevt.output.entity.feat", "fmod.node.pentityevt.output.entity.type")
            .output("fmod.node.pentityevt.output.position.name", "fmod.node.pentityevt.output.position.feat", "fmod.node.pentityevt.output.position.type")
            .output("fmod.node.pentityevt.output.distance.name", "fmod.node.pentityevt.output.distance.feat", "fmod.node.pentityevt.output.distance.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
}
}
