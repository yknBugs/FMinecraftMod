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

import net.minecraft.text.Text;

/**
 * This node can be triggered by a dedicated command by players without OP permission.
 * Inputs: None
 * Outputs: 
 * 1. Player - The player who triggered this node.
 * 2. String - The parameter provided by the player. Empty String if not provided.
 * Branches: 1 (Next node)
 */
public class TriggerNode extends EventNode {

    public TriggerNode(long id, String name) {
        super(id, name, 0, 2, 1);
        this.type = "TriggerNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Text displayName = Util.parseTranslatableText("fmod.node.trigger.title.name");
        Text description = Util.parseTranslatableText("fmod.node.trigger.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.trigger.output.player.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.trigger.output.player.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.trigger.output.player.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.trigger.output.param.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.trigger.output.param.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.trigger.output.param.type"));
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

}
