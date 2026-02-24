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

/**
 * A node to set an object at a specific index in a list.
 * Inputs:
 * 1. List - The list to set the object in, can be any type of list.
 * 2. Number - The index of the object to set, if negative, it will count from the end of the list.
 * 3. Object - The object to set at the specified index in the list, can be null.
 * 4. Boolean - If true, replace the existing element, if false, insert the object at the specified index. Default true.
 * 5. Boolean - If true and the Object input is also a list, flatten the Object input. Default true.
 * 6. Boolean - If true, strip all the leading and tailing null values. Default true.
 * Outputs:
 * 1. List - The list after setting the object.
 * 2. Object - The original object at the specified index in the list before setting.
 * 3. Number - The number of elements in the list before setting the object.
 * 4. Number - The number of elements in the list after setting the object.
 * Branches: 1 (Next node)
 */
public class SetObjectAtIndexNode extends FlowNode {
    
    public SetObjectAtIndexNode(long id, String name) {
        super(id, name, 6, 4, 1);
        this.type = "SetObjectAtIndexNode";
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
            boolean isReplace = isReplaceBool == null ? true : isReplaceBool;

            Boolean isFlattenBool = TypeAdaptor.parse(resolvedInputs.get(4)).asBoolean();
            boolean isFlatten = isFlattenBool == null ? true : isFlattenBool;

            Boolean isStripBool = TypeAdaptor.parse(resolvedInputs.get(5)).asBoolean();
            boolean isStrip = isStripBool == null ? true : isStripBool; 
            
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
                } else if (replacedObjects.size() == 1) {
                    status.setOutput(1, replacedObjects.get(0));
                } else {
                    status.setOutput(1, replacedObjects);
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
                while (inputList.size() > 0 && inputList.get(0) == null) {
                    inputList.remove(0);
                }
                // Strip nulls at the end
                while (inputList.size() > 0 && inputList.get(inputList.size() - 1) == null) {
                    inputList.remove(inputList.size() - 1);
                }
            }
            
            // Set outputs
            status.setOutput(0, inputList);
            status.setOutput(2, sizeBefore);
            status.setOutput(3, inputList.size());
        } catch (OutOfMemoryError e) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.setindexat.error.oom"), null);
        } 
    }

    private List<Object> parseList(Object obj) {
        // Return a copy of the original list
        if (obj == null) {
            return new ArrayList<Object>();
        }
        List<Object> tryCastList = TypeAdaptor.parse(obj).asList();
        if (tryCastList == null) {
            List<Object> singleItemList = new ArrayList<>();
            singleItemList.add(obj);
            return singleItemList;
        }
        return tryCastList;
    }
}
