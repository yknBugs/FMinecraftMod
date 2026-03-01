/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;

import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.PlayerMessageType;
import com.ykn.fmod.server.base.util.ServerMessageType;
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
        super(id, name, 4, 0, 1, "BroadcastMessageNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.bcmessage.title.name", "fmod.node.bcmessage.title.feat")
            .input("fmod.node.bcmessage.input.player.name", "fmod.node.bcmessage.input.player.feat", "fmod.node.bcmessage.input.player.type")
            .input("fmod.node.bcmessage.input.receiver.name", "fmod.node.bcmessage.input.receiver.feat", "fmod.node.bcmessage.input.receiver.type")
            .input("fmod.node.bcmessage.input.type.name", "fmod.node.bcmessage.input.type.feat", "fmod.node.bcmessage.input.type.type")
            .input("fmod.node.bcmessage.input.message.name", "fmod.node.bcmessage.input.message.feat", "fmod.node.bcmessage.input.message.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        ServerPlayer player = parsePlayer(resolvedInputs.get(0));
        PlayerMessageType.Receiver receiver = parseReceiver(resolvedInputs.get(1));
        MessageType.Location messageType = parseMessageType(resolvedInputs.get(2));
        Component message = parseMessage(resolvedInputs.get(3));
        PlayerMessageType type = PlayerMessageType.of(messageType, receiver);
        
        if (player == null) {
            // When player is null, handle special cases
            if (receiver == PlayerMessageType.Receiver.ALL || receiver == PlayerMessageType.Receiver.OP || receiver == PlayerMessageType.Receiver.NONE) {
                ServerMessageType serverMessageType = ServerMessageType.fromPlayerMessageType(type);
                serverMessageType.postMessage(context.getServer(), message);
            } else {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.bcmessage.error.invalid", receiver), null);
            }
        } else {
            // When player is not null, use postMessage
            type.postMessage(player, message);
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

    private PlayerMessageType.Receiver parseReceiver(Object receiverObj) throws LogicException {
        if (receiverObj == null) {
            return PlayerMessageType.Receiver.ALL;
        } else if (receiverObj instanceof PlayerMessageType.Receiver) {
            PlayerMessageType.Receiver receiver = (PlayerMessageType.Receiver) receiverObj;
            return receiver;
        } else {
            String receiverStr = TypeAdaptor.parse(receiverObj).asString().strip();
            if ("all".equalsIgnoreCase(receiverStr)) {
                return PlayerMessageType.Receiver.ALL;
            } else if ("op".equalsIgnoreCase(receiverStr)) {
                return PlayerMessageType.Receiver.OP;
            } else if ("none".equalsIgnoreCase(receiverStr)) {
                return PlayerMessageType.Receiver.NONE;
            } else if ("team".equalsIgnoreCase(receiverStr)) {
                return PlayerMessageType.Receiver.TEAM;
            } else if ("self".equalsIgnoreCase(receiverStr)) {
                return PlayerMessageType.Receiver.SELF;
            } else if ("teamop".equalsIgnoreCase(receiverStr)) {
                return PlayerMessageType.Receiver.TEAMOP;
            } else if ("selfop".equalsIgnoreCase(receiverStr)) {
                return PlayerMessageType.Receiver.SELFOP;
            } else {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(1), this.metadata.inputDataTypes.get(1)), null);
            }
        }
    }

    private MessageType.Location parseMessageType(Object typeObj) throws LogicException {
        if (typeObj == null) {
            return MessageType.Location.CHAT;
        } else if (typeObj instanceof MessageType.Location) {
            return (MessageType.Location) typeObj;
        } else {
            String typeStr = TypeAdaptor.parse(typeObj).asString().strip();
            if ("actionbar".equalsIgnoreCase(typeStr)) {
                return MessageType.Location.ACTIONBAR;
            } else if ("chat".equalsIgnoreCase(typeStr)) {
                return MessageType.Location.CHAT;
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
