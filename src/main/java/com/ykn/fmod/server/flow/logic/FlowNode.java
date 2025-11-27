package com.ykn.fmod.server.flow.logic;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * This class represents a node in a logic flow.
 * To create a new node with its own functionality, inherit from this class and override necessary methods.
 */
public class FlowNode {

    /**
     * The unique ID of this node within the logic flow.
     */
    private final long id;

    /**
     * This variable is only used for serialization. Usually it should be the same as the class name.
     */
    protected String type;

    /**
     * The name of this node. Can be determined by the user, can be renamed at any time.
     */
    public String name;

    /**
     * The static information about this node.
     * Should not be serialized because it just contains the static information about this node type.
     * We can already have this information from the type of this node.
     */
    protected final transient NodeMetadata metadata;

    /**
     * The input data references for this node.
     */
    private List<DataReference> inputs;

    /**
     * The IDs of the next nodes to execute after this node.
     */
    protected List<Long> nextNodeIds;

    public FlowNode(long id, String name, int inputNumber, int outputNumber, int branchNumber) {
        this.id = id;
        this.type = "AbstractNode";
        this.name = name;
        this.inputs = new ArrayList<>();
        for (int i = 0; i < inputNumber; i++) {
            this.inputs.add(null);
        }
        this.nextNodeIds = new ArrayList<>();
        for (int i = 0; i < branchNumber; i++) {
            this.nextNodeIds.add(-1L);
        }
        this.metadata = createMetadata(inputNumber, outputNumber, branchNumber);
    }

