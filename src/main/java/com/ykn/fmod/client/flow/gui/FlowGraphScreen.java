/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.flow.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ykn.fmod.client.base.gui.OptionPickerScreen;
import com.ykn.fmod.server.flow.logic.DataReference;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicFlow;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.flow.tool.FlowSerializer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The node-graph editor for one flow being edited via a detached deep copy
 * ({@link FlowEditorBridge.EditSession}) - see {@link FlowEditorBridge} for why edits never touch
 * the live, registered flow until the user explicitly commits.
 *
 * <p>Every input/output/branch of every node is a real button positioned directly on the canvas
 * (see {@link #rebuildNodeButtons()}), each with a tooltip describing its data type/purpose, plus a
 * plain-text readout of its current value/target so the graph is scannable without clicking
 * anything. A single contextual "editor menu" bar along the bottom shows only what's relevant to
 * whatever was last clicked (see {@link Mode}). There is no Cancel button anywhere - clicking empty
 * canvas always resets to the idle/add-node mode, which serves as the universal cancel.</p>
 *
 * <p>All edits mutate {@code session.manager} (an orphan {@link FlowManager} nobody else
 * references) synchronously and locally - no server round trip until {@link #save()}/{@link #update()}.
 * Node positions are session-only and reset to a fresh default layout every time a flow is opened.</p>
 */
public class FlowGraphScreen extends Screen {

    private static final int NODE_WIDTH = 216;
    private static final int NODE_INPUT_WIDTH = 50;
    private static final int NODE_INPUT_VALUE_WIDTH = 100;
    private static final int NODE_OUTPUT_WIDTH = 50;
    private static final int TITLE_HEIGHT = 18;
    private static final int ROW_HEIGHT = 16;
    private static final int PADDING = 4;
    private static final int COL_SPACING = 40;
    private static final int ROW_SPACING = 24;
    private static final int MARGIN = 20;
    private static final int SCROLLBAR_SIZE = 6;

    private enum Mode { IDLE_ADD, NODE_SELECTED, RENAMING, BRANCH_PICK, INPUT_PICK, OUTPUT_PICK, EDIT_CONST, FEEDBACK_MESSAGE }

    private enum ScrollDrag { NONE, VERTICAL, HORIZONTAL }

    private final FlowEditorScreen parent;
    private final FlowEditorBridge.EditSession session;
    private final Map<Long, int[]> positions = new HashMap<>();
    private final Map<Long, NodeBox> nodeBoxes = new HashMap<>();
    private final List<AbstractWidget> menuWidgets = new ArrayList<>();

    private int canvasX;
    private int canvasY;
    private int canvasWidth;
    private int canvasHeight;
    private int scrollX;
    private int scrollY;
    private Long draggingId;
    private int dragOffsetX;
    private int dragOffsetY;
    private ScrollDrag scrollDragging = ScrollDrag.NONE;
    private int scrollDragStartMouse;
    private int scrollDragStartValue;

    private Mode mode = Mode.IDLE_ADD;
    private Long modeNodeId;
    private int modeIndex;
    private String pendingAddName = "";
    private String pendingAddType;
    private String pendingConstInitial = "";

    private Component statusMessage = Component.empty();
    private Button undoButton;
    private Button redoButton;

    public FlowGraphScreen(FlowEditorScreen parent, FlowEditorBridge.EditSession session) {
        super(Component.literal(session.manager.getFlow().getName()));
        this.parent = parent;
        this.session = session;
        computeDefaultLayout();
        for (FlowEditorBridge.NodeTypeInfo info : FlowEditorBridge.nodeTypeCatalog()) {
            if (!info.event) {
                this.pendingAddType = info.type;
                break;
            }
        }
    }

    @Override
    protected void init() {
        this.canvasX = 10;
        this.canvasY = 34;
        this.canvasWidth = this.width - 20;
        this.canvasHeight = this.height - 68;

        int bw = 60;
        int gap = 5;
        int bx = this.width - (bw * 5 + gap * 4) - 10;
        int ty = 8;
        this.undoButton = Button.builder(Component.translatable("fmod.misc.undo"), b -> doUndo()).pos(bx, ty).size(bw, 20).build();
        this.addRenderableWidget(this.undoButton);
        bx += bw + gap;
        this.redoButton = Button.builder(Component.translatable("fmod.misc.redo"), b -> doRedo()).pos(bx, ty).size(bw, 20).build();
        this.addRenderableWidget(this.redoButton);
        bx += bw + gap;
        this.addRenderableWidget(Button.builder(Component.translatable("fmod.misc.update"), b -> update()).pos(bx, ty).size(bw, 20).build());
        bx += bw + gap;
        this.addRenderableWidget(Button.builder(Component.translatable("fmod.misc.save"), b -> save()).pos(bx, ty).size(bw, 20).build());
        bx += bw + gap;
        this.addRenderableWidget(Button.builder(Component.translatable("fmod.misc.exit"), b -> exit()).pos(bx, ty).size(bw, 20).build());

        refreshAll();
    }

    // ------------------------------------------------------------------
    // Node box layout: title row, then one row per branch, then input/output
    // rows aligned by index (input i on the left lines up with output i on the right)
    // ------------------------------------------------------------------

    private static int bodyRows(FlowNode node) {
        NodeMetadata meta = node.getMetadata();
        return meta.branchNumber + Math.max(meta.inputNumber, meta.outputNumber);
    }

    private static int nodeBoxHeight(FlowNode node) {
        return PADDING + TITLE_HEIGHT + bodyRows(node) * ROW_HEIGHT + PADDING;
    }

    /** Nodes in {@link LogicFlow#getSortedNodes()} order, starting a new row whenever the traversal
     * doesn't continue directly from the previous node's branches; row height adapts to content. */
    private void computeDefaultLayout() {
        LogicFlow flow = this.session.manager.getFlow();
        List<FlowNode> order = flow.getSortedNodes();
        Map<Long, int[]> rowCol = new HashMap<>();
        int row = 0;
        int col = 0;
        FlowNode prev = null;
        for (FlowNode node : order) {
            if (prev != null) {
                if (prev.getNextNodeIds().contains(node.getId())) {
                    col++;
                } else {
                    row++;
                    col = 0;
                }
            }
            rowCol.put(node.getId(), new int[]{row, col});
            prev = node;
        }
        Map<Integer, Integer> rowHeight = new HashMap<>();
        int maxRow = 0;
        for (FlowNode node : order) {
            int[] rc = rowCol.get(node.getId());
            rowHeight.merge(rc[0], nodeBoxHeight(node), Math::max);
            maxRow = Math.max(maxRow, rc[0]);
        }
        Map<Integer, Integer> rowY = new HashMap<>();
        int cumulative = MARGIN;
        for (int r = 0; r <= maxRow; r++) {
            rowY.put(r, cumulative);
            cumulative += rowHeight.getOrDefault(r, 60) + ROW_SPACING;
        }
        this.positions.clear();
        for (FlowNode node : order) {
            int[] rc = rowCol.get(node.getId());
            this.positions.put(node.getId(), new int[]{MARGIN + rc[1] * (NODE_WIDTH + COL_SPACING), rowY.get(rc[0])});
        }
    }

    /** Assigns a default position to any node missing one (e.g. just created) without disturbing dragged positions. */
    private void ensurePositions() {
        LogicFlow flow = this.session.manager.getFlow();
        this.positions.keySet().removeIf(id -> flow.getNode(id) == null);
        int maxBottom = MARGIN;
        for (FlowNode node : flow.getNodes()) {
            int[] pos = this.positions.get(node.getId());
            if (pos != null) {
                maxBottom = Math.max(maxBottom, pos[1] + nodeBoxHeight(node));
            }
        }
        int col = 0;
        for (FlowNode node : flow.getNodes()) {
            if (!this.positions.containsKey(node.getId())) {
                this.positions.put(node.getId(), new int[]{MARGIN + col * (NODE_WIDTH + COL_SPACING), maxBottom + ROW_SPACING});
                col++;
            }
        }
    }

    // ------------------------------------------------------------------
    // Node port buttons + inline value readouts (rebuilt whenever the flow's structure changes)
    // ------------------------------------------------------------------

    private static final class NodeBox {
        final long nodeId;
        final int width;
        final int height;
        final Button nameButton;
        final int[] nameOffset;
        final Component typeText;
        final int[] typeTextOffset;
        final List<Button> branchButtons;
        final List<int[]> branchOffsets;
        final List<Component> branchValueTexts;
        final List<int[]> branchValueOffsets;
        final List<Button> inputButtons;
        final List<int[]> inputOffsets;
        final List<Component> inputValueTexts;
        final List<int[]> inputValueOffsets;
        final List<Button> outputButtons;
        final List<int[]> outputOffsets;

        NodeBox(long nodeId, int width, int height, Button nameButton, int[] nameOffset, Component typeText, int[] typeTextOffset,
                List<Button> branchButtons, List<int[]> branchOffsets, List<Component> branchValueTexts, List<int[]> branchValueOffsets,
                List<Button> inputButtons, List<int[]> inputOffsets, List<Component> inputValueTexts, List<int[]> inputValueOffsets,
                List<Button> outputButtons, List<int[]> outputOffsets) {
            this.nodeId = nodeId;
            this.width = width;
            this.height = height;
            this.nameButton = nameButton;
            this.nameOffset = nameOffset;
            this.typeText = typeText;
            this.typeTextOffset = typeTextOffset;
            this.branchButtons = branchButtons;
            this.branchOffsets = branchOffsets;
            this.branchValueTexts = branchValueTexts;
            this.branchValueOffsets = branchValueOffsets;
            this.inputButtons = inputButtons;
            this.inputOffsets = inputOffsets;
            this.inputValueTexts = inputValueTexts;
            this.inputValueOffsets = inputValueOffsets;
            this.outputButtons = outputButtons;
            this.outputOffsets = outputOffsets;
        }

        List<Button> allButtons() {
            List<Button> all = new ArrayList<>();
            all.add(this.nameButton);
            all.addAll(this.branchButtons);
            all.addAll(this.inputButtons);
            all.addAll(this.outputButtons);
            return all;
        }
    }

    private static Component combineTooltip(Component type, Component description) {
        return Component.empty().append(type).append("\n").append(description);
    }

    private NodeBox buildNodeBox(FlowNode node, LogicFlow flow) {
        NodeMetadata meta = node.getMetadata();
        long id = node.getId();
        int bodyTop = PADDING + TITLE_HEIGHT;
        int height = nodeBoxHeight(node);
        int rightWidth = NODE_OUTPUT_WIDTH;
        // Flush with the node box's right edge, same as the branch button below - otherwise
        // shrinking NODE_OUTPUT_WIDTH leaves a dead gap and data wires stop short of the border.
        int rightX = NODE_WIDTH - PADDING - rightWidth;
        int nameButtonWidth = 100;

        Button nameButton = Button.builder(Component.literal(node.getName()), b -> onNameClicked(id)).size(nameButtonWidth, 14).build();
        nameButton.setTooltip(Tooltip.create(meta.description));
        int[] nameOffset = {PADDING, PADDING, nameButtonWidth, 14};
        Component typeText = Component.literal(this.font.plainSubstrByWidth(node.getType(), NODE_WIDTH - PADDING * 2 - nameButtonWidth - 6));
        int[] typeTextOffset = {PADDING + nameButtonWidth + 6, PADDING + 3};

        List<Button> branchButtons = new ArrayList<>();
        List<int[]> branchOffsets = new ArrayList<>();
        List<Component> branchValueTexts = new ArrayList<>();
        List<int[]> branchValueOffsets = new ArrayList<>();
        // The branch ("next node") button is right-aligned, flush with the node box's right edge
        // (matching where outputs live) - its value text fills the space to its left instead.
        int branchButtonWidth = 60;
        int branchButtonX = NODE_WIDTH - PADDING - branchButtonWidth;
        int branchValueX = PADDING;
        int branchValueWidth = branchButtonX - PADDING - 4;
        for (int i = 0; i < meta.branchNumber; i++) {
            int index = i;
            Button branchButton = Button.builder(meta.branchNames.get(i), b -> onBranchClicked(id, index)).size(branchButtonWidth, ROW_HEIGHT - 2).build();
            branchButton.setTooltip(Tooltip.create(meta.branchDescriptions.get(i)));
            branchButtons.add(branchButton);
            int y = bodyTop + i * ROW_HEIGHT;
            branchOffsets.add(new int[]{branchButtonX, y, branchButtonWidth, ROW_HEIGHT - 2});
            long targetId = node.getNextNodeIds().get(i);
            FlowNode target = flow.getNode(targetId);
            String targetText = target == null ? Component.translatable("fmod.misc.null").getString() : target.getName();
            branchValueTexts.add(Component.literal(this.font.plainSubstrByWidth(targetText, branchValueWidth)));
            branchValueOffsets.add(new int[]{branchValueX, y + 3});
        }

        int rowOffset = meta.branchNumber;
        List<Button> inputButtons = new ArrayList<>();
        List<int[]> inputOffsets = new ArrayList<>();
        List<Component> inputValueTexts = new ArrayList<>();
        List<int[]> inputValueOffsets = new ArrayList<>();
        int inputButtonWidth = NODE_INPUT_WIDTH;
        int inputValueX = PADDING + inputButtonWidth + 4;
        int inputValueWidth = NODE_INPUT_VALUE_WIDTH;
        for (int i = 0; i < meta.inputNumber; i++) {
            int index = i;
            Button inputButton = Button.builder(meta.inputNames.get(i), b -> onInputClicked(id, index)).size(inputButtonWidth, ROW_HEIGHT - 2).build();
            inputButton.setTooltip(Tooltip.create(combineTooltip(meta.inputDataTypes.get(i), meta.inputDescriptions.get(i))));
            inputButtons.add(inputButton);
            int y = bodyTop + (rowOffset + i) * ROW_HEIGHT;
            inputOffsets.add(new int[]{PADDING, y, inputButtonWidth, ROW_HEIGHT - 2});
            DataReference ref = node.getInput(i);
            String valueText;
            if (ref.getType() == DataReference.ReferenceType.NODE_OUTPUT) {
                FlowNode source = flow.getNode(ref.getReferenceId());
                String outputName = "?";
                if (source != null && ref.getReferenceIndex() >= 0 && ref.getReferenceIndex() < source.getMetadata().outputNumber) {
                    outputName = source.getMetadata().outputNames.get(ref.getReferenceIndex()).getString();
                }
                valueText = (source == null ? "?" : source.getName()) + " " + outputName;
            } else if (ref.getValue() == null) {
                valueText = Component.translatable("fmod.misc.null").getString();
            } else {
                valueText = String.valueOf(ref.getValue());
            }
            inputValueTexts.add(Component.literal(this.font.plainSubstrByWidth(valueText, inputValueWidth)));
            inputValueOffsets.add(new int[]{inputValueX, y + 3});
        }

        List<Button> outputButtons = new ArrayList<>();
        List<int[]> outputOffsets = new ArrayList<>();
        for (int i = 0; i < meta.outputNumber; i++) {
            int index = i;
            Button outputButton = Button.builder(meta.outputNames.get(i), b -> onOutputClicked(id, index)).size(rightWidth, ROW_HEIGHT - 2).build();
            outputButton.setTooltip(Tooltip.create(combineTooltip(meta.outputDataTypes.get(i), meta.outputDescriptions.get(i))));
            outputButtons.add(outputButton);
            int y = bodyTop + (rowOffset + i) * ROW_HEIGHT;
            outputOffsets.add(new int[]{rightX, y, rightWidth, ROW_HEIGHT - 2});
        }

        return new NodeBox(id, NODE_WIDTH, height, nameButton, nameOffset, typeText, typeTextOffset,
            branchButtons, branchOffsets, branchValueTexts, branchValueOffsets,
            inputButtons, inputOffsets, inputValueTexts, inputValueOffsets,
            outputButtons, outputOffsets);
    }

    private void rebuildNodeButtons() {
        for (NodeBox box : this.nodeBoxes.values()) {
            box.allButtons().forEach(this::removeWidget);
        }
        this.nodeBoxes.clear();
        LogicFlow flow = this.session.manager.getFlow();
        for (FlowNode node : flow.getNodes()) {
            NodeBox box = buildNodeBox(node, flow);
            this.nodeBoxes.put(node.getId(), box);
            box.allButtons().forEach(this::addWidget);
        }
    }

    private static void placeButton(Button button, int[] offset, int baseX, int baseY) {
        button.setX(baseX + offset[0]);
        button.setY(baseY + offset[1]);
    }

    private void updateNodeButtonPositions() {
        for (NodeBox box : this.nodeBoxes.values()) {
            int[] pos = this.positions.get(box.nodeId);
            if (pos == null) {
                continue;
            }
            int baseX = this.canvasX + pos[0] - this.scrollX;
            int baseY = this.canvasY + pos[1] - this.scrollY;
            boolean visible = baseX + box.width >= this.canvasX && baseX <= this.canvasX + viewWidth()
                && baseY + box.height >= this.canvasY && baseY <= this.canvasY + viewHeight();
            for (Button button : box.allButtons()) {
                button.visible = visible;
            }
            placeButton(box.nameButton, box.nameOffset, baseX, baseY);
            for (int i = 0; i < box.branchButtons.size(); i++) {
                placeButton(box.branchButtons.get(i), box.branchOffsets.get(i), baseX, baseY);
            }
            for (int i = 0; i < box.inputButtons.size(); i++) {
                placeButton(box.inputButtons.get(i), box.inputOffsets.get(i), baseX, baseY);
            }
            for (int i = 0; i < box.outputButtons.size(); i++) {
                placeButton(box.outputButtons.get(i), box.outputOffsets.get(i), baseX, baseY);
            }
        }
    }

    // ------------------------------------------------------------------
    // Port click handling - mode-aware: a click either completes a pending
    // pick (if it matches what's being picked) or starts a new mode.
    // ------------------------------------------------------------------

    private FlowNode getNode(Long id) {
        return id == null ? null : this.session.manager.getFlow().getNode(id);
    }

    private void onNameClicked(long nodeId) {
        if (this.mode == Mode.BRANCH_PICK) {
            FlowNode source = getNode(this.modeNodeId);
            FlowNode target = getNode(nodeId);
            long sourceId = this.modeNodeId;
            int index = this.modeIndex;
            if (source != null && target != null) {
                mutateLocal(() -> this.session.manager.setNextNode(source.getName(), index, target.getName()));
            }
            enterNodeSelected(sourceId);
        } else {
            enterRenaming(nodeId);
        }
    }

    private void onInputClicked(long nodeId, int index) {
        if (this.mode == Mode.OUTPUT_PICK) {
            if (this.modeNodeId != null && this.modeNodeId == nodeId) {
                // A node's own output can never satisfy its own input: inputs resolve before
                // onExecute() sets outputs, so this would always throw at runtime. Reject with
                // feedback rather than silently doing nothing (mode/menu deliberately unchanged).
                this.statusMessage = Component.translatable("fmod.flowgui.graph.error.selfreference").withStyle(ChatFormatting.RED);
                this.mode = Mode.FEEDBACK_MESSAGE;
                rebuildMenu();
                return;
            }
            FlowNode source = getNode(this.modeNodeId);
            FlowNode target = getNode(nodeId);
            long sourceId = this.modeNodeId;
            int outIndex = this.modeIndex;
            if (source != null && target != null) {
                mutateLocal(() -> this.session.manager.setReferenceInput(target.getName(), index, source.getName(), outIndex));
            }
            enterNodeSelected(sourceId);
        } else {
            enterInputPick(nodeId, index);
        }
    }

    private void onBranchClicked(long nodeId, int index) {
        enterBranchPick(nodeId, index);
    }

    private void onOutputClicked(long nodeId, int index) {
        if (this.mode == Mode.INPUT_PICK) {
            if (this.modeNodeId != null && this.modeNodeId == nodeId) {
                // Same restriction as above, entered from the output side instead.
                this.statusMessage = Component.translatable("fmod.flowgui.graph.error.selfreference").withStyle(ChatFormatting.RED);
                this.mode = Mode.FEEDBACK_MESSAGE;
                rebuildMenu();
                return;
            }
            FlowNode target = getNode(this.modeNodeId);
            FlowNode source = getNode(nodeId);
            long targetId = this.modeNodeId;
            int inIndex = this.modeIndex;
            if (target != null && source != null) {
                mutateLocal(() -> this.session.manager.setReferenceInput(target.getName(), inIndex, source.getName(), index));
            }
            enterNodeSelected(targetId);
        } else {
            enterOutputPick(nodeId, index);
        }
    }

    /** Runs a mutation against the edit-session {@link FlowManager} and refreshes the graph view (not the menu/mode). */
    private void mutateLocal(Runnable action) {
        action.run();
        ensurePositions();
        rebuildNodeButtons();
        clampScroll();
        updateNodeButtonPositions();
        updateTopBarState();
    }

    // ------------------------------------------------------------------
    // Mode transitions - each one clears any leftover error from a previous action
    // ------------------------------------------------------------------

    private void enterIdleAdd() {
        this.mode = Mode.IDLE_ADD;
        this.modeNodeId = null;
        this.statusMessage = Component.empty();
        rebuildMenu();
    }

    private void enterNodeSelected(long nodeId) {
        this.mode = Mode.NODE_SELECTED;
        this.modeNodeId = nodeId;
        this.statusMessage = Component.empty();
        rebuildMenu();
    }

    private void enterRenaming(long nodeId) {
        this.mode = Mode.RENAMING;
        this.modeNodeId = nodeId;
        this.statusMessage = Component.empty();
        rebuildMenu();
    }

    private void enterBranchPick(long nodeId, int index) {
        this.mode = Mode.BRANCH_PICK;
        this.modeNodeId = nodeId;
        this.modeIndex = index;
        this.statusMessage = Component.empty();
        rebuildMenu();
    }

    private void enterInputPick(long nodeId, int index) {
        this.mode = Mode.INPUT_PICK;
        this.modeNodeId = nodeId;
        this.modeIndex = index;
        this.statusMessage = Component.empty();
        rebuildMenu();
    }

    private void enterOutputPick(long nodeId, int index) {
        this.mode = Mode.OUTPUT_PICK;
        this.modeNodeId = nodeId;
        this.modeIndex = index;
        this.statusMessage = Component.empty();
        rebuildMenu();
    }

    private void enterEditConst(long nodeId, int index) {
        FlowNode node = getNode(nodeId);
        String initial = "";
        if (node != null) {
            DataReference ref = node.getInput(index);
            if (ref.getType() == DataReference.ReferenceType.CONSTANT) {
                initial = String.valueOf(ref.getValue());
            }
        }
        this.pendingConstInitial = initial;
        this.mode = Mode.EDIT_CONST;
        this.modeNodeId = nodeId;
        this.modeIndex = index;
        this.statusMessage = Component.empty();
        rebuildMenu();
    }

    // ------------------------------------------------------------------
    // Bottom "editor menu" bar - contents depend entirely on the current mode.
    // No Cancel button anywhere: clicking empty canvas is the universal cancel.
    // ------------------------------------------------------------------

    private void addMenuWidget(AbstractWidget widget) {
        this.menuWidgets.add(widget);
        this.addRenderableWidget(widget);
    }

    private Component typeButtonLabel() {
        for (FlowEditorBridge.NodeTypeInfo info : FlowEditorBridge.nodeTypeCatalog()) {
            if (info.type.equals(this.pendingAddType)) {
                return info.metadata.displayName;
            }
        }
        return Component.literal(this.pendingAddType == null ? "?" : this.pendingAddType);
    }

    private void rebuildMenu() {
        for (AbstractWidget widget : this.menuWidgets) {
            this.removeWidget(widget);
        }
        this.menuWidgets.clear();

        int y = this.height - 26;
        int x = 10;

        switch (this.mode) {
            case IDLE_ADD -> {
                EditBox nameBox = new EditBox(this.font, x, y, this.width - 290, 20, Component.empty());
                nameBox.setHint(Component.translatable("fmod.flowgui.graph.menu.namehint").withStyle(ChatFormatting.DARK_GRAY));
                nameBox.setValue(this.pendingAddName);
                nameBox.setResponder(v -> this.pendingAddName = v);
                addMenuWidget(nameBox);
                x = this.width - 270;
                addMenuWidget(Button.builder(typeButtonLabel(), b -> openTypePicker()).pos(x, y).size(150, 20).build());
                x = this.width - 110;
                addMenuWidget(Button.builder(Component.translatable("fmod.flowgui.graph.addnode"), b -> doAddNode()).pos(x, y).size(100, 20).build());
            }
            case NODE_SELECTED -> {
                FlowNode node = getNode(this.modeNodeId);
                Button delete = Button.builder(Component.translatable("fmod.flowgui.graph.deletenode"), b -> doDeleteSelected()).pos(x, y).size(100, 20).build();
                delete.active = node != null && !node.isEventNode();
                addMenuWidget(delete);
            }
            case RENAMING -> {
                FlowNode node = getNode(this.modeNodeId);
                EditBox nameBox = new EditBox(this.font, x, y, this.width - 130, 20, Component.empty());
                nameBox.setHint(Component.translatable("fmod.flowgui.graph.menu.namehint").withStyle(ChatFormatting.DARK_GRAY));
                nameBox.setValue(node == null ? "" : node.getName());
                addMenuWidget(nameBox);
                x = this.width - 110;
                addMenuWidget(Button.builder(Component.translatable("fmod.misc.rename"), b -> doRename(nameBox.getValue())).pos(x, y).size(100, 20).build());
            }
            case BRANCH_PICK -> {
                FlowNode node = getNode(this.modeNodeId);
                FlowNode target = node == null ? null : getNode(node.getNextNodeIds().get(this.modeIndex));
                Component hint = target == null
                    ? Component.translatable("fmod.flowgui.graph.menu.branch.hint")
                    : Component.translatable("fmod.flowgui.graph.menu.branch.connected", target.getName());
                addMenuWidget(hintWidget(hint, x, y));
                x = this.width - 70;
                // Always shown (just disabled when there's nothing to clear) so the control is never
                // hidden - a branch that IS connected must always offer a way to disconnect it.
                Button clearBranch = Button.builder(Component.translatable("fmod.misc.clear"), b -> doClearBranch()).pos(x, y).size(60, 20).build();
                clearBranch.active = target != null;
                addMenuWidget(clearBranch);
            }
            case INPUT_PICK -> {
                FlowNode node = getNode(this.modeNodeId);
                DataReference ref = node == null ? null : node.getInput(this.modeIndex);
                Component hint;
                boolean canClear;
                Component constLabel = Component.translatable("fmod.misc.const");
                if (ref == null || (ref.getType() == DataReference.ReferenceType.CONSTANT && ref.getValue() == null)) {
                    hint = Component.translatable("fmod.flowgui.graph.menu.input.hint");
                    canClear = false;
                } else if (ref.getType() == DataReference.ReferenceType.NODE_OUTPUT) {
                    FlowNode source = getNode(ref.getReferenceId());
                    String outputName = source != null && ref.getReferenceIndex() >= 0 && ref.getReferenceIndex() < source.getMetadata().outputNumber
                        ? source.getMetadata().outputNames.get(ref.getReferenceIndex()).getString() : "?";
                    hint = Component.translatable("fmod.flowgui.graph.menu.input.connected", source == null ? "?" : source.getName(), outputName);
                    canClear = true;
                } else {
                    hint = Component.translatable("fmod.flowgui.graph.menu.input.constvalue", String.valueOf(ref.getValue()));
                    canClear = true;
                    constLabel = Component.translatable("fmod.misc.edit");
                }
                addMenuWidget(hintWidget(hint, x, y));
                x = this.width - 140;
                // Always shown (just disabled when already empty) - same reasoning as the branch Clear above.
                Button clearInput = Button.builder(Component.translatable("fmod.misc.clear"), b -> doClearInput()).pos(x, y).size(60, 20).build();
                clearInput.active = canClear;
                addMenuWidget(clearInput);
                x = this.width - 70;
                addMenuWidget(Button.builder(constLabel, b -> enterEditConst(this.modeNodeId, this.modeIndex)).pos(x, y).size(60, 20).build());
            }
            case OUTPUT_PICK -> addMenuWidget(hintWidget(Component.translatable("fmod.flowgui.graph.menu.output.hint"), x, y));
            case EDIT_CONST -> {
                EditBox valueBox = new EditBox(this.font, x, y, this.width - 90, 20, Component.empty());
                valueBox.setHint(Component.translatable("fmod.flowgui.graph.menu.valuehint").withStyle(ChatFormatting.DARK_GRAY));
                valueBox.setValue(this.pendingConstInitial);
                valueBox.setMaxLength(512);
                addMenuWidget(valueBox);
                x = this.width - 70;
                addMenuWidget(Button.builder(CommonComponents.GUI_DONE, b -> doSetConst(valueBox.getValue())).pos(x, y).size(60, 20).build());
            }
            case FEEDBACK_MESSAGE -> {
                addMenuWidget(hintWidget(this.statusMessage, x, y));
            }
        }
    }

    private StringWidget hintWidget(Component text, int x, int y) {
        StringWidget widget = new StringWidget(x, y, 300, 20, text, this.font);
        widget.alignLeft();
        return widget;
    }

    // ------------------------------------------------------------------
    // Menu actions
    // ------------------------------------------------------------------

    private void openTypePicker() {
        List<OptionPickerScreen.Option> options = new ArrayList<>();
        for (FlowEditorBridge.NodeTypeInfo info : FlowEditorBridge.nodeTypeCatalog()) {
            if (info.event) {
                continue;
            }
            Component label = Component.literal(info.type + " (").append(info.metadata.displayName).append(")");
            options.add(new OptionPickerScreen.Option(label, info.metadata.description, () -> {
                this.pendingAddType = info.type;
                this.minecraft.setScreen(this);
                rebuildMenu();
            }, info.type + " " + info.metadata.displayName.getString()));
        }
        this.minecraft.setScreen(new OptionPickerScreen(this, Component.translatable("fmod.flowgui.graph.addnode"), options));
    }

    private void doAddNode() {
        if (this.pendingAddName.isEmpty()) {
            this.statusMessage = Component.translatable("fmod.flowgui.graph.error.nameempty").withStyle(ChatFormatting.RED);
            this.mode = Mode.FEEDBACK_MESSAGE;
            rebuildMenu();
            return;
        }
        if (this.pendingAddType == null) {
            return;
        }
        if (this.session.manager.getFlow().getNodeByName(this.pendingAddName) != null) {
            this.statusMessage = Component.translatable("fmod.flowgui.graph.error.duplicate", this.pendingAddName).withStyle(ChatFormatting.RED);
            this.mode = Mode.FEEDBACK_MESSAGE;
            rebuildMenu();
            return;
        }
        String type = this.pendingAddType;
        String name = this.pendingAddName;
        mutateLocal(() -> this.session.manager.createNode(type, name));
        this.pendingAddName = "";
        this.statusMessage = Component.empty();
        rebuildMenu();
    }

    private void doDeleteSelected() {
        FlowNode node = getNode(this.modeNodeId);
        if (node == null || node.isEventNode()) {
            return;
        }
        mutateLocal(() -> this.session.manager.removeNode(node.getName()));
        enterIdleAdd();
    }

    private void doRename(String newName) {
        FlowNode node = getNode(this.modeNodeId);
        if (node == null) {
            return;
        }
        if (newName == null || newName.isEmpty()) {
            this.statusMessage = Component.translatable("fmod.flowgui.graph.error.nameempty").withStyle(ChatFormatting.RED);
            this.mode = Mode.FEEDBACK_MESSAGE;
            rebuildMenu();
            return;
        }
        if (newName.equals(node.getName())) {
            return;
        }
        if (this.session.manager.getFlow().getNodeByName(newName) != null) {
            this.statusMessage = Component.translatable("fmod.flowgui.graph.error.duplicate", newName).withStyle(ChatFormatting.RED);
            this.mode = Mode.FEEDBACK_MESSAGE;
            rebuildMenu();
            return;
        }
        long id = this.modeNodeId;
        mutateLocal(() -> this.session.manager.renameNode(node.getName(), newName));
        enterNodeSelected(id);
    }

    private void doClearBranch() {
        FlowNode node = getNode(this.modeNodeId);
        if (node == null) {
            return;
        }
        long id = this.modeNodeId;
        int index = this.modeIndex;
        mutateLocal(() -> this.session.manager.disconnectNextNode(node.getName(), index));
        enterNodeSelected(id);
    }

    private void doClearInput() {
        FlowNode node = getNode(this.modeNodeId);
        if (node == null) {
            return;
        }
        long id = this.modeNodeId;
        int index = this.modeIndex;
        mutateLocal(() -> this.session.manager.disconnectInput(node.getName(), index));
        enterNodeSelected(id);
    }

    private void doSetConst(String raw) {
        FlowNode node = getNode(this.modeNodeId);
        if (node == null) {
            return;
        }
        long id = this.modeNodeId;
        int index = this.modeIndex;
        Object value = FlowSerializer.parseConstDataReference(raw).getValue();
        mutateLocal(() -> this.session.manager.setConstInput(node.getName(), index, value));
        enterNodeSelected(id);
    }

    private void doUndo() {
        this.session.manager.undo();
        refreshAll();
    }

    private void doRedo() {
        this.session.manager.redo();
        refreshAll();
    }

    private void updateTopBarState() {
        this.undoButton.active = this.session.manager.canUndo();
        this.redoButton.active = this.session.manager.canRedo();
    }

    private void refreshAll() {
        ensurePositions();
        rebuildNodeButtons();
        clampScroll();
        updateNodeButtonPositions();
        if (this.modeNodeId != null && getNode(this.modeNodeId) == null) {
            this.mode = Mode.IDLE_ADD;
            this.modeNodeId = null;
        }
        updateTopBarState();
        rebuildMenu();
    }

    // ------------------------------------------------------------------
    // Commit / discard - the only points that talk to ServerData
    // ------------------------------------------------------------------

    private void update() {
        LogicFlow copy = this.session.manager.getFlow().copy();
        FlowEditorBridge.onClient(FlowEditorBridge.commitEdit(this.session.originalName, copy, false), result -> {
            this.statusMessage = result.message;
            this.mode = Mode.FEEDBACK_MESSAGE;
            rebuildMenu();
        });
    }

    private void save() {
        LogicFlow copy = this.session.manager.getFlow().copy();
        FlowEditorBridge.onClient(FlowEditorBridge.commitEdit(this.session.originalName, copy, true), result -> {
            this.statusMessage = result.message;
            this.mode = Mode.FEEDBACK_MESSAGE;
            rebuildMenu();
        });
    }

    private void exit() {
        this.parent.refresh();
        this.minecraft.setScreen(this.parent);
    }

    // ------------------------------------------------------------------
    // Scrolling (both axes): mouse wheel (+ shift for horizontal) and draggable scrollbar thumbs
    // ------------------------------------------------------------------

    private int viewWidth() {
        return this.canvasWidth - SCROLLBAR_SIZE;
    }

    private int viewHeight() {
        return this.canvasHeight - SCROLLBAR_SIZE;
    }

    private int contentWidth() {
        int max = MARGIN;
        for (Map.Entry<Long, int[]> entry : this.positions.entrySet()) {
            NodeBox box = this.nodeBoxes.get(entry.getKey());
            if (box != null) {
                max = Math.max(max, entry.getValue()[0] + box.width);
            }
        }
        return max + MARGIN;
    }

    private int contentHeight() {
        int max = MARGIN;
        for (Map.Entry<Long, int[]> entry : this.positions.entrySet()) {
            NodeBox box = this.nodeBoxes.get(entry.getKey());
            if (box != null) {
                max = Math.max(max, entry.getValue()[1] + box.height);
            }
        }
        return max + MARGIN;
    }

    private int maxScrollX() {
        return Math.max(0, contentWidth() - viewWidth());
    }

    private int maxScrollY() {
        return Math.max(0, contentHeight() - viewHeight());
    }

    private void clampScroll() {
        this.scrollX = Math.max(0, Math.min(this.scrollX, maxScrollX()));
        this.scrollY = Math.max(0, Math.min(this.scrollY, maxScrollY()));
    }

    /** {x, y, w, h} of the vertical scrollbar thumb, or null if the content fits without scrolling. */
    private int[] verticalThumbRect() {
        int maxScroll = maxScrollY();
        if (maxScroll <= 0) {
            return null;
        }
        int trackX = this.canvasX + this.canvasWidth - SCROLLBAR_SIZE;
        int trackH = viewHeight();
        int thumbH = Math.max(16, trackH * viewHeight() / contentHeight());
        int thumbY = this.canvasY + (trackH - thumbH) * this.scrollY / maxScroll;
        return new int[]{trackX, thumbY, SCROLLBAR_SIZE, thumbH};
    }

    /** {x, y, w, h} of the horizontal scrollbar thumb, or null if the content fits without scrolling. */
    private int[] horizontalThumbRect() {
        int maxScroll = maxScrollX();
        if (maxScroll <= 0) {
            return null;
        }
        int trackY = this.canvasY + this.canvasHeight - SCROLLBAR_SIZE;
        int trackW = viewWidth();
        int thumbW = Math.max(16, trackW * viewWidth() / contentWidth());
        int thumbX = this.canvasX + (trackW - thumbW) * this.scrollX / maxScroll;
        return new int[]{thumbX, trackY, thumbW, SCROLLBAR_SIZE};
    }

    private static boolean contains(int[] rect, double mouseX, double mouseY) {
        return rect != null && mouseX >= rect[0] && mouseX <= rect[0] + rect[2] && mouseY >= rect[1] && mouseY <= rect[1] + rect[3];
    }

    // ------------------------------------------------------------------
    // Mouse handling: real widgets (buttons) get first refusal via super.*;
    // then scrollbar thumbs; then node body (select + drag); then empty canvas (reset to idle-add)
    // ------------------------------------------------------------------

    private Long findNodeAt(double mouseX, double mouseY) {
        for (Map.Entry<Long, int[]> entry : this.positions.entrySet()) {
            NodeBox box = this.nodeBoxes.get(entry.getKey());
            if (box == null) {
                continue;
            }
            int bx = this.canvasX + entry.getValue()[0] - this.scrollX;
            int by = this.canvasY + entry.getValue()[1] - this.scrollY;
            if (mouseX >= bx && mouseX <= bx + box.width && mouseY >= by && mouseY <= by + box.height) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isOverCanvas(double mouseX, double mouseY) {
        return mouseX >= this.canvasX && mouseX <= this.canvasX + this.canvasWidth
            && mouseY >= this.canvasY && mouseY <= this.canvasY + this.canvasHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            if (contains(verticalThumbRect(), mouseX, mouseY)) {
                this.scrollDragging = ScrollDrag.VERTICAL;
                this.scrollDragStartMouse = (int) mouseY;
                this.scrollDragStartValue = this.scrollY;
                return true;
            }
            if (contains(horizontalThumbRect(), mouseX, mouseY)) {
                this.scrollDragging = ScrollDrag.HORIZONTAL;
                this.scrollDragStartMouse = (int) mouseX;
                this.scrollDragStartValue = this.scrollX;
                return true;
            }
        }
        Long hit = findNodeAt(mouseX, mouseY);
        if (hit != null) {
            enterNodeSelected(hit);
            if (button == 0) {
                this.draggingId = hit;
                int[] pos = this.positions.get(hit);
                this.dragOffsetX = (int) mouseX - this.canvasX - pos[0] + this.scrollX;
                this.dragOffsetY = (int) mouseY - this.canvasY - pos[1] + this.scrollY;
            }
            return true;
        }
        if (isOverCanvas(mouseX, mouseY)) {
            enterIdleAdd();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrollDragging == ScrollDrag.VERTICAL) {
            int trackH = viewHeight();
            int maxScroll = maxScrollY();
            int thumbH = Math.max(16, trackH * viewHeight() / contentHeight());
            int range = trackH - thumbH;
            int delta = (int) mouseY - this.scrollDragStartMouse;
            this.scrollY = range <= 0 ? 0 : Math.max(0, Math.min(maxScroll, this.scrollDragStartValue + delta * maxScroll / range));
            return true;
        }
        if (this.scrollDragging == ScrollDrag.HORIZONTAL) {
            int trackW = viewWidth();
            int maxScroll = maxScrollX();
            int thumbW = Math.max(16, trackW * viewWidth() / contentWidth());
            int range = trackW - thumbW;
            int delta = (int) mouseX - this.scrollDragStartMouse;
            this.scrollX = range <= 0 ? 0 : Math.max(0, Math.min(maxScroll, this.scrollDragStartValue + delta * maxScroll / range));
            return true;
        }
        if (this.draggingId != null) {
            int[] pos = this.positions.get(this.draggingId);
            if (pos != null) {
                pos[0] = Math.max(0, (int) mouseX - this.canvasX - this.dragOffsetX + this.scrollX);
                pos[1] = Math.max(0, (int) mouseY - this.canvasY - this.dragOffsetY + this.scrollY);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingId != null || this.scrollDragging != ScrollDrag.NONE) {
            this.draggingId = null;
            this.scrollDragging = ScrollDrag.NONE;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverCanvas(mouseX, mouseY)) {
            if (hasShiftDown()) {
                this.scrollX = Math.max(0, Math.min(maxScrollX(), this.scrollX - (int) (scrollY * 16)));
            } else {
                this.scrollY = Math.max(0, Math.min(maxScrollY(), this.scrollY - (int) (scrollY * 16)));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void drawConnections(GuiGraphics context) {
        LogicFlow flow = this.session.manager.getFlow();
        for (FlowNode node : flow.getNodes()) {
            NodeBox box = this.nodeBoxes.get(node.getId());
            if (box == null) {
                continue;
            }
            List<Long> nextIds = node.getNextNodeIds();
            for (int i = 0; i < nextIds.size(); i++) {
                long targetId = nextIds.get(i);
                if (targetId < 0) {
                    continue;
                }
                NodeBox targetBox = this.nodeBoxes.get(targetId);
                if (targetBox == null || i >= box.branchButtons.size()) {
                    continue;
                }
                Button from = box.branchButtons.get(i);
                Button to = targetBox.nameButton;
                drawLine(context, rightX(from), centerY(from), leftX(to), centerY(to), 0xFF55FF55);
            }
            List<DataReference> inputs = node.getInputs();
            for (int i = 0; i < inputs.size(); i++) {
                DataReference ref = inputs.get(i);
                if (ref.getType() != DataReference.ReferenceType.NODE_OUTPUT) {
                    continue;
                }
                NodeBox sourceBox = this.nodeBoxes.get(ref.getReferenceId());
                if (sourceBox == null || ref.getReferenceIndex() < 0 || ref.getReferenceIndex() >= sourceBox.outputButtons.size() || i >= box.inputButtons.size()) {
                    continue;
                }
                Button from = box.inputButtons.get(i);
                Button to = sourceBox.outputButtons.get(ref.getReferenceIndex());
                drawLine(context, leftX(from), centerY(from), rightX(to), centerY(to), 0xFFFF5555);
            }
        }
    }

    private static int leftX(Button button) {
        return button.getX();
    }

    private static int rightX(Button button) {
        return button.getX() + button.getWidth();
    }

    private static int centerY(Button button) {
        return button.getY() + button.getHeight() / 2;
    }

    private static void drawLine(GuiGraphics context, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(Math.max(dx, dy), 1);
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            context.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void drawNodeBoxes(GuiGraphics context) {
        LogicFlow flow = this.session.manager.getFlow();
        long startId = flow.getStartNodeId();
        for (FlowNode node : flow.getNodes()) {
            NodeBox box = this.nodeBoxes.get(node.getId());
            int[] pos = this.positions.get(node.getId());
            if (box == null || pos == null) {
                continue;
            }
            int bx = this.canvasX + pos[0] - this.scrollX;
            int by = this.canvasY + pos[1] - this.scrollY;
            if (bx + box.width < this.canvasX || bx > this.canvasX + viewWidth() || by + box.height < this.canvasY || by > this.canvasY + viewHeight()) {
                continue;
            }
            int fillColor = node.getId() == startId ? 0xFFAA8800 : 0xFF444444;
            context.fill(bx, by, bx + box.width, by + box.height, fillColor);
            boolean selected = this.modeNodeId != null && this.modeNodeId == node.getId() && this.mode != Mode.IDLE_ADD;
            int borderColor = selected ? 0xFFFFFF55 : 0xFF808080;
            context.fill(bx, by, bx + box.width, by + 1, borderColor);
            context.fill(bx, by + box.height - 1, bx + box.width, by + box.height, borderColor);
            context.fill(bx, by, bx + 1, by + box.height, borderColor);
            context.fill(bx + box.width - 1, by, bx + box.width, by + box.height, borderColor);

            context.drawString(this.font, box.typeText, bx + box.typeTextOffset[0], by + box.typeTextOffset[1], 0xAAAAAA, false);
            for (int i = 0; i < box.branchValueTexts.size(); i++) {
                int[] off = box.branchValueOffsets.get(i);
                context.drawString(this.font, box.branchValueTexts.get(i), bx + off[0], by + off[1], 0xCCCCCC, false);
            }
            for (int i = 0; i < box.inputValueTexts.size(); i++) {
                int[] off = box.inputValueOffsets.get(i);
                context.drawString(this.font, box.inputValueTexts.get(i), bx + off[0], by + off[1], 0xCCCCCC, false);
            }
        }
    }

    private void drawScrollbars(GuiGraphics context) {
        int[] vThumb = verticalThumbRect();
        if (vThumb != null) {
            int trackX = this.canvasX + this.canvasWidth - SCROLLBAR_SIZE;
            context.fill(trackX, this.canvasY, trackX + SCROLLBAR_SIZE, this.canvasY + viewHeight(), 0xFF2A2A2A);
            context.fill(vThumb[0], vThumb[1], vThumb[0] + vThumb[2], vThumb[1] + vThumb[3], 0xFF888888);
        }
        int[] hThumb = horizontalThumbRect();
        if (hThumb != null) {
            int trackY = this.canvasY + this.canvasHeight - SCROLLBAR_SIZE;
            context.fill(this.canvasX, trackY, this.canvasX + viewWidth(), trackY + SCROLLBAR_SIZE, 0xFF2A2A2A);
            context.fill(hThumb[0], hThumb[1], hThumb[0] + hThumb[2], hThumb[1] + hThumb[3], 0xFF888888);
        }
    }

    /** Node port buttons are added via {@link #addWidget} (interactive but not auto-rendered by
     * {@code super.render}) specifically so they can be rendered here, inside the canvas's scissor
     * region - otherwise a node scrolled only partially out of view would still draw its buttons
     * un-clipped, spilling over the top/bottom bars. */
    private void renderNodeButtons(GuiGraphics context, int mouseX, int mouseY, float delta) {
        for (NodeBox box : this.nodeBoxes.values()) {
            for (Button button : box.allButtons()) {
                if (button.visible) {
                    button.render(context, mouseX, mouseY, delta);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        updateNodeButtonPositions();
        context.fill(this.canvasX, this.canvasY, this.canvasX + this.canvasWidth, this.canvasY + this.canvasHeight, 0xFF1E1E1E);
        context.enableScissor(this.canvasX, this.canvasY, this.canvasX + viewWidth(), this.canvasY + viewHeight());
        drawConnections(context);
        drawNodeBoxes(context);
        renderNodeButtons(context, mouseX, mouseY, delta);
        context.disableScissor();
        drawScrollbars(context);
        context.drawString(this.font, this.title, 10, 14, 0xFFFFFF, false);
    }

    @Override
    public void onClose() {
        exit();
    }
}
