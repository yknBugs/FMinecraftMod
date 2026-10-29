/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;

import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import com.ykn.fmod.server.flow.tool.FlowConvert;

/**
 * A flow node that reads the object at an index of a list.
 * Inputs:
 * 1. List - The list to get the object from.
 * 2. Integer - The index of the object to get, starting from 0. Negative values count from the end.
 * 3. String - Optional variable name to store the result.
 * Outputs:
 * 1. Object - The object at the specified index.
 * 2. Integer - The number of elements in the list.
 * Branches: 1 (Next node)
 */
public class GetObjectAtIndexNode extends FlowNode {
    
    public GetObjectAtIndexNode(long id, String name) {
        super(id, name, 3, 2, 1, "GetObjectAtIndexNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.getindexat.title.name", "fmod.node.getindexat.title.feat")
            .input("fmod.node.getindexat.input.list.name", "fmod.node.getindexat.input.list.feat", "fmod.node.getindexat.input.list.type")
            .input("fmod.node.getindexat.input.index.name", "fmod.node.getindexat.input.index.feat", "fmod.node.getindexat.input.index.type")
            .input("fmod.node.getindexat.input.name.name", "fmod.node.getindexat.input.name.feat", "fmod.node.getindexat.input.name.type")
            .output("fmod.node.getindexat.output.object.name", "fmod.node.getindexat.output.object.feat", "fmod.node.getindexat.output.object.type")
            .output("fmod.node.getindexat.output.size.name", "fmod.node.getindexat.output.size.feat", "fmod.node.getindexat.output.size.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        List<Object> list = parseList(resolvedInputs.get(0));

        Object indexObj = resolvedInputs.get(1);
        Double indexDouble = TypeAdaptor.parse(indexObj).asDouble();

        Object varNameObj = resolvedInputs.get(2);
        String varName = varNameObj == null ? null : TypeAdaptor.parse(varNameObj).asString();
        
        int listSize = list.size();
        Object resultObj = null;
        if (indexDouble != null) {
            int index = indexDouble.intValue();
            if (index < 0) {
                index = listSize + index;
            }
            if (index >= 0 && index < listSize) {
                resultObj = list.get(index);
            }
        } else if (listSize > 0) {
            resultObj = list.get(listSize - 1);
        }

        if (varName != null) {
            context.setVariable(varName, resultObj);
        }
        status.setOutput(0, resultObj);
        status.setOutput(1, listSize);
    }

    private List<Object> parseList(Object obj) throws LogicException {
        return FlowConvert.listInput(obj);
    }
}
