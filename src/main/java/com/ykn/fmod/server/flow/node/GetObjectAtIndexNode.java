/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.text.Text;

/**
 * A node to get an object at a specific index in a list
 * Inputs:
 * 1. List - The list to get the object from, can be any type of list
 * 2. Number - The index of the object to get, if negative, it will count from the end of the list, if out of bounds, the output will be null
 * 3. String - If not null, the output will be stored in the variable even if the output is null
 * Outputs:
 * 1. Object - The object at the specified index in the list
 * 2. Number - The number of elements in the list
 * Branches: 1 (Next node)
 */
public class GetObjectAtIndexNode extends FlowNode {
    
    public GetObjectAtIndexNode(long id, String name) {
        super(id, name, 3, 2, 1);
        this.type = "GetObjectAtIndexNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Text displayName = Util.parseTranslatableText("fmod.node.getindexat.title.name");
        Text description = Util.parseTranslatableText("fmod.node.getindexat.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.getindexat.input.list.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getindexat.input.list.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getindexat.input.list.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.getindexat.input.index.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getindexat.input.index.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getindexat.input.index.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.getindexat.input.name.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getindexat.input.name.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getindexat.input.name.type"));
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.getindexat.output.object.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getindexat.output.object.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getindexat.output.object.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getindexat.output.size.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getindexat.output.size.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getindexat.output.size.type"));
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        List<?> list = parseList(resolvedInputs.get(0));

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

    private List<?> parseList(Object obj) {
        if (obj == null) {
            return new ArrayList<Object>();
        }
        if (obj instanceof List) {
            return (List<?>) obj;
        }
        List<Object> singleItemList = new ArrayList<>();
        singleItemList.add(obj);
        return singleItemList;
    }
}
