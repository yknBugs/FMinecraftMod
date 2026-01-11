package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.EventNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;

import net.minecraft.text.Text;

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
        super(id, name, 0, 6, 1);
        this.type = "EntityDamageEventNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Text displayName = Util.parseTranslateableText("fmod.node.damageevt.title.name");
        Text description = Util.parseTranslateableText("fmod.node.damageevt.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslateableText("fmod.node.damageevt.output.victim.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.damageevt.output.victim.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.damageevt.output.victim.type"));
        outputNames.add(Util.parseTranslateableText("fmod.node.damageevt.output.amount.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.damageevt.output.amount.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.damageevt.output.amount.type"));
        outputNames.add(Util.parseTranslateableText("fmod.node.damageevt.output.damage.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.damageevt.output.damage.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.damageevt.output.damage.type"));
        outputNames.add(Util.parseTranslateableText("fmod.node.damageevt.output.attacker.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.damageevt.output.attacker.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.damageevt.output.attacker.type"));
        outputNames.add(Util.parseTranslateableText("fmod.node.damageevt.output.source.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.damageevt.output.source.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.damageevt.output.source.type"));
        outputNames.add(Util.parseTranslateableText("fmod.node.damageevt.output.position.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.damageevt.output.position.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.damageevt.output.position.type"));
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslateableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslateableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

}
