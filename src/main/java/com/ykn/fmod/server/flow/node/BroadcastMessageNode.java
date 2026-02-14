/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.MessageReceiver;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * A flow node that sends a message to players based on receiver settings.
 * Inputs:
 * 1. Player - The main receiver (can be null).
 * 2. MessageReceiver - The receiver method.
 * 3. MessageLocation - The location to send the message (chat or actionbar).
 * 4. Text - The message to send.
 * Outputs: None
 * Branches: 1 (Next node)
 */
public class BroadcastMessageNode extends FlowNode {

    public BroadcastMessageNode(long id, String name) {
        super(id, name, 4, 0, 1);
        this.type = "BroadcastMessageNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Component displayName = Util.parseTranslatableText("fmod.node.bcmessage.title.name");
        Component description = Util.parseTranslatableText("fmod.node.bcmessage.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.bcmessage.input.player.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.bcmessage.input.player.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.bcmessage.input.player.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.bcmessage.input.receiver.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.bcmessage.input.receiver.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.bcmessage.input.receiver.type"));
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
        ServerPlayer player = parsePlayer(resolvedInputs.get(0));
        MessageReceiver receiver = parseReceiver(resolvedInputs.get(1));
        MessageLocation messageType = parseMessageType(resolvedInputs.get(2));
        Component message = parseMessage(resolvedInputs.get(3));
        
        if (player == null) {
            // When player is null, handle special cases
            if (receiver == MessageReceiver.ALL || receiver == MessageReceiver.OP || receiver == MessageReceiver.NONE) {
                Util.postMessage(context.getServer(), receiver, messageType, message);
            } else {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.bcmessage.error.invalid", receiver), null);
            }
        } else {
            // When player is not null, use postMessage
            Util.postMessage(player, receiver, messageType, message);
        }
    }

    private ServerPlayer parsePlayer(Object playerObj) throws LogicException {
        if (playerObj == null) {
            return null;
        } else if (playerObj instanceof ServerPlayer) {
            return (ServerPlayer) playerObj;
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
        }
    }

    private MessageReceiver parseReceiver(Object receiverObj) throws LogicException {
        if (receiverObj == null) {
            return MessageReceiver.ALL;
        } else if (receiverObj instanceof MessageReceiver) {
            MessageReceiver receiver = (MessageReceiver) receiverObj;
            return receiver;
        } else {
            String receiverStr = TypeAdaptor.parse(receiverObj).asString().strip();
            if ("all".equalsIgnoreCase(receiverStr)) {
                return MessageReceiver.ALL;
            } else if ("op".equalsIgnoreCase(receiverStr)) {
                return MessageReceiver.OP;
            } else if ("none".equalsIgnoreCase(receiverStr)) {
                return MessageReceiver.NONE;
            } else if ("team".equalsIgnoreCase(receiverStr)) {
                return MessageReceiver.TEAM;
            } else if ("self".equalsIgnoreCase(receiverStr)) {
                return MessageReceiver.SELF;
            } else if ("teamop".equalsIgnoreCase(receiverStr)) {
                return MessageReceiver.TEAMOP;
            } else if ("selfop".equalsIgnoreCase(receiverStr)) {
                return MessageReceiver.SELFOP;
            } else {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(1), this.metadata.inputDataTypes.get(1)), null);
            }
        }
    }

    private MessageLocation parseMessageType(Object typeObj) throws LogicException {
        if (typeObj == null) {
            return MessageLocation.CHAT;
        } else if (typeObj instanceof MessageLocation) {
            return (MessageLocation) typeObj;
        } else {
            String typeStr = TypeAdaptor.parse(typeObj).asString().strip();
            if ("actionbar".equalsIgnoreCase(typeStr)) {
                return MessageLocation.ACTIONBAR;
            } else if ("chat".equalsIgnoreCase(typeStr)) {
                return MessageLocation.CHAT;
            } else {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(2), this.metadata.inputDataTypes.get(2)), null);
            }
        }
    }

    private Component parseMessage(Object messageObj) throws LogicException {
        if (messageObj == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(3)), null);
        } else if (messageObj instanceof Component) {
            return (Component) messageObj;
        } else {
            String messageStr = TypeAdaptor.parse(messageObj).asString();
            return Component.literal(messageStr);
        }
    }
}
