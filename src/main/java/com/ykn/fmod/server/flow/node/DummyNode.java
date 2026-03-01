/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import com.ykn.fmod.server.flow.logic.EventNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;

/**
 * This node will be the first node in a logic flow
 * It does nothing and will never be executed unless explicitly called by user or another logic flow.
 * Inputs: None
 * Outputs: None
 * Branches: 1 (Next node)
 */
public class DummyNode extends EventNode {

    public DummyNode(long id, String name) {
        super(id, name, 0, 0, 1, "DummyNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.dummy.title.name", "fmod.node.dummy.title.feat")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }
}
