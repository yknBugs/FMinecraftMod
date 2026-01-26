/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.EventNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;

import net.minecraft.network.chat.Component;

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
        Component displayName = Util.parseTranslatableText("fmod.node.deathevt.title.name");
        Component description = Util.parseTranslatableText("fmod.node.deathevt.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        List<Component> outputNames = new ArrayList<>();
        List<Component> outputDescriptions = new ArrayList<>();
        List<Component> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.deathevt.output.victim.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.deathevt.output.victim.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.deathevt.output.victim.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.deathevt.output.damage.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.deathevt.output.damage.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.deathevt.output.damage.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.deathevt.output.attacker.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.deathevt.output.attacker.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.deathevt.output.attacker.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.deathevt.output.source.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.deathevt.output.source.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.deathevt.output.source.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.deathevt.output.position.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.deathevt.output.position.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.deathevt.output.position.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.deathevt.output.message.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.deathevt.output.message.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.deathevt.output.message.type"));
        List<Component> branchNames = new ArrayList<>();
        List<Component> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }
}
