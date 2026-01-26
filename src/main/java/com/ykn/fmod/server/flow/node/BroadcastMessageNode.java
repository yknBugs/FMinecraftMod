/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * A flow node that broadcasts a message to all players on the server.
 * Inputs:
 * 1. MessageLocation - The location to send the message (chat or actionbar).
 * 2. Text - The message to broadcast.
 * Outputs: None
 * Branches: 1 (Next node)
 */
public class BroadcastMessageNode extends FlowNode {

    public BroadcastMessageNode(long id, String name) {
        super(id, name, 2, 0, 1);
        this.type = "BroadcastMessageNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Component displayName = Util.parseTranslatableText("fmod.node.bcmessage.title.name");
        Component description = Util.parseTranslatableText("fmod.node.bcmessage.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.bcmessage.input.type.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.bcmessage.input.type.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.bcmessage.input.type.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.bcmessage.input.message.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.bcmessage.input.message.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.bcmessage.input.message.type"));
        List<Component> outputNames = new ArrayList<>();
        List<Component> outputDescriptions = new ArrayList<>();
        List<Component> outputDataTypes = new ArrayList<>();
        List<Component> branchNames = new ArrayList<>();
        List<Component> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        MinecraftServer server = context.getServer();
        MessageLocation messageType = parseMessageType(resolvedInputs.get(0));
        Component message = parseMessage(resolvedInputs.get(1));
        Util.broadcastMessage(server, messageType, message);
    }

    private MessageLocation parseMessageType(Object typeObj) throws LogicException {
        if (typeObj == null) {
            return MessageLocation.CHAT;
        } else if (typeObj instanceof MessageLocation) {
            return (MessageLocation) typeObj;
        } else {
            String typeStr = TypeAdaptor.parse(typeObj).asString().trim();
            if ("actionbar".equalsIgnoreCase(typeStr)) {
                return MessageLocation.ACTIONBAR;
            } else if ("chat".equalsIgnoreCase(typeStr)) {
                return MessageLocation.CHAT;
            } else {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.bcmessage.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
            }
        }
    }

    private Component parseMessage(Object messageObj) throws LogicException {
        if (messageObj == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.bcmessage.error.inputnull", this.name, this.metadata.inputNames.get(1)), null);
        } else if (messageObj instanceof Component) {
            return (Component) messageObj;
        } else {
            String messageStr = TypeAdaptor.parse(messageObj).asString();
            return Component.literal(messageStr);
        }
    }
}
