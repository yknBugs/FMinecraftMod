package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.EventNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;

import net.minecraft.network.chat.Component;

/**
 * This node will be the first node in a logic flow
 * It does nothing and will never be executed unless explicitly called by user or another logic flow.
 * Inputs: None
 * Outputs: None
 * Branches: 1 (Next node)
 */
public class DummyNode extends EventNode {

    public DummyNode(long id, String name) {
        super(id, name, 0, 0, 1);
        this.type = "DummyNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Component displayName = Util.parseTranslateableText("fmod.node.dummy.title.name");
        Component description = Util.parseTranslateableText("fmod.node.dummy.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        List<Component> outputNames = new ArrayList<>();
        List<Component> outputDescriptions = new ArrayList<>();
        List<Component> outputDataTypes = new ArrayList<>();
        List<Component> branchNames = new ArrayList<>();
        List<Component> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslateableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslateableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }
}
