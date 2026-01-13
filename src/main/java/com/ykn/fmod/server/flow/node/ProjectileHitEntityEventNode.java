package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.EventNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;

import net.minecraft.network.chat.Component;

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
        Component displayName = Util.parseTranslateableText("fmod.node.pentityevt.title.name");
        Component description = Util.parseTranslateableText("fmod.node.pentityevt.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        List<Component> outputNames = new ArrayList<>();
        List<Component> outputDescriptions = new ArrayList<>();
        List<Component> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslateableText("fmod.node.pentityevt.output.projectile.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.pentityevt.output.projectile.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.pentityevt.output.projectile.type"));
        outputNames.add(Util.parseTranslateableText("fmod.node.pentityevt.output.shooter.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.pentityevt.output.shooter.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.pentityevt.output.shooter.type"));
        outputNames.add(Util.parseTranslateableText("fmod.node.pentityevt.output.entity.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.pentityevt.output.entity.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.pentityevt.output.entity.type"));
        outputNames.add(Util.parseTranslateableText("fmod.node.pentityevt.output.position.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.pentityevt.output.position.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.pentityevt.output.position.type"));
        outputNames.add(Util.parseTranslateableText("fmod.node.pentityevt.output.distance.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.pentityevt.output.distance.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.pentityevt.output.distance.type"));
        List<Component> branchNames = new ArrayList<>();
        List<Component> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslateableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslateableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }
}
