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
 * This node can be triggered by a dedicated command by players without OP permission.
 * Inputs: None
 * Outputs: 
 * 1. Player - The player who triggered this node.
 * Branches: 1 (Next node)
 */
public class TriggerNode extends EventNode {

    public TriggerNode(long id, String name) {
        super(id, name, 0, 1, 1);
        this.type = "TriggerNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Component displayName = Util.parseTranslatableText("fmod.node.trigger.title.name");
        Component description = Util.parseTranslatableText("fmod.node.trigger.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        List<Component> outputNames = new ArrayList<>();
        List<Component> outputDescriptions = new ArrayList<>();
        List<Component> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.trigger.output.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.trigger.output.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.trigger.output.type"));
        List<Component> branchNames = new ArrayList<>();
        List<Component> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

}
