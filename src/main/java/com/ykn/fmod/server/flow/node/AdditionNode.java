package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.text.Text;

public class AdditionNode extends FlowNode {

    public AdditionNode(long id, String name) {
        super(id, name, 2, 1, 1);
        this.type = "Addition Node";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Text displayName = Util.parseTranslateableText("fmod.node.addition.title.name");
        Text description = Util.parseTranslateableText("fmod.node.addition.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        inputNames.add(Util.parseTranslateableText("fmod.node.addition.input.a.name"));
        inputDescriptions.add(Util.parseTranslateableText("fmod.node.addition.input.a.feat"));
        inputNames.add(Util.parseTranslateableText("fmod.node.addition.input.b.name"));
        inputDescriptions.add(Util.parseTranslateableText("fmod.node.addition.input.b.feat"));
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        outputNames.add(Util.parseTranslateableText("fmod.node.addition.output.sum.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.addition.output.sum.feat"));
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslateableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslateableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, outputNames, outputDescriptions, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        try {
            double a = (double) resolvedInputs.get(0);
            double b = (double) resolvedInputs.get(1);
            double sum = a + b;
            status.setOutput(0, sum);
        } catch (ClassCastException e) {
            throw new LogicException(e, Util.parseTranslateableText("fmod.flow.error.classcast", this.name), null);
        } catch (NullPointerException e) {
            throw new LogicException(e, Util.parseTranslateableText("fmod.flow.error.missinginput", this.name), null);
        } catch (IndexOutOfBoundsException e) {
            throw new LogicException(e, Util.parseTranslateableText("fmod.flow.error.inputindex", this.name), null);
        } catch (Exception e) {
            throw new LogicException(e, Util.parseTranslateableText("fmod.flow.error.unknown", this.name), null);
        }
    }
}
