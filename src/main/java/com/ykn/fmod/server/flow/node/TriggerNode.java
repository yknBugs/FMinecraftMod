/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import com.ykn.fmod.server.flow.logic.EventNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;

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
        return NodeMetadata.builder("fmod.node.trigger.title.name", "fmod.node.trigger.title.feat")
            .output("fmod.node.trigger.output.player.name", "fmod.node.trigger.output.player.feat", "fmod.node.trigger.output.player.type")
            .output("fmod.node.trigger.output.param.name", "fmod.node.trigger.output.param.feat", "fmod.node.trigger.output.param.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

}
