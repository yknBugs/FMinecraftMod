/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.flow.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ykn.fmod.client.base.gui.OptionPickerScreen;
import com.ykn.fmod.client.base.gui.TextPromptScreen;
import com.ykn.fmod.server.flow.tool.NodeRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Top-level flow editor entry screen: lists every flow currently loaded on the (local, singleplayer)
 * server. Click a row to select it, then New / Edit / Delete act on the selection.
 *
 * <p>"Edit" opens {@link FlowGraphScreen} on a detached deep copy of the flow - see
 * {@link FlowEditorBridge} for why nothing here is written back to the live flow until the user
 * explicitly commits from that screen.</p>
 */
public class FlowEditorScreen extends Screen {

    private final Screen parent;
    private FlowListWidget list;
    private List<FlowEditorBridge.FlowListEntry> entries = new ArrayList<>();
    private String selectedFlowName;
    private Component statusMessage;

    private StringWidget statusText;
    private Button newButton;
    private Button editButton;
    private Button deleteButton;
    private Button toggleButton;
    private Button loadButton;
    private Button doneButton;

    public FlowEditorScreen(Screen parent) {
        super(Component.translatable("fmod.flowgui.editor.title"));
        this.parent = parent;
        this.statusMessage = Component.empty();
    }

    @Override
    protected void init() {
        this.list = new FlowListWidget(this.minecraft, this.width, this.height, 40, this.height - 80);
        this.addWidget(this.list);

        // Built here rather than in the constructor: this.width/this.height are still 0 until the
        // framework calls init(), so building this earlier pins it permanently off-screen.
        this.statusText = new StringWidget(this.width / 2 - 200, this.height - 80, 400, 20, this.statusMessage, this.font)
            .alignCenter();
        this.addRenderableWidget(this.statusText);
        this.newButton = Button.builder(Component.translatable("fmod.flowgui.editor.new"), b -> openCreatePrompt())
            .pos(this.width / 2 - 260, this.height - 60).size(100, 20).build();
        this.addRenderableWidget(this.newButton);
        this.editButton = Button.builder(Component.translatable("fmod.misc.edit"), b -> editSelected())
            .pos(this.width / 2 - 155, this.height - 60).size(100, 20).build();
        this.addRenderableWidget(this.editButton);
        this.deleteButton = Button.builder(Component.translatable("fmod.misc.delete"), b -> deleteSelected())
            .pos(this.width / 2 - 50, this.height - 60).size(100, 20).build();
        this.addRenderableWidget(this.deleteButton);
        this.toggleButton = Button.builder(Component.translatable("fmod.misc.enable"), b -> toggleSelected())
            .pos(this.width / 2 + 55, this.height - 60).size(100, 20).build();
        this.addRenderableWidget(this.toggleButton);
        this.loadButton = Button.builder(Component.translatable("fmod.flowgui.editor.loadfile"), b -> openLoadFilePicker())
            .pos(this.width / 2 + 160, this.height - 60).size(100, 20).build();
        this.addRenderableWidget(this.loadButton);
        this.doneButton = Button.builder(CommonComponents.GUI_DONE, b -> this.minecraft.setScreen(this.parent))
            .pos(this.width / 2 - 100, this.height - 30).size(200, 20).build();
        this.addRenderableWidget(this.doneButton);

        refresh();
    }

    void refresh() {
        FlowEditorBridge.onClient(FlowEditorBridge.listFlows(), entries -> {
            this.entries = entries;
            this.list.clear();
            for (FlowEditorBridge.FlowListEntry entry : entries) {
                this.list.addFlow(entry);
            }
            if (this.selectedFlowName != null && entries.stream().noneMatch(e -> e.name.equals(this.selectedFlowName))) {
                this.selectedFlowName = null;
            }
            updateSelectionButtons();
        });
    }

    private void selectFlow(String name) {
        this.selectedFlowName = Objects.equals(this.selectedFlowName, name) ? null : name;
        updateSelectionButtons();
    }

    private FlowEditorBridge.FlowListEntry selectedEntry() {
        return this.entries.stream().filter(e -> e.name.equals(this.selectedFlowName)).findFirst().orElse(null);
    }

    private void updateSelectionButtons() {
        boolean hasSelection = this.selectedFlowName != null;
        this.editButton.active = hasSelection;
        this.deleteButton.active = hasSelection;
        FlowEditorBridge.FlowListEntry entry = selectedEntry();
        // A corrupted flow (no first/event node) can't meaningfully run, so don't offer to enable it.
        this.toggleButton.active = entry != null && !entry.corrupted;
        boolean showDisable = entry != null && entry.enabled;
        this.toggleButton.setMessage(Component.translatable(showDisable ? "fmod.misc.disable" : "fmod.misc.enable"));
    }

    private void openCreatePrompt() {
        this.minecraft.setScreen(new TextPromptScreen(this, Component.translatable("fmod.flowgui.editor.new"),
            Component.translatable("fmod.flowgui.editor.new.prompt"), "", name -> {
                if (name == null || name.isEmpty()) {
                    this.statusMessage = Component.translatable("fmod.command.flow.empty").withStyle(ChatFormatting.RED);
                    this.minecraft.setScreen(this);
                    return;
                }
                // Check for a name collision right away instead of only after the event-type picker,
                // so an invalid name fails immediately on Done rather than after another whole screen.
                FlowEditorBridge.onClient(FlowEditorBridge.listFlows(), entries -> {
                    if (entries.stream().anyMatch(e -> e.name.equals(name))) {
                        this.statusMessage = Component.translatable("fmod.command.flow.exists", name).withStyle(ChatFormatting.RED);
                        this.minecraft.setScreen(this);
                        return;
                    }
                    List<OptionPickerScreen.Option> options = new ArrayList<>();
                    for (String eventType : NodeRegistry.getEventNodeList()) {
                        options.add(new OptionPickerScreen.Option(Component.literal(eventType), null, () -> createFlow(name, eventType)));
                    }
                    this.minecraft.setScreen(new OptionPickerScreen(this, Component.translatable("fmod.flowgui.editor.new.event"), options));
                });
            }));
    }

