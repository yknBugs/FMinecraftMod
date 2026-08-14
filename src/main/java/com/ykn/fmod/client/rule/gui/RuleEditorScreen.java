/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.rule.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ykn.fmod.client.base.gui.OptionPickerScreen;
import com.ykn.fmod.client.base.gui.TextPromptScreen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Top-level rule editor entry screen: lists every rule currently loaded on the (local, singleplayer)
 * server. Click a row to select it, then Edit / Delete / Enable act on the selection.
 *
 * <p>"Edit" opens {@link RuleGraphScreen} on a detached deep copy of the rule - see
 * {@link RuleEditorBridge} for why nothing here is written back to the live rule until the user
 * explicitly commits from that screen.</p>
 */
@OnlyIn(Dist.CLIENT)
public class RuleEditorScreen extends Screen {

    private final Screen parent;
    private RuleListWidget list;
    private List<RuleEditorBridge.RuleListEntry> entries = new ArrayList<>();
    private String selectedRuleName;
    private Component statusMessage;

    private StringWidget statusText;
    private Button newButton;
    private Button editButton;
    private Button deleteButton;
    private Button toggleButton;
    private Button loadButton;
    private Button doneButton;

    public RuleEditorScreen(Screen parent) {
        super(Component.translatable("fmod.rulegui.editor.title"));
        this.parent = parent;
        this.statusMessage = Component.empty();
    }

    @Override
    protected void init() {
        this.list = new RuleListWidget(this.minecraft, this.width, this.height, 40, this.height - 80);
        this.addWidget(this.list);

        this.statusText = new StringWidget(this.width / 2 - 200, this.height - 80, 400, 20, this.statusMessage, this.font)
            .alignCenter();
        this.addRenderableWidget(this.statusText);
        this.newButton = Button.builder(Component.translatable("fmod.rulegui.editor.new"), b -> openCreatePrompt())
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
        this.loadButton = Button.builder(Component.translatable("fmod.rulegui.editor.loadfile"), b -> openLoadFilePicker())
            .pos(this.width / 2 + 160, this.height - 60).size(100, 20).build();
        this.addRenderableWidget(this.loadButton);
        this.doneButton = Button.builder(CommonComponents.GUI_DONE, b -> this.minecraft.setScreen(this.parent))
            .pos(this.width / 2 - 100, this.height - 30).size(200, 20).build();
        this.addRenderableWidget(this.doneButton);

        refresh();
    }

    void refresh() {
        RuleEditorBridge.onClient(RuleEditorBridge.listRules(), entries -> {
            this.entries = entries;
            this.list.clear();
            for (RuleEditorBridge.RuleListEntry entry : entries) {
                this.list.addRule(entry);
            }
            if (this.selectedRuleName != null && entries.stream().noneMatch(e -> e.name.equals(this.selectedRuleName))) {
                this.selectedRuleName = null;
            }
            updateSelectionButtons();
        });
    }

    private void selectRule(String name) {
        this.selectedRuleName = Objects.equals(this.selectedRuleName, name) ? null : name;
        updateSelectionButtons();
    }

    private RuleEditorBridge.RuleListEntry selectedEntry() {
        return this.entries.stream().filter(e -> e.name.equals(this.selectedRuleName)).findFirst().orElse(null);
    }

    private void updateSelectionButtons() {
        boolean hasSelection = this.selectedRuleName != null;
        this.editButton.active = hasSelection;
        this.deleteButton.active = hasSelection;
        this.toggleButton.active = hasSelection;
        RuleEditorBridge.RuleListEntry entry = selectedEntry();
        boolean showDisable = entry != null && entry.enabled;
        this.toggleButton.setMessage(Component.translatable(showDisable ? "fmod.misc.disable" : "fmod.misc.enable"));
    }

    private void openCreatePrompt() {
        this.minecraft.setScreen(new TextPromptScreen(this, Component.translatable("fmod.rulegui.editor.new"),
            Component.translatable("fmod.rulegui.editor.new.prompt"), "", name -> {
                if (name == null || name.isEmpty()) {
                    this.statusMessage = Component.translatable("fmod.command.rule.empty").withStyle(ChatFormatting.RED);
                    this.minecraft.setScreen(this);
                    return;
                }
                // Check for a name collision right away instead of only after the event-type picker,
                // so an invalid name fails immediately on Done rather than after another whole screen.
                RuleEditorBridge.onClient(RuleEditorBridge.listRules(), entries -> {
                    if (entries.stream().anyMatch(e -> e.name.equals(name))) {
                        this.statusMessage = Component.translatable("fmod.command.rule.exists", name).withStyle(ChatFormatting.RED);
                        this.minecraft.setScreen(this);
                        return;
                    }
                    List<OptionPickerScreen.Option> options = new ArrayList<>();
                    for (RuleEditorBridge.EventTypeInfo event : RuleEditorBridge.eventTypeCatalog()) {
                        options.add(new OptionPickerScreen.Option(Component.literal(event.type), event.render, () -> createRule(name, event.type)));
                    }
                    this.minecraft.setScreen(new OptionPickerScreen(this, Component.translatable("fmod.rulegui.editor.new.event"), options));
                });
            }));
    }

