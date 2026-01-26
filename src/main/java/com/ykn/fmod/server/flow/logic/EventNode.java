/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.logic;

import java.util.List;

public class EventNode extends FlowNode {

    public EventNode(long id, String name, int inputNumber, int outputNumber, int branchNumber) {
        super(id, name, inputNumber, outputNumber, branchNumber);
        this.type = "AbstractEventNode";
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        // Do nothing, because the event node's output is managed by the ExecutionContext
    }

    @Override
    public boolean isEventNode() {
        return true;
    }
}
