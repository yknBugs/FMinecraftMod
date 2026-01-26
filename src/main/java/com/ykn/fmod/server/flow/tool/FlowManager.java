/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.tool;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.DataReference;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.LogicFlow;

/**
 * This class represents a manager for a logic flow
 * It is used to track how the user modified the flow, and its status.
 */
public class FlowManager {

    /**
     * The logic flow instance.
     */
    public LogicFlow flow;

    /**
     * Only activated flows can be automatically triggered by specific events.
     */
    public boolean isEnabled;

    /**
     * Tracking how the user modified the flow, to support redo operation.
     */
    public Stack<NodeEditPath> redoPath;

    /**
     * Tracking how the user modified the flow, to support undo operation.
     */
    public Stack<NodeEditPath> undoPath;

    public FlowManager(LogicFlow flow) {
        this.flow = flow;
        this.isEnabled = false;
        this.redoPath = new Stack<>();
        this.undoPath = new Stack<>();
    }

    public FlowManager(String name, String eventNode, String eventNodeName) {
        this.flow = new LogicFlow(name);
        FlowNode startNode = NodeRegistry.createNode(eventNode, flow.generateId(), eventNodeName);
        this.flow.addNode(startNode);
        this.flow.startNodeId = startNode.getId();
        this.isEnabled = false;
        this.redoPath = new Stack<>();
        this.undoPath = new Stack<>();
    }

    public void createNode(String type, String name) {
        this.redoPath.clear();
        FlowNode node = NodeRegistry.createNode(type, flow.generateId(), name);
        this.flow.addNode(node);
        this.undoPath.add(new NodeEditPath(f -> f.addNode(node), f -> f.removeNode(node.getId())));
    }

    public void removeNode(String name) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        if (node != null) {
            this.flow.removeNode(node.getId());
            this.undoPath.add(new NodeEditPath(f -> f.removeNode(node.getId()), f -> f.addNode(node)));
        }
    }

    public void renameNode(String oldName, String newName) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(oldName);
        if (node != null) {
            node.name = newName;
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.name = newName;
                    }
                }, 
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.name = oldName;
                    }
                }
            ));
        }
    }

    public void setConstInput(String name, int index, Object value) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        if (node != null) {
            DataReference oldValue = node.getInput(index);
            node.setInput(index, DataReference.createConstantReference(value));
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setInput(index, DataReference.createConstantReference(value));
                    }
                },
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setInput(index, oldValue);
                    }
                }
            ));
        }
    }

    public void setReferenceInput(String name, int index, String refNode, int refIndex) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        FlowNode ref = this.flow.getNodeByName(refNode);
        if (node != null && ref != null) {
            DataReference oldValue = node.getInput(index);
            node.setInput(index, DataReference.createNodeOutputReference(ref.getId(), refIndex));
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    FlowNode r = f.getNode(ref.getId());
                    if (n != null && r != null) {
                        n.setInput(index, DataReference.createNodeOutputReference(r.getId(), refIndex));
                    }
                },
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setInput(index, oldValue);
                    }
                }
            ));
        }
    }

    public void disconnectInput(String name, int index) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        if (node != null) {
            DataReference oldValue = node.getInput(index);
            node.setInput(index, DataReference.createEmptyReference());
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setInput(index, DataReference.createEmptyReference());
                    }
                },
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setInput(index, oldValue);
                    }
                }
            ));
        }
    }

    public void setNextNode(String name, int index, String next) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        FlowNode nextNode = this.flow.getNodeByName(next);
        if (node != null && nextNode != null) {
            long oldNextId = node.nextNodeIds.get(index);
            node.setNextNodeId(index, nextNode.getId());
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    FlowNode nn = f.getNode(nextNode.getId());
                    if (n != null && nn != null) {
                        n.setNextNodeId(index, nn.getId());
                    }
                },
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setNextNodeId(index, oldNextId);
                    }
                }
            ));
        }
    }

    public void disconnectNextNode(String name, int index) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        if (node != null) {
            long oldNextId = node.nextNodeIds.get(index);
            node.setNextNodeId(index, -1L);
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setNextNodeId(index, -1L);
                    }
                },
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setNextNodeId(index, oldNextId);
                    }
                }
            ));
        }
    }

    public void redo() {
        if (!this.redoPath.isEmpty()) {
            NodeEditPath edit = this.redoPath.pop();
            edit.redo(this.flow);
            this.undoPath.push(edit);
        }
    }

    public void undo() {
        if (!this.undoPath.isEmpty()) {
            NodeEditPath edit = this.undoPath.pop();
            edit.undo(this.flow);
            this.redoPath.push(edit);
        }
    }

    @Nullable
    public LogicException execute(@Nonnull ServerData serverData, @Nullable List<Object> startNodeOutputs, @Nullable Map<String, Object> initialVariables) {
        ExecutionContext executionContext = new ExecutionContext(this.flow, serverData.server);
        executionContext.execute(Util.serverConfig.getMaxFlowLength(), startNodeOutputs, initialVariables);
        serverData.executeHistory.add(executionContext);
        int historyLimit = Util.serverConfig.getKeepFlowHistoryNumber();
        while (serverData.executeHistory.size() > historyLimit) {
            serverData.executeHistory.remove(0);
        }
        return executionContext.getException();
    }
}
