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

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

/**
 * A flow node that broadcasts a message to all players on the server.
 * Inputs:
 * 1. MinecraftServer - The server to broadcast the message on.
 * 2. MessageLocation - The location to send the message (chat or actionbar).
 * 3. Text - The message to broadcast.
 * Outputs: None
 * Branches: 1 (Next node)
 */
public class BroadcastMessageNode extends FlowNode {

    public BroadcastMessageNode(long id, String name) {
        super(id, name, 3, 0, 1);
        this.type = "BroadcastMessageNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Text displayName = Util.parseTranslateableText("fmod.node.bcmessage.title.name");
        Text description = Util.parseTranslateableText("fmod.node.bcmessage.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslateableText("fmod.node.bcmessage.input.server.name"));
        inputDescriptions.add(Util.parseTranslateableText("fmod.node.bcmessage.input.server.feat"));
        inputDataTypes.add(Util.parseTranslateableText("fmod.node.bcmessage.input.server.type"));
        inputNames.add(Util.parseTranslateableText("fmod.node.bcmessage.input.type.name"));
        inputDescriptions.add(Util.parseTranslateableText("fmod.node.bcmessage.input.type.feat"));
        inputDataTypes.add(Util.parseTranslateableText("fmod.node.bcmessage.input.type.type"));
        inputNames.add(Util.parseTranslateableText("fmod.node.bcmessage.input.message.name"));
        inputDescriptions.add(Util.parseTranslateableText("fmod.node.bcmessage.input.message.feat"));
        inputDataTypes.add(Util.parseTranslateableText("fmod.node.bcmessage.input.message.type"));
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslateableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslateableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        MinecraftServer server = parseServer(resolvedInputs.get(0));
        MessageLocation messageType = parseMessageType(resolvedInputs.get(1));
        Text message = parseMessage(resolvedInputs.get(2));
        Util.broadcastMessage(server, messageType, message);
    }

    private MinecraftServer parseServer(Object serverObj) throws LogicException {
        if (serverObj == null) {
            throw new LogicException(null, Util.parseTranslateableText("fmod.node.bcmessage.error.inputnull", this.name, this.metadata.inputNames.get(0)), null);
        } else if (!(serverObj instanceof MinecraftServer)) {
            throw new LogicException(null, Util.parseTranslateableText("fmod.node.bcmessage.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
        }
        return (MinecraftServer) serverObj;
    }

    private MessageLocation parseMessageType(Object typeObj) throws LogicException {
        if (typeObj == null) {
            return MessageLocation.CHAT;
        } else if (typeObj instanceof MessageLocation) {
            return (MessageLocation) typeObj;
        } else {
            String typeStr = TypeAdaptor.parseStringLikeObject(typeObj);
            if ("actionbar".equalsIgnoreCase(typeStr)) {
                return MessageLocation.ACTIONBAR;
            } else if ("chat".equalsIgnoreCase(typeStr)) {
                return MessageLocation.CHAT;
            } else {
                throw new LogicException(null, Util.parseTranslateableText("fmod.node.bcmessage.error.classcast", this.name, this.metadata.inputNames.get(1), this.metadata.inputDataTypes.get(1)), null);
            }
        }
    }

    private Text parseMessage(Object messageObj) {
        if (messageObj == null) {
            return Text.empty(); 
        } else if (messageObj instanceof Text) {
            return (Text) messageObj;
        } else {
            String messageStr = TypeAdaptor.parseStringLikeObject(messageObj);
            return Text.literal(messageStr);
        }
    }
}
