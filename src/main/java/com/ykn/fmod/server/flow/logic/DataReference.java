package com.ykn.fmod.server.flow.logic;

/**
 * Represents the input or output data of a FlowNode.
 * It can be a constant value or a reference to another node's output.
 */
public class DataReference {

    public enum ReferenceType {
        CONSTANT,
        NODE_OUTPUT
    }

    /**
     * Indicates the type of this data reference
     */
    public ReferenceType type;

    /**
     * Used only when the type is CONSTANT
     * This variable holds the actual constant value
     */
    public Object value;

    /**
     * Used only when the type is NODE_OUTPUT
     * This variable holds the node that provides the data
     */
    public long referenceId;

    /**
     * Used only when the type is NODE_OUTPUT
     * One node may have multiple outputs, this variable indicates which output to use
     */
    public int referenceIndex; 

    /**
     * Create a constant data reference
     * @param value The constant value
     * @return The data reference
     */
    public static DataReference createConstantReference(Object value) {
        DataReference ref = new DataReference();
        ref.type = ReferenceType.CONSTANT;
        ref.value = value;
        ref.referenceId = -1;
        ref.referenceIndex = -1;
        return ref;
    }

    /**
     * Create a node output data reference
     * @param nodeId The ID of the node
     * @param outputIndex The index of the output
     * @return The data reference
     */
    public static DataReference createNodeOutputReference(long nodeId, int outputIndex) {
        DataReference ref = new DataReference();
        ref.type = ReferenceType.NODE_OUTPUT;
        ref.value = null;
        ref.referenceId = nodeId;
        ref.referenceIndex = outputIndex;
        return ref;
    }

    /**
     * Create an empty data reference (null constant)
     * @return The data reference
     */
    public static DataReference createEmptyReference() {
        return createConstantReference(null);
    }
}
