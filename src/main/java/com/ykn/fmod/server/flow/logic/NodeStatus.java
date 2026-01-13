package com.ykn.fmod.server.flow.logic;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The runtime status of a node during execution.
 * In a multi-threaded execution context, each thread should have its own NodeStatus instance for each node.
 */
public class NodeStatus implements Cloneable {

    /**
     * The node this status belongs to.
     * Shallow copy is sufficient for this variable.
     */
    public FlowNode node;

    /**
     * The input data references for this node. Should not be serialized because it is a runtime state.
     * Copy is required for this variable.
     */
    public List<Object> inputs;

    /**
     * Indicates whether this node has been executed in the current execution context. Should not be serialized.
     * Deep copy is required for this variable.
     */
    public boolean hasExecuted;

    /**
     * The output data references for this node. Should not be serialized because it is a runtime state.
     * Copy is required for this variable.
     */
    public List<Object> outputs;

    /**
     * The next branch ID to take after execution. Should not be serialized because it is a runtime state.
     * Deep copy is required for this variable.
     */
    public long nextBranchId;

    public NodeStatus(FlowNode node) {
        this.node = node;
        int inputNumber = node.metadata.inputNumber;
        this.inputs = new ArrayList<>();
        for (int i = 0; i < inputNumber; i++) {
            this.inputs.add(null);
        }
        this.hasExecuted = false;
        int outputNumber = node.metadata.outputNumber;
        this.outputs = new ArrayList<>();
        for (int i = 0; i < outputNumber; i++) {
            this.outputs.add(null);
        }
        this.nextBranchId = -1;
    }

    public NodeStatus copy() {
        NodeStatus cloned = new NodeStatus(node);
        int inputNumber = node.metadata.inputNumber;
        cloned.inputs = new ArrayList<>();
        for (int i = 0; i < inputNumber; i++) {
            cloned.inputs.add(this.inputs.get(i));
        }
        cloned.hasExecuted = this.hasExecuted;
        int outputNumber = node.metadata.outputNumber;
        cloned.outputs = new ArrayList<>();
        for (int i = 0; i < outputNumber; i++) {
            cloned.outputs.add(this.outputs.get(i));
        }
        cloned.nextBranchId = this.nextBranchId;
        return cloned;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return this.copy();
    }

    public void setExecuted() {
        this.hasExecuted = true;
    }

    public void reset() {
        for (int i = 0; i < this.inputs.size(); i++) {
            this.inputs.set(i, null);
        }
        this.hasExecuted = false;
        for (int i = 0; i < this.outputs.size(); i++) {
            this.outputs.set(i, null);
        }
        this.nextBranchId = -1;
    }

    public void setOutput(int index, Object value) {
        this.outputs.set(index, value);
    }

    /**
     * Render this node into a text representation for display.
     * It renders the execution status of this node.
     * To render the static information about this node, use {@link FlowNode#render()} instead.
     * @param index The index of this node in the execution sequence
     * @return A text representation of this node
     */
    public Component render(long index, LogicFlow flow) {
        // Render title
        MutableComponent text = Util.parseTranslateableText("fmod.flow.execute.node", String.valueOf(index), this.node.name, this.node.metadata.displayName);
        text = text.append("\n");
        // Render inputs
        for (int i = 0; i < this.node.metadata.inputNumber; i++) {
            Object inputValue = this.inputs.get(i);
            if (inputValue == null) {
                text = text.append(Util.parseTranslateableText("fmod.flow.execute.input", this.node.metadata.inputNames.get(i), Util.parseTranslateableText("fmod.misc.null")));
            } else {
                text = text.append(Util.parseTranslateableText("fmod.flow.execute.input", this.node.metadata.inputNames.get(i), String.valueOf(inputValue)));
            }
            text = text.append("\n");
        }
        // Render outputs
        for (int i = 0; i < this.node.metadata.outputNumber; i++) {
            Object outputValue = this.outputs.get(i);
            if (outputValue == null) {
                text = text.append(Util.parseTranslateableText("fmod.flow.execute.output", this.node.metadata.outputNames.get(i), Util.parseTranslateableText("fmod.misc.null")));
            } else {
                text = text.append(Util.parseTranslateableText("fmod.flow.execute.output", this.node.metadata.outputNames.get(i), String.valueOf(outputValue)));
            }
            text = text.append("\n");
        }
        // Render next node
        FlowNode nextNode = flow.getNode(this.nextBranchId);
        if (nextNode == null) {
            text = text.append(Util.parseTranslateableText("fmod.flow.execute.next", Util.parseTranslateableText("fmod.misc.null")));
        } else {
            text = text.append(Util.parseTranslateableText("fmod.flow.execute.next", nextNode.name));
        }
        return text;
    }
}
