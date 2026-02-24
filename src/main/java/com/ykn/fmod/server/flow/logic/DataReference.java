/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.logic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.ykn.fmod.server.base.util.Util;

/**
 * Represents a reference to data that can be used as input for a node.
 * <p>
 * A DataReference can be one of two types:
 * <ul>
 *   <li><b>CONSTANT:</b> A direct value stored in this reference</li>
 *   <li><b>NODE_OUTPUT:</b> A reference to an output from another node</li>
 * </ul>
 * <p>
 * This abstraction allows nodes to accept inputs from either constant values
 * or computed values from other nodes in the flow, enabling flexible data flow.
 * <p>
 * Use the static factory methods to create instances:
 * <ul>
 *   <li>{@link #createConstantReference(Object)} for constant values</li>
 *   <li>{@link #createNodeOutputReference(long, int)} for node outputs</li>
 *   <li>{@link #createEmptyReference()} for uninitialized references</li>
 * </ul>
 * 
 * @see FlowNode
 * @see ExecutionContext
 */
public class DataReference implements Cloneable {

    /**
     * Enumeration of the types of data references supported.
     */
    public enum ReferenceType {
        /**
         * The reference holds a constant value directly.
         */
        CONSTANT,
        
        /**
         * The reference points to an output from another node.
         */
        NODE_OUTPUT
    }

    /**
     * Indicates the type of this data reference (CONSTANT or NODE_OUTPUT).
     */
    private final ReferenceType type;

    /**
     * The actual constant value when type is CONSTANT.
     * <p>
     * This field is only used when {@link #type} is {@link ReferenceType#CONSTANT}.
     * It holds the literal value that will be returned by {@link #resolve}.
     * For NODE_OUTPUT type references, this field should be null.
     */
    private final Object value;

    /**
     * The ID of the node that provides the data when type is NODE_OUTPUT.
     * <p>
     * This field is only used when {@link #type} is {@link ReferenceType#NODE_OUTPUT}.
     * It identifies which node's output should be retrieved.
     * For CONSTANT type references, this should be set to -1.
     */
    private final long referenceId;

    /**
     * The output port index when type is NODE_OUTPUT.
     * <p>
     * This field is only used when {@link #type} is {@link ReferenceType#NODE_OUTPUT}.
     * Since one node may have multiple outputs, this indicates which output port to use (0-based).
     * For CONSTANT type references, this should be set to -1.
     */
    private final int referenceIndex; 

    /**
     * Private constructor to initialize a DataReference with the specified parameters.
     * <p>
     * This constructor is not intended to be called directly; use the static factory methods instead.
     * 
     * @param type The type of this data reference (CONSTANT or NODE_OUTPUT)
     * @param value The constant value (only used if type is CONSTANT, otherwise should be null)
     * @param referenceId The ID of the referenced node (only used if type is NODE_OUTPUT, otherwise should be -1)
     * @param referenceIndex The output port index (only used if type is NODE_OUTPUT, otherwise should be -1)
     */
    private DataReference(ReferenceType type, Object value, long referenceId, int referenceIndex) {
        this.type = type;
        this.value = value;
        this.referenceId = referenceId;
        this.referenceIndex = referenceIndex;
    }

    /**
     * Creates a constant data reference with the specified value.
     * <p>
     * The reference will directly hold and return the provided value when resolved.
     * 
     * @param value The constant value to store in this reference (can be null)
     * @return A new DataReference of type CONSTANT
     */
    public static DataReference createConstantReference(Object value) {
        return new DataReference(ReferenceType.CONSTANT, value, -1, -1);
    }

    /**
     * Creates a node output reference pointing to another node's output.
     * <p>
     * The reference will retrieve the specified output from the specified node when resolved.
     * 
     * @param nodeId The ID of the node whose output to reference
     * @param outputIndex The index of the output port (0-based)
     * @return A new DataReference of type NODE_OUTPUT
     */
    public static DataReference createNodeOutputReference(long nodeId, int outputIndex) {
        return new DataReference(ReferenceType.NODE_OUTPUT, null, nodeId, outputIndex);
    }

    /**
     * Creates an empty data reference with a null constant value.
     * <p>
     * This is useful for initializing input ports that haven't been configured yet.
     * 
     * @return A new DataReference of type CONSTANT with a null value
     */
    public static DataReference createEmptyReference() {
        return createConstantReference(null);
    }

    /**
     * Resolves this data reference to its actual value.
     * <p>
     * The resolution behavior depends on the reference type:
     * <ul>
     *   <li><b>CONSTANT:</b> Returns the stored value directly</li>
     *   <li><b>NODE_OUTPUT:</b> Retrieves the output from the referenced node</li>
     * </ul>
     * <p>
     * For NODE_OUTPUT references, the referenced node must have already been executed
     * in the given context, or a {@link LogicException} will be thrown.
     * 
     * @param context The execution context containing node execution states
     * @return The resolved value, which may be null
     * @throws LogicException If the referenced node doesn't exist or hasn't been executed yet
     */
    @Nullable
    public Object resolve(@NotNull ExecutionContext context, @NotNull FlowNode currentNode) throws LogicException {
        if (this.type == ReferenceType.CONSTANT) {
            return this.value;
        } else if (this.type == ReferenceType.NODE_OUTPUT) {
            LogicFlow flow = context.getFlow();
            FlowNode node = flow.getNode(this.referenceId);
            if (node == null) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.flow.error.nullnode", currentNode.name), null);
            }
            return node.getOutput(context, this.referenceIndex);
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.flow.error.assert"), null);
        }
    }

    /**
     * Creates a shallow copy of this data reference.
     * <p>
     * The copy includes all fields (type, value, referenceId, referenceIndex).
     * Note: The value object itself is not deep copied; only the reference is copied.
     * 
     * @return A new DataReference with the same configuration
     */
    @NotNull
    public DataReference copy() {
        return new DataReference(this.type, this.value, this.referenceId, this.referenceIndex);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return this.copy();
    }

    /**
     * Gets the type of this data reference.
     * 
     * @return The reference type (CONSTANT or NODE_OUTPUT)
     */
    public ReferenceType getType() {
        return type;
    }

    /**
     * Gets the constant value stored in this reference.
     * <p>
     * This is only meaningful if the reference type is CONSTANT; otherwise, it may return null.
     * 
     * @return The constant value, or null if this reference is not of type CONSTANT
     */
    public Object getValue() {
        return value;
    }

    /**
     * Gets the ID of the node that this reference points to (if type is NODE_OUTPUT).
     * <p>
     * This is only meaningful if the reference type is NODE_OUTPUT; otherwise, it will return -1.
     * 
     * @return The referenced node ID, or -1 if this reference is not of type NODE_OUTPUT
     */
    public long getReferenceId() {
        return referenceId;
    }

    /**
     * Gets the output port index that this reference points to (if type is NODE_OUTPUT).
     * <p>
     * This is only meaningful if the reference type is NODE_OUTPUT; otherwise, it will return -1.
     * 
     * @return The referenced output index, or -1 if this reference is not of type NODE_OUTPUT
     */
    public int getReferenceIndex() {
        return referenceIndex;
    }

    @Override
    public String toString() {
        if (this.type == ReferenceType.CONSTANT) {
            return "DataReference{type=CONSTANT, value=" + this.value + "}";
        } else if (this.type == ReferenceType.NODE_OUTPUT) {
            return "DataReference{type=NODE_OUTPUT, referenceId=" + this.referenceId + ", referenceIndex=" + this.referenceIndex + "}";
        } else {
            return "DataReference{type=" + this.type + "}";
        }
    }
}
