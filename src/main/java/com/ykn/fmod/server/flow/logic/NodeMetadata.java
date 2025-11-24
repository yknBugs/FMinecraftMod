package com.ykn.fmod.server.flow.logic;

import java.util.List;

import net.minecraft.text.Text;

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
    public final Text displayName;

    /**
     * The description about the functionality of this node type.
     */
    public final Text description;

    /**
     * The official name of the inputs
     */
    public final List<Text> inputNames;

    /**
     * The description about the inputs
     */
    public final List<Text> inputDescriptions;

    /**
     * The official name of the outputs
     */
    public final List<Text> outputNames;

    /**
     * The description about the outputs
     */
    public final List<Text> outputDescriptions;

    /**
     * The official name of the branches
     */
    public final List<Text> branchNames;
    
    /**
     * The description about the branches
     */
    public final List<Text> branchDescriptions;

    public NodeMetadata(int inputNumber, int outputNumber, int branchNumber,
                        Text displayName, Text description,
                        List<Text> inputNames, List<Text> inputDescriptions,
                        List<Text> outputNames, List<Text> outputDescriptions,
                        List<Text> branchNames, List<Text> branchDescriptions) {
        this.inputNumber = inputNumber;
        this.outputNumber = outputNumber;
        this.branchNumber = branchNumber;
        this.displayName = displayName;
        this.description = description;
        this.inputNames = inputNames;
        this.inputDescriptions = inputDescriptions;
        this.outputNames = outputNames;
        this.outputDescriptions = outputDescriptions;
        this.branchNames = branchNames;
        this.branchDescriptions = branchDescriptions;
    }

}