    /**
     * Create the metadata for this node. All the fields should be filled and final in this method.
     * @param inputNumber the number of inputs this node has
     * @param outputNumber the number of outputs this node has
     * @param branchNumber the number of branches this node has
     * @return the metadata object for this node
     */
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        // Should be overridden by subclasses to provide specific metadata
        Text displayName = Util.parseTranslateableText("fmod.node.abstract.title.name");
        Text description = Util.parseTranslateableText("fmod.node.abstract.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        for (int i = 0; i < inputNumber; i++) {
            inputNames.add(Util.parseTranslateableText("fmod.node.abstract.input.name", String.valueOf(i)));
            inputDescriptions.add(Util.parseTranslateableText("fmod.node.abstract.input.feat", String.valueOf(i)));
            inputDataTypes.add(Util.parseTranslateableText("fmod.node.abstract.input.type"));
        }
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        for (int i = 0; i < outputNumber; i++) {
            outputNames.add(Util.parseTranslateableText("fmod.node.abstract.output.name", String.valueOf(i)));
            outputDescriptions.add(Util.parseTranslateableText("fmod.node.abstract.output.feat", String.valueOf(i)));
            outputDataTypes.add(Util.parseTranslateableText("fmod.node.abstract.output.type"));
        }
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        for (int i = 0; i < branchNumber; i++) {
            branchNames.add(Util.parseTranslateableText("fmod.node.abstract.branch.name", String.valueOf(i)));
            branchDescriptions.add(Util.parseTranslateableText("fmod.node.abstract.branch.feat", String.valueOf(i)));
        }
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    } 

    /**
     * Resolves the list of input DataReference objects into their concrete values.
     * The returned list preserves the order of inputs and may contain null values if a constant or a node output is null.
     * @param context the execution context used to resolve node references.
     * @return a List<Object> containing the resolved input values in the same order as the inputs collection
     * @throws LogicException if a referenced node cannot be found or it has not executed yet.
     */
    protected List<Object> resolveInputs(ExecutionContext context) throws LogicException {
        List<Object> resolvedInputs = new ArrayList<>();
        for (DataReference inputRef : inputs) {
            if (inputRef == null) {
                throw new LogicException(null, Util.parseTranslateableText("fmod.flow.error.nullinput", this.name), null);
            } else {
                Object resolvedValue = inputRef.resolve(context);
                resolvedInputs.add(resolvedValue);
            }
        }
        return resolvedInputs;
    }

    /**
     * Executes this flow node with the provided execution context.
     * This method may be called multiple times (loops in the flow are permitted)
     * @param context the execution context used to resolve inputs and to lookup the next node
     * @return the next {@code FlowNode} to execute, or {@code null} if there is no successor
     * @throws LogicException if an error occurs while resolving inputs or executing the node
     */
    public FlowNode execute(ExecutionContext context) throws LogicException {
        // Executed multiple times is expected because we allow loops in logic flows, so no need to check hasExecuted here.
        NodeStatus status = context.getNodeStatus(this.id);
        List<Object> resolvedInputs = resolveInputs(context);
        status.inputs = resolvedInputs;
        this.onExecute(context, status, resolvedInputs);
        long nextNodeId = this.getNextNodeId(context, status, resolvedInputs);
        status.setExecuted();
        status.nextBranchId = nextNodeId;
        return context.getFlow().getNode(nextNodeId);
    }

    /**
     * The actual execution logic of this node.
     * Should write the output values into the outputs list in this method.
     * Should be overridden by subclasses to implement specific node functionality.
     * @param context the execution context used to resolve inputs and to lookup the next node
     * @param resolvedInputs the list of resolved input values
     * @throws LogicException if an error occurs during execution
     */
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        // To be overridden by subclasses to implement specific node logic
    }

    public long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public NodeMetadata getMetadata() {
        return metadata;
    }

    /**
     * Get a specific input reference
     * @param index The index of the input
     * @return The data reference of the input
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public DataReference getInput(int index) {
        return inputs.get(index);
    }

    /**
     * Set a specific input reference
     * @param index The index of the input
     * @param reference The data reference to set
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public void setInput(int index, DataReference reference) {
        inputs.set(index, reference);
    }

    /**
     * Set a specific output reference
     * @param index The index of the output
     * @return The data reference of the output
     * @throws LogicException if the node has not executed yet
     */
    public Object getOutput(ExecutionContext context, int index) throws LogicException {
        NodeStatus status = context.getNodeStatus(this.id);
        if (status.hasExecuted == false) {
            throw new LogicException(null, Util.parseTranslateableText("fmod.flow.error.notexecuted", this.name), null);
        }
        return status.outputs.get(index);
    }

    /**
     * Set a specific output reference.
     * @param index The index of the output
     * @param value The data reference to set
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    protected void setOutput(ExecutionContext context, int index, Object value) {
        NodeStatus status = context.getNodeStatus(this.id);
        status.setOutput(index, value);
    }

    /**
     * This class method determines which node to execute next after this node.
     * Should always be overridden by subclasses.
     * @return -1 if there is no next node. Otherwise return the ID of the next node.
     */
    public long getNextNodeId(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        // Should be overridden to return the correct next node ID based on the node logic
        return nextNodeIds.get(0);
    }
    
    /**
     * Get the next node ID for a specific branch
     * @param branchIndex The index of the branch
     * @param nodeId The ID of the next node
     */
    public void setNextNodeId(int branchIndex, long nodeId) {
        this.nextNodeIds.set(branchIndex, nodeId);
    }

    /**
     * Render this node into a text representation for display.
     * It only renders the static information about this node, not the execution status.
     * To render the execution status, use {@link NodeStatus#render()} instead.
     * @param flow The logic flow this node belongs to
     * @return A text representation of this node
     */
    public Text render(LogicFlow flow) {
        // Render title
        MutableText text = Util.parseTranslateableText("fmod.flow.node.title", this.name, this.metadata.displayName, this.metadata.description);
        text = text.append("\n");
        // Render inputs
        for (int i = 0; i < this.metadata.inputNumber; i++) {
            DataReference inputRef = this.inputs.get(i);
            MutableText inputLine = Util.parseTranslateableText("fmod.flow.node.input", this.metadata.inputNames.get(i), this.metadata.inputDescriptions.get(i));
            // Render optional info about the input source
            if (inputRef != null) {
                if (inputRef.type == DataReference.ReferenceType.CONSTANT && inputRef.value != null) {
                    inputLine = inputLine.append(" (");
                    inputLine = inputLine.append(Util.parseTranslateableText("fmod.flow.node.const", String.valueOf(inputRef.value)));
                    inputLine = inputLine.append(")");
                } else if (inputRef.type == DataReference.ReferenceType.NODE_OUTPUT) {
                    FlowNode refNode = flow.getNode(inputRef.referenceId);
                    if (refNode != null) {
                        inputLine = inputLine.append(" (");
                        inputLine = inputLine.append(Util.parseTranslateableText("fmod.flow.node.from", refNode.name, refNode.metadata.outputNames.get(inputRef.referenceIndex)));
                        inputLine = inputLine.append(")");
                    }
                }
            }
            text = text.append(inputLine).append("\n");
        }
        // Render outputs
        for (int i = 0; i < this.metadata.outputNumber; i++) {
            MutableText outputLine = Util.parseTranslateableText("fmod.flow.node.output", this.metadata.outputNames.get(i), this.metadata.outputDescriptions.get(i));
            text = text.append(outputLine).append("\n");
        }
        // Render branches
        if (this.metadata.branchNumber == 1) {
            // Directly show the next node
            long nextNodeId = this.nextNodeIds.get(0);
            if (flow.getNode(nextNodeId) == null) {
                text = text.append(Util.parseTranslateableText("fmod.flow.node.connect", Util.parseTranslateableText("fmod.misc.null")));
            } else {
                FlowNode nextNode = flow.getNode(nextNodeId);
                text = text.append(Util.parseTranslateableText("fmod.flow.node.connect", nextNode.name));
            }
        } else {
            // List all branches and show their next nodes
            for (int i = 0; i < this.metadata.branchNumber; i++) {
                long nextNodeId = this.nextNodeIds.get(i);
                MutableText branchLine = Util.parseTranslateableText("fmod.flow.node.branch", this.metadata.branchNames.get(i), this.metadata.branchDescriptions.get(i));
                // Show optional info about the next node
                if (flow.getNode(nextNodeId) != null) {
                    FlowNode nextNode = flow.getNode(nextNodeId);
                    MutableText connectText = Util.parseTranslateableText("fmod.flow.node.connect", nextNode.name);
                    branchLine = branchLine.append(" (").append(connectText).append(")");
                }
                text = text.append(branchLine).append("\n");
            }
        }
        return text;
    }
}
