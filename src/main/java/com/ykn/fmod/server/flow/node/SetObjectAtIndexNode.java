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

import com.ykn.fmod.server.flow.tool.FlowConvert;

/**
 * A flow node that writes an object at an index of a list.
 * Inputs:
 * 1. List - The list to set the object in.
 * 2. Integer - The index to set the object at, starting from 0. Negative values count from the end.
 * 3. Object - The object to set at the specified index.
 * 4. Boolean - If true, replace the element at the index. If false, insert and shift existing elements. Treat null as true.
 * 5. Boolean - If true and Object is a list, expand its elements. If false, treat Object as a single element. Treat null as true.
 * 6. Boolean - If true, remove null values from the beginning and end of the result. Treat null as true.
 * Outputs:
 * 1. List - The list after setting the object.
 * 2. Object - The original object at the specified index before modification.
 * 3. Integer - The number of elements in the list before setting the object.
 * 4. Integer - The number of elements in the list after setting the object.
 * Branches: 1 (Next node)
 */
public class SetObjectAtIndexNode extends FlowNode {
    
    public SetObjectAtIndexNode(long id, String name) {
        super(id, name, 6, 4, 1, "SetObjectAtIndexNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.setindexat.title.name", "fmod.node.setindexat.title.feat")
            .input("fmod.node.setindexat.input.list.name", "fmod.node.setindexat.input.list.feat", "fmod.node.setindexat.input.list.type")
            .input("fmod.node.setindexat.input.index.name", "fmod.node.setindexat.input.index.feat", "fmod.node.setindexat.input.index.type")
            .input("fmod.node.setindexat.input.object.name", "fmod.node.setindexat.input.object.feat", "fmod.node.setindexat.input.object.type")
            .input("fmod.node.setindexat.input.replace.name", "fmod.node.setindexat.input.replace.feat", "fmod.node.setindexat.input.replace.type")
            .input("fmod.node.setindexat.input.flatten.name", "fmod.node.setindexat.input.flatten.feat", "fmod.node.setindexat.input.flatten.type")
            .input("fmod.node.setindexat.input.strip.name", "fmod.node.setindexat.input.strip.feat", "fmod.node.setindexat.input.strip.type")
            .output("fmod.node.setindexat.output.list.name", "fmod.node.setindexat.output.list.feat", "fmod.node.setindexat.output.list.type")
            .output("fmod.node.setindexat.output.object.name", "fmod.node.setindexat.output.object.feat", "fmod.node.setindexat.output.object.type")
            .output("fmod.node.setindexat.output.sizebefore.name", "fmod.node.setindexat.output.sizebefore.feat", "fmod.node.setindexat.output.sizebefore.type")
            .output("fmod.node.setindexat.output.sizeafter.name", "fmod.node.setindexat.output.sizeafter.feat", "fmod.node.setindexat.output.sizeafter.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        try {
            List<Object> inputList = parseList(resolvedInputs.get(0));
            
            Double indexDouble = TypeAdaptor.parse(resolvedInputs.get(1)).asDouble();

            Object objectToSet = resolvedInputs.get(2);
            
            Boolean isReplaceBool = TypeAdaptor.parse(resolvedInputs.get(3)).asBoolean();
            boolean isReplace = isReplaceBool == null || isReplaceBool;

            Boolean isFlattenBool = TypeAdaptor.parse(resolvedInputs.get(4)).asBoolean();
            boolean isFlatten = isFlattenBool == null || isFlattenBool;

            Boolean isStripBool = TypeAdaptor.parse(resolvedInputs.get(5)).asBoolean();
            boolean isStrip = isStripBool == null || isStripBool;
            
            int sizeBefore = inputList.size();
            
            // Determine index
            int index = inputList.size();
            if (indexDouble != null) {
                index = indexDouble.intValue();
                if (index < 0) {
                    index = sizeBefore + index;
                    // index can still be negative here
                }
            }

            // Validate the peak size before padding, copying insertion values, or publishing outputs.
            // Use long arithmetic so extreme negative indices and flattened inserts cannot overflow.
            long paddedSize = index < 0 ? (long) sizeBefore - index : Math.max((long) sizeBefore, index);
            long start = Math.max(0, index);
            long inserted = isFlatten && objectToSet instanceof List<?> list ? list.size() : 1;
            long peakSize = isReplace ? Math.max(paddedSize, start + inserted) : paddedSize + inserted;
            FlowConvert.checkListSize(peakSize);

            // Append nulls if index is negative and out of bounds
            if (index < 0) {
                int nullsToAdd = -index;
                for (int i = 0; i < nullsToAdd; i++) {
                    inputList.add(0, null);
                }
                index = 0;
            }

            // Append nulls if index is positive and out of bounds
            if (index > sizeBefore) {
                while (inputList.size() < index) {
                    inputList.add(null);
                }
            }

            // Prepare Elements
            List<Object> objectsToInsert = new ArrayList<>();
            if (isFlatten && objectToSet instanceof List) {
                objectsToInsert.addAll((List<?>) objectToSet);
            } else {
                objectsToInsert.add(objectToSet);
            }
            
            if (isReplace) {
                // Perform replacement
                List<Object> replacedObjects = new ArrayList<>();
                for (int i = 0; i < objectsToInsert.size(); i++) {
                    int currentIndex = index + i;
                    if (currentIndex < inputList.size()) {
                        Object replacedObject = inputList.get(currentIndex);
                        if (replacedObject != null) {
                            replacedObjects.add(replacedObject);
                        }
                        inputList.set(currentIndex, objectsToInsert.get(i));
                    } else {
                        while (inputList.size() < currentIndex) {
                            inputList.add(null);
                        }
                        inputList.add(objectsToInsert.get(i));
                    }
                }

                // Set Output value
                if (replacedObjects.isEmpty()) {
                    status.setOutput(1, null);
                } else {
                    status.setOutput(1, FlowConvert.unwrapOutput(replacedObjects));
                }
            } else {
                // Set Output value
                if (index < inputList.size()) {
                    status.setOutput(1, inputList.get(index));
                } else {
                    status.setOutput(1, null);
                }

                // Insert elements
                for (int i = objectsToInsert.size() - 1; i >= 0; i--) {
                    inputList.add(index, objectsToInsert.get(i));
                }
            }

            if (isStrip) {
                // Strip nulls at the beginning
                while (!inputList.isEmpty() && inputList.get(0) == null) {
                    inputList.remove(0);
                }
                // Strip nulls at the end
                while (!inputList.isEmpty() && inputList.get(inputList.size() - 1) == null) {
                    inputList.remove(inputList.size() - 1);
                }
            }
            
            // Set outputs
            status.setOutput(0, FlowConvert.unwrapOutput(inputList));
            status.setOutput(2, sizeBefore);
            status.setOutput(3, inputList.size());
        } catch (OutOfMemoryError e) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.setindexat.error.oom"), null);
        } 
    }

    private List<Object> parseList(Object obj) throws LogicException {
        return FlowConvert.listInput(obj);
    }
}