    private void createRule(String name, String eventType) {
        RuleEditorBridge.onClient(RuleEditorBridge.createRule(name, eventType), result -> {
            this.statusMessage = result.message.copy();
            if (!result.success) {
                this.minecraft.setScreen(this);
                return;
            }
            // Jump straight into editing the freshly created rule.
            RuleEditorBridge.onClient(RuleEditorBridge.beginEdit(name), session -> this.minecraft.setScreen(new RuleGraphScreen(this, session)));
        });
    }

    private void editSelected() {
        if (this.selectedRuleName == null) {
            return;
        }
        this.statusMessage = Component.empty();
        RuleEditorBridge.onClientResult(RuleEditorBridge.beginEdit(this.selectedRuleName),
            session -> this.minecraft.setScreen(new RuleGraphScreen(this, session)),
            error -> {
                this.statusMessage = Component.translatable("fmod.command.rule.notexists", this.selectedRuleName).withStyle(ChatFormatting.RED);
                refresh();
            });
    }

    private void deleteSelected() {
        if (this.selectedRuleName == null) {
            return;
        }
        RuleEditorBridge.onClient(RuleEditorBridge.deleteRule(this.selectedRuleName), result -> {
            this.statusMessage = result.message.copy();
            if (result.success) {
                this.selectedRuleName = null;
                refresh();
            }
        });
    }

    private void toggleSelected() {
        RuleEditorBridge.RuleListEntry entry = selectedEntry();
        if (entry == null) {
            return;
        }
        RuleEditorBridge.onClient(RuleEditorBridge.setRuleEnabled(entry.name, !entry.enabled), result -> {
            this.statusMessage = result.message.copy();
            if (result.success) {
                refresh();
            }
        });
    }

    private void openLoadFilePicker() {
        List<OptionPickerScreen.Option> options = new ArrayList<>();
        for (String file : RuleEditorBridge.listRuleFiles()) {
            options.add(new OptionPickerScreen.Option(Component.literal(file), null, () ->
                RuleEditorBridge.onClient(RuleEditorBridge.loadRuleFile(file), result -> {
                    this.statusMessage = result.message.copy();
                    this.minecraft.setScreen(this);
                    if (result.success) {
                        refresh();
                    }
                })));
        }
        this.minecraft.setScreen(new OptionPickerScreen(this, Component.translatable("fmod.rulegui.editor.loadfile"), options));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // this.list is added via addWidget (not addRenderableWidget) so it isn't auto-rendered by
        // super.render(); it must render first since AbstractSelectionList paints a full-width fade
        // above/below its own bounds that would otherwise mask the renderable widgets drawn by super.render().
        this.list.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        this.statusText.setMessage(this.statusMessage);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private class RuleListWidget extends ObjectSelectionList<RuleListWidget.Entry> {

        RuleListWidget(Minecraft client, int width, int height, int top, int bottom) {
            super(client, width, height, top, bottom, 20);
        }

        void addRule(RuleEditorBridge.RuleListEntry entry) {
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

            Entry(RuleEditorBridge.RuleListEntry entry) {
                this.name = entry.name;
                this.label = buildLabel(entry);
            }

            private Component buildLabel(RuleEditorBridge.RuleListEntry entry) {
                Component status = entry.enabled ? Component.translatable("fmod.misc.enabled") : Component.translatable("fmod.misc.disabled");
                return Component.literal(entry.name + "  ")
                    .append(tag(Component.literal(entry.eventType)))
                    .append(" ").append(tag(Component.translatable("fmod.rulegui.editor.row.extracount", String.valueOf(entry.extraCount))))
                    .append(" ").append(tag(Component.translatable("fmod.rulegui.editor.row.actioncount", String.valueOf(entry.satisfiedCount), String.valueOf(entry.violatedCount))))
                    .append(" ").append(tag(status));
            }

            private Component tag(Component inner) {
                return Component.literal("[").append(inner).append("]");
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                if (this.name.equals(selectedRuleName)) {
                    context.fill(x - 4, y - 1, x + entryWidth + 4, y + entryHeight + 1, 0x552277FF);
                } else if (hovered) {
                    context.fill(x - 4, y - 1, x + entryWidth + 4, y + entryHeight + 1, 0x30FFFFFF);
                }
                context.drawString(Minecraft.getInstance().font, this.label, x + 4, y + (entryHeight - 9) / 2, 0xFFFFFF, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                selectRule(this.name);
                return true;
            }

            @Override
            public Component getNarration() {
                return this.label;
            }
        }
    }
}
