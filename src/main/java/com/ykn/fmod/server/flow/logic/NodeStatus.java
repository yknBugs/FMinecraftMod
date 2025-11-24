package com.ykn.fmod.server.flow.logic;

import java.util.ArrayList;
import java.util.List;

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
     * Indicates whether this node has been executed in the current execution context. Should not be serialized.
     * Deep copy is required for this variable.
     */
    public boolean hasExecuted;

    /**
     * The output data references for this node. Should not be serialized because it is a runtime state.
     * Deep copy is required for this variable.
     */
    public List<Object> outputs;

    public NodeStatus(FlowNode node) {
        this.node = node;
        this.hasExecuted = false;
        int outputNumber = node.metadata.outputNumber;
        this.outputs = new ArrayList<>();
        for (int i = 0; i < outputNumber; i++) {
            this.outputs.add(null);
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        NodeStatus cloned = new NodeStatus(node);
        cloned.hasExecuted = this.hasExecuted;
        int outputNumber = node.metadata.outputNumber;
        cloned.outputs = new ArrayList<>();
        for (int i = 0; i < outputNumber; i++) {
            cloned.outputs.add(this.outputs.get(i));
        }
        return cloned;
    }

    public void setExecuted() {
        this.hasExecuted = true;
    }

    public void reset() {
        this.hasExecuted = false;
        for (int i = 0; i < this.outputs.size(); i++) {
            this.outputs.set(i, null);
        }
    }

    public void setOutput(int index, Object value) {
        this.outputs.set(index, value);
    }
}