    private void createFlow(String name, String eventType) {
        FlowEditorBridge.onClient(FlowEditorBridge.createFlow(name, eventType, eventType), result -> {
            this.statusMessage = result.message.copy();
            if (!result.success) {
                this.minecraft.setScreen(this);
                return;
            }
            // Jump straight into editing the freshly created flow.
            FlowEditorBridge.onClient(FlowEditorBridge.beginEdit(name), session -> this.minecraft.setScreen(new FlowGraphScreen(this, session)));
        });
    }

    private void editSelected() {
        if (this.selectedFlowName == null) {
            return;
        }
        // Clear any leftover message from an earlier, unrelated action - editing never sets its own
        // success message (it just navigates away), so without this a stale message (e.g. "Created
        // flow X") would still be showing when the user comes back from editing a different flow.
        this.statusMessage = Component.empty();
        FlowEditorBridge.onClientResult(FlowEditorBridge.beginEdit(this.selectedFlowName),
            session -> this.minecraft.setScreen(new FlowGraphScreen(this, session)),
            error -> {
                this.statusMessage = Component.translatable("fmod.command.flow.notexists", this.selectedFlowName).withStyle(ChatFormatting.RED);
                refresh();
            });
    }

    private void deleteSelected() {
        if (this.selectedFlowName == null) {
            return;
        }
        FlowEditorBridge.onClient(FlowEditorBridge.deleteFlow(this.selectedFlowName), result -> {
            this.statusMessage = result.message.copy();
            if (result.success) {
                this.selectedFlowName = null;
                refresh();
            }
        });
    }

    private void toggleSelected() {
        FlowEditorBridge.FlowListEntry entry = selectedEntry();
        if (entry == null || entry.corrupted) {
            return;
        }
        FlowEditorBridge.onClient(FlowEditorBridge.setFlowEnabled(entry.name, !entry.enabled), result -> {
            this.statusMessage = result.message.copy();
            if (result.success) {
                refresh();
            }
        });
    }

    private void openLoadFilePicker() {
        List<OptionPickerScreen.Option> options = new ArrayList<>();
        for (String file : FlowEditorBridge.listFlowFiles()) {
            options.add(new OptionPickerScreen.Option(Component.literal(file), null, () ->
                FlowEditorBridge.onClient(FlowEditorBridge.loadFlowFile(file), result -> {
                    this.statusMessage = result.message.copy();
                    this.minecraft.setScreen(this);
                    if (result.success) {
                        refresh();
                    }
                })));
        }
        this.minecraft.setScreen(new OptionPickerScreen(this, Component.translatable("fmod.flowgui.editor.loadfile"), options));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // super.render() now renders the background and the renderable widgets itself, so it must
        // come first; this.list is added via addWidget (not auto-rendered) and drawn on top.
        this.statusText.setMessage(this.statusMessage);
        super.render(context, mouseX, mouseY, delta);
        this.list.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private class FlowListWidget extends ObjectSelectionList<FlowListWidget.Entry> {

        FlowListWidget(Minecraft client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 20);
        }

        void addFlow(FlowEditorBridge.FlowListEntry entry) {
            this.addEntry(new Entry(entry));
        }

        void clear() {
            this.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return this.width - 40;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 5;
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String name;
            private final Component label;

            Entry(FlowEditorBridge.FlowListEntry entry) {
                this.name = entry.name;
                this.label = buildLabel(entry);
            }

            private Component buildLabel(FlowEditorBridge.FlowListEntry entry) {
                Component firstNode = entry.corrupted
                    ? Component.translatable("fmod.flowgui.editor.row.nofirstnode")
                    : Component.translatable("fmod.flowgui.editor.row.firstnode", entry.firstNodeName);
                Component status = entry.corrupted
                    ? Component.translatable("fmod.flowgui.editor.row.corrupted")
                    : (entry.enabled ? Component.translatable("fmod.misc.enabled") : Component.translatable("fmod.misc.disabled"));
                return Component.literal(entry.name + "  ")
                    .append(tag(Component.translatable("fmod.flowgui.editor.row.nodecount", String.valueOf(entry.nodeCount))))
                    .append(" ").append(tag(firstNode))
                    .append(" ").append(tag(status));
            }

            private Component tag(Component inner) {
                return Component.literal("[").append(inner).append("]");
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                if (this.name.equals(selectedFlowName)) {
                    context.fill(x - 4, y - 1, x + entryWidth + 4, y + entryHeight + 1, 0x552277FF);
                } else if (hovered) {
                    context.fill(x - 4, y - 1, x + entryWidth + 4, y + entryHeight + 1, 0x30FFFFFF);
                }
                context.drawString(Minecraft.getInstance().font, this.label, x + 4, y + (entryHeight - 9) / 2, 0xFFFFFF, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                selectFlow(this.name);
                return true;
            }

            @Override
            public Component getNarration() {
                return this.label;
            }
        }
    }
}
