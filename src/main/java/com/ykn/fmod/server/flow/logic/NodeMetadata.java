package com.ykn.fmod.server.flow.logic;

import java.util.List;

import net.minecraft.network.chat.Component;

/**
 * Metadata about a node type.
 * Users are not allowed to modify the metadata.
 * The data like {@code name} and {@code inputs} can be modified by users, so they should be in the {@link FlowNode} class, not here.
 */
public class NodeMetadata {

    /**
     * The number of inputs this node has.
     */
    public final int inputNumber;

    /**
     * The number of outputs this node has.
     */
    public final int outputNumber;

    /**
     * The branch number of this node.
     * Usually 1 for most nodes. 2 for simple if nodes (true/false).
     */
    public final int branchNumber;

    /**
     * The official name of this node type.
     */
    public final Component displayName;

    /**
     * The description about the functionality of this node type.
     */
    public final Component description;

    /**
     * The official name of the inputs
     */
    public final List<Component> inputNames;

    /**
     * The description about the inputs
     */
    public final List<Component> inputDescriptions;

    /**
     * The data types of the inputs
     */
    public final List<Component> inputDataTypes;

    /**
     * The official name of the outputs
     */
    public final List<Component> outputNames;

    /**
     * The description about the outputs
     */
    public final List<Component> outputDescriptions;

    /**
     * The data types of the outputs
     */
    public final List<Component> outputDataTypes;

    /**
     * The official name of the branches
     */
    public final List<Component> branchNames;
    
    /**
     * The description about the branches
     */
    public final List<Component> branchDescriptions;

    public NodeMetadata(int inputNumber, int outputNumber, int branchNumber,
                        Component displayName, Component description,
                        List<Component> inputNames, List<Component> inputDescriptions, List<Component> inputDataTypes,
                        List<Component> outputNames, List<Component> outputDescriptions, List<Component> outputDataTypes,
                        List<Component> branchNames, List<Component> branchDescriptions) {
        this.inputNumber = inputNumber;
        this.outputNumber = outputNumber;
        this.branchNumber = branchNumber;
        this.displayName = displayName;
        this.description = description;
        this.inputNames = inputNames;
        this.inputDescriptions = inputDescriptions;
        this.inputDataTypes = inputDataTypes;
        this.outputNames = outputNames;
        this.outputDescriptions = outputDescriptions;
        this.outputDataTypes = outputDataTypes;
        this.branchNames = branchNames;
        this.branchDescriptions = branchDescriptions;
    }

}
