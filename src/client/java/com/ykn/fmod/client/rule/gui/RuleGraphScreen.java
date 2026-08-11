/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.rule.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import com.ykn.fmod.client.base.gui.OptionPickerScreen;
import com.ykn.fmod.client.base.gui.TextPromptScreen;
import com.ykn.fmod.server.rule.core.CustomRule;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.tool.ConditionFormulaParser;
import com.ykn.fmod.server.rule.tool.RuleManager;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Main rule form editor: event picker, three sections (extra conditions, actions-if-satisfied,
 * actions-if-violated) each with add/edit/rename/remove, a validated main-condition formula box,
 * client-side undo/redo, and Save/Save &amp; Persist/Cancel.
 *
 * <p>Holds one {@code final RuleManager} for the whole session (the deep copy handed over by
 * {@link RuleEditorBridge#beginEdit}) and mutates it directly and synchronously on the client
 * thread - the copy is registered nowhere, so nothing here is visible to the live rule until
 * {@link #save} explicitly commits. Undo/redo is a client-side {@link CustomRule#copy()} snapshot
 * stack; restoring a snapshot calls {@link RuleManager#setRule}, which the rule-GUI work made
 * non-final specifically to support this in-place restore.</p>
 */
public class RuleGraphScreen extends Screen {

    private final Screen parent;
    private final RuleEditorBridge.EditSession session;
    private final RuleManager workingManager;

    private final Deque<CustomRule> undoStack = new ArrayDeque<>();
    private final Deque<CustomRule> redoStack = new ArrayDeque<>();

    private String selectedExtraCondition;
    private String selectedSatisfiedAction;
    private String selectedViolatedAction;

    private Component statusMessage = Component.empty();

    private StringWidget eventLabel;
    private Button eventChangeButton;
    private EditBox formulaBox;
    private StringWidget statusText;

    private NamedListWidget extraList;
    private NamedListWidget satisfiedList;
    private NamedListWidget violatedList;

    private Button undoButton;
    private Button redoButton;

    public RuleGraphScreen(Screen parent, RuleEditorBridge.EditSession session) {
        super(Component.translatable("fmod.rulegui.rule.title", session.manager.getRule().getName()));
        this.parent = parent;
        this.session = session;
        this.workingManager = session.manager;
    }

    private void beforeMutate() {
        this.statusMessage = Component.empty();
        this.undoStack.push(this.workingManager.getRule().copy());
        this.redoStack.clear();
    }

    private void undo() {
        if (this.undoStack.isEmpty()) {
            return;
        }
        this.statusMessage = Component.empty();
        this.redoStack.push(this.workingManager.getRule().copy());
        this.workingManager.setRule(this.undoStack.pop());
        refreshAll();
    }

    private void redo() {
        if (this.redoStack.isEmpty()) {
            return;
        }
        this.statusMessage = Component.empty();
        this.undoStack.push(this.workingManager.getRule().copy());
        this.workingManager.setRule(this.redoStack.pop());
        refreshAll();
    }

    @Override
    protected void init() {
        int buttonHeight = 20;
        int gap = 4;
        int topY = 8;

        int cancelW = 60, updateW = 60, saveW = 60, redoW = 60, undoW = 60;
        int cancelX = this.width - 8 - cancelW;
        int saveX = cancelX - gap - saveW;
        int updateX = saveX - gap - updateW;
        int redoX = updateX - gap - redoW;
        int undoX = redoX - gap - undoW;

        this.undoButton = Button.builder(Component.translatable("fmod.misc.undo"), b -> undo()).pos(undoX, topY).size(undoW, buttonHeight).build();
        this.redoButton = Button.builder(Component.translatable("fmod.misc.redo"), b -> redo()).pos(redoX, topY).size(redoW, buttonHeight).build();
        this.addRenderableWidget(this.undoButton);
        this.addRenderableWidget(this.redoButton);
        this.addRenderableWidget(Button.builder(Component.translatable("fmod.misc.update"), b -> save(false)).pos(updateX, topY).size(updateW, buttonHeight).build());
        this.addRenderableWidget(Button.builder(Component.translatable("fmod.misc.save"), b -> save(true)).pos(saveX, topY).size(saveW, buttonHeight).build());
        this.addRenderableWidget(Button.builder(Component.translatable("fmod.misc.exit"), b -> exit()).pos(cancelX, topY).size(cancelW, buttonHeight).build());

        this.addRenderableWidget(new StringWidget(15, topY + (buttonHeight - 9) / 2, undoX - 20, 9, this.title, this.font).alignLeft());

        int rowBY = topY + buttonHeight + 6;
        this.eventChangeButton = Button.builder(Component.translatable("fmod.rulegui.rule.event.change"), b -> openEventPicker())
            .pos(this.width - 8 - 90, rowBY).size(90, buttonHeight).build();
        this.addRenderableWidget(this.eventChangeButton);
        int applyFormulaX = this.eventChangeButton.getX() - gap - 85;
        this.addRenderableWidget(Button.builder(Component.translatable("fmod.rulegui.rule.formula.apply"), b -> applyFormula())
            .pos(applyFormulaX, rowBY).size(85, buttonHeight).build());

        int eventLabelWidth = 170;
        this.eventLabel = new StringWidget(8, rowBY + (buttonHeight - 9) / 2, eventLabelWidth, 9, Component.empty(), this.font);
        this.addRenderableWidget(this.eventLabel);

        int formulaX = 8 + eventLabelWidth + gap;
        int formulaWidth = applyFormulaX - gap - formulaX;
        this.formulaBox = new EditBox(this.font, formulaX, rowBY, formulaWidth, buttonHeight, Component.empty());
        this.formulaBox.setMaxLength(1024);
        this.formulaBox.setHint(Component.translatable("fmod.rulegui.rule.formula.hint").withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.formulaBox);

        int statusY = rowBY + buttonHeight + 8;
        this.statusText = new StringWidget(8, statusY, this.width - 16, 10, Component.empty(), this.font);
        this.addRenderableWidget(this.statusText);

        int headerY = statusY + 12;
        int columnWidth = (this.width - 32) / 3;
        int col0X = 8;
        int col1X = col0X + columnWidth + 8;
        int col2X = col1X + columnWidth + 8;

        this.addRenderableWidget(new StringWidget(col0X, headerY, columnWidth, 12, Component.translatable("fmod.rulegui.rule.section.extra"), this.font));
        this.addRenderableWidget(new StringWidget(col1X, headerY, columnWidth, 12, Component.translatable("fmod.rulegui.rule.section.satisfied"), this.font));
        this.addRenderableWidget(new StringWidget(col2X, headerY, columnWidth, 12, Component.translatable("fmod.rulegui.rule.section.violated"), this.font));

        int listTop = headerY + 14;
        int bottomRowY = this.height - 8 - buttonHeight;
        int listBottom = bottomRowY - 6;

        this.extraList = new NamedListWidget(this.minecraft, columnWidth, listBottom - listTop, listTop, listBottom,
            () -> this.selectedExtraCondition, name -> { this.selectedExtraCondition = toggle(this.selectedExtraCondition, name); updateSectionButtons(); });
        this.extraList.setLeftPos(col0X);
        this.extraList.setRenderTopAndBottom(false);
        this.addWidget(this.extraList);

        this.satisfiedList = new NamedListWidget(this.minecraft, columnWidth, listBottom - listTop, listTop, listBottom,
            () -> this.selectedSatisfiedAction, name -> { this.selectedSatisfiedAction = toggle(this.selectedSatisfiedAction, name); updateSectionButtons(); });
        this.satisfiedList.setLeftPos(col1X);
        this.satisfiedList.setRenderTopAndBottom(false);
        this.addWidget(this.satisfiedList);

        this.violatedList = new NamedListWidget(this.minecraft, columnWidth, listBottom - listTop, listTop, listBottom,
            () -> this.selectedViolatedAction, name -> { this.selectedViolatedAction = toggle(this.selectedViolatedAction, name); updateSectionButtons(); });
        this.violatedList.setLeftPos(col2X);
        this.violatedList.setRenderTopAndBottom(false);
        this.addWidget(this.violatedList);

        int rowButtonWidth = (columnWidth - 3 * gap) / 4;

        this.extraAddButton = Button.builder(Component.translatable("fmod.misc.add"), b -> addCondition()).pos(col0X, bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.extraEditButton = Button.builder(Component.translatable("fmod.misc.edit"), b -> editCondition()).pos(col0X + (rowButtonWidth + gap), bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.extraRenameButton = Button.builder(Component.translatable("fmod.misc.rename"), b -> renameCondition()).pos(col0X + 2 * (rowButtonWidth + gap), bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.extraRemoveButton = Button.builder(Component.translatable("fmod.misc.delete"), b -> removeCondition()).pos(col0X + 3 * (rowButtonWidth + gap), bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.addRenderableWidget(this.extraAddButton);
        this.addRenderableWidget(this.extraEditButton);
        this.addRenderableWidget(this.extraRenameButton);
        this.addRenderableWidget(this.extraRemoveButton);

        this.satisfiedAddButton = Button.builder(Component.translatable("fmod.misc.add"), b -> addAction(true)).pos(col1X, bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.satisfiedEditButton = Button.builder(Component.translatable("fmod.misc.edit"), b -> editAction(true)).pos(col1X + (rowButtonWidth + gap), bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.satisfiedRenameButton = Button.builder(Component.translatable("fmod.misc.rename"), b -> renameAction(true)).pos(col1X + 2 * (rowButtonWidth + gap), bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.satisfiedRemoveButton = Button.builder(Component.translatable("fmod.misc.delete"), b -> removeAction(true)).pos(col1X + 3 * (rowButtonWidth + gap), bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.addRenderableWidget(this.satisfiedAddButton);
        this.addRenderableWidget(this.satisfiedEditButton);
        this.addRenderableWidget(this.satisfiedRenameButton);
        this.addRenderableWidget(this.satisfiedRemoveButton);

        this.violatedAddButton = Button.builder(Component.translatable("fmod.misc.add"), b -> addAction(false)).pos(col2X, bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.violatedEditButton = Button.builder(Component.translatable("fmod.misc.edit"), b -> editAction(false)).pos(col2X + (rowButtonWidth + gap), bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.violatedRenameButton = Button.builder(Component.translatable("fmod.misc.rename"), b -> renameAction(false)).pos(col2X + 2 * (rowButtonWidth + gap), bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.violatedRemoveButton = Button.builder(Component.translatable("fmod.misc.delete"), b -> removeAction(false)).pos(col2X + 3 * (rowButtonWidth + gap), bottomRowY).size(rowButtonWidth, buttonHeight).build();
        this.addRenderableWidget(this.violatedAddButton);
        this.addRenderableWidget(this.violatedEditButton);
        this.addRenderableWidget(this.violatedRenameButton);
        this.addRenderableWidget(this.violatedRemoveButton);

        refreshAll();
    }

    private String toggle(String current, String name) {
        return Objects.equals(current, name) ? null : name;
    }

    // ------------------------------------------------------------------
    // Refresh
    // ------------------------------------------------------------------

    private void refreshAll() {
        CustomRule rule = this.workingManager.getRule();
        this.eventLabel.setMessage(Component.translatable("fmod.rulegui.rule.event", rule.getEvent().getType()));
        this.eventLabel.setTooltip(Tooltip.create(rule.getEvent().render()));
        this.formulaBox.setValue(ConditionFormulaParser.toFormula(rule.getCondition()));
        this.statusText.setMessage(this.statusMessage);

        List<NamedListWidget.NameLabel> extraItems = new ArrayList<>();
        for (RuleCondition condition : rule.getExtra()) {
            extraItems.add(new NamedListWidget.NameLabel(condition.getName(), Component.literal(condition.getName() + "  [" + condition.getType() + "]")));
        }
        this.extraList.setItems(extraItems);
        if (this.selectedExtraCondition != null && rule.getExtraCondition(this.selectedExtraCondition) == null) {
            this.selectedExtraCondition = null;
        }

        List<NamedListWidget.NameLabel> satisfiedItems = new ArrayList<>();
        for (RuleAction action : rule.getActionIfSatisfied()) {
            satisfiedItems.add(new NamedListWidget.NameLabel(action.getName(), Component.literal(action.getName() + "  [" + action.getType() + "]")));
        }
        this.satisfiedList.setItems(satisfiedItems);
        if (this.selectedSatisfiedAction != null && rule.getActionIfSatisfied(this.selectedSatisfiedAction) == null) {
            this.selectedSatisfiedAction = null;
        }

        List<NamedListWidget.NameLabel> violatedItems = new ArrayList<>();
        for (RuleAction action : rule.getActionIfViolated()) {
            violatedItems.add(new NamedListWidget.NameLabel(action.getName(), Component.literal(action.getName() + "  [" + action.getType() + "]")));
        }
        this.violatedList.setItems(violatedItems);
        if (this.selectedViolatedAction != null && rule.getActionIfViolated(this.selectedViolatedAction) == null) {
            this.selectedViolatedAction = null;
        }

        updateSectionButtons();
    }

    private void updateSectionButtons() {
        this.undoButton.active = !this.undoStack.isEmpty();
        this.redoButton.active = !this.redoStack.isEmpty();

        boolean hasExtra = this.selectedExtraCondition != null;
        this.extraEditButton.active = hasExtra;
        this.extraRenameButton.active = hasExtra;
        this.extraRemoveButton.active = hasExtra;

        boolean hasSatisfied = this.selectedSatisfiedAction != null;
        this.satisfiedEditButton.active = hasSatisfied;
        this.satisfiedRenameButton.active = hasSatisfied;
        this.satisfiedRemoveButton.active = hasSatisfied;

        boolean hasViolated = this.selectedViolatedAction != null;
        this.violatedEditButton.active = hasViolated;
        this.violatedRenameButton.active = hasViolated;
        this.violatedRemoveButton.active = hasViolated;
    }

    private Button extraAddButton;
    private Button extraEditButton;
    private Button extraRenameButton;
    private Button extraRemoveButton;
    private Button satisfiedAddButton;
    private Button satisfiedEditButton;
    private Button satisfiedRenameButton;
    private Button satisfiedRemoveButton;
    private Button violatedAddButton;
    private Button violatedEditButton;
    private Button violatedRenameButton;
    private Button violatedRemoveButton;

    // ------------------------------------------------------------------
    // Event
    // ------------------------------------------------------------------

    private void openEventPicker() {
        List<OptionPickerScreen.Option> options = new ArrayList<>();
        for (RuleEditorBridge.EventTypeInfo event : RuleEditorBridge.eventTypeCatalog()) {
            options.add(new OptionPickerScreen.Option(Component.literal(event.type), event.render, () -> {
                beforeMutate();
                this.workingManager.setEvent(event.type);
                this.minecraft.setScreen(this);
                refreshAll();
            }));
        }
        this.minecraft.setScreen(new OptionPickerScreen(this, Component.translatable("fmod.rulegui.rule.event.change"), options));
    }

    // ------------------------------------------------------------------
    // Formula
    // ------------------------------------------------------------------

    private void applyFormula() {
        CustomRule snapshot = this.workingManager.getRule().copy();
        try {
            this.workingManager.setCondition(this.formulaBox.getValue());
            this.undoStack.push(snapshot);
            this.redoStack.clear();
            this.statusMessage = Component.translatable("fmod.rulegui.rule.formula.applied").withStyle(ChatFormatting.GREEN);
            updateSectionButtons();
        } catch (IllegalArgumentException e) {
            this.statusMessage = Component.literal(e.getMessage()).withStyle(ChatFormatting.RED);
        }
        this.statusText.setMessage(this.statusMessage);
    }

    // ------------------------------------------------------------------
    // Extra conditions
    // ------------------------------------------------------------------

    /**
     * Validates a newly-entered (or renamed-to) extra-condition name: non-empty, matching
     * {@link RuleCondition#NAME_PATTERN} - the identifier syntax {@code ConditionFormulaParser}
     * requires, since extra conditions are referenced by name from the main formula - and not
     * already used by another extra condition.
     *
     * @return an error message to show the user, or {@code null} if {@code name} is valid
     */
    private Component validateNewConditionName(String name, boolean alreadyExists) {
        if (name == null || name.isEmpty()) {
            return Component.translatable("fmod.command.rule.empty").withStyle(ChatFormatting.RED);
        }
        if (!RuleCondition.NAME_PATTERN.matcher(name).matches()) {
            return Component.translatable("fmod.rulegui.rule.name.invalid", name).withStyle(ChatFormatting.RED);
        }
        if (alreadyExists) {
            return Component.translatable("fmod.rulegui.rule.name.exists", name).withStyle(ChatFormatting.RED);
        }
        return null;
    }

    /**
     * Validates a newly-entered (or renamed-to) action name: non-empty and not already used within
     * the same section. Unlike {@link #validateNewConditionName}, actions are never referenced from
     * a formula, so their names aren't required to be identifier syntax.
     *
     * @return an error message to show the user, or {@code null} if {@code name} is valid
     */
    private Component validateNewActionName(String name, boolean alreadyExists) {
        if (name == null || name.isEmpty()) {
            return Component.translatable("fmod.command.rule.empty").withStyle(ChatFormatting.RED);
        }
        if (alreadyExists) {
            return Component.translatable("fmod.rulegui.rule.name.exists", name).withStyle(ChatFormatting.RED);
        }
        return null;
    }

    private void addCondition() {
        List<OptionPickerScreen.Option> options = new ArrayList<>();
        for (RuleEditorBridge.ComponentTypeInfo type : RuleEditorBridge.conditionTypeCatalog()) {
            options.add(new OptionPickerScreen.Option(Component.literal(type.type), null, () -> promptConditionName(type.type)));
        }
        this.minecraft.setScreen(new OptionPickerScreen(this, Component.translatable("fmod.misc.add"), options));
    }

    private void promptConditionName(String type) {
        this.minecraft.setScreen(new TextPromptScreen(this, Component.translatable("fmod.misc.add"),
            Component.translatable("fmod.rulegui.rule.name.prompt"), "", name -> {
                Component error = validateNewConditionName(name, this.workingManager.getRule().hasExtraCondition(name));
                if (error != null) {
                    this.statusMessage = error;
                    this.minecraft.setScreen(this);
                    return;
                }
                this.minecraft.setScreen(ParamEditorScreen.forCreate(this, ParamEditorScreen.conditionAdapter(), type, name,
                    this.workingManager.getRule().getName(), this.workingManager.getRule().getEvent(), edited -> {
                        beforeMutate();
                        this.workingManager.addCondition(edited);
                        refreshAll();
                    }));
            }));
    }

    private void editCondition() {
        if (this.selectedExtraCondition == null) {
            return;
        }
        RuleCondition existing = this.workingManager.getRule().getExtraCondition(this.selectedExtraCondition);
        if (!(existing instanceof SourceCondition sc)) {
            this.statusMessage = Component.translatable("fmod.rulegui.rule.condition.noteditable", existing.getName(), existing.getType())
                .withStyle(ChatFormatting.RED);
            this.statusText.setMessage(this.statusMessage);
            return;
        }
        this.minecraft.setScreen(ParamEditorScreen.forEdit(this, ParamEditorScreen.conditionAdapter(), sc,
            this.workingManager.getRule().getName(), this.workingManager.getRule().getEvent(), edited -> {
                beforeMutate();
                this.workingManager.removeCondition(sc.getName());
                this.workingManager.addCondition(edited);
                refreshAll();
            }));
    }

    private void renameCondition() {
        if (this.selectedExtraCondition == null) {
            return;
        }
        String oldName = this.selectedExtraCondition;
        this.minecraft.setScreen(new TextPromptScreen(this, Component.translatable("fmod.misc.rename"),
            Component.translatable("fmod.rulegui.rule.name.prompt"), oldName, newName -> {
                Component error = validateNewConditionName(newName, this.workingManager.getRule().hasExtraCondition(newName));
                if (error == null) {
                    beforeMutate();
                    this.workingManager.renameCondition(oldName, newName);
                    this.selectedExtraCondition = newName;
                    refreshAll();
                } else {
                    this.statusMessage = error;
                }
                this.minecraft.setScreen(this);
            }));
    }

    private void removeCondition() {
        if (this.selectedExtraCondition == null) {
            return;
        }
        beforeMutate();
        this.workingManager.removeCondition(this.selectedExtraCondition);
        this.selectedExtraCondition = null;
        refreshAll();
    }

    // ------------------------------------------------------------------
    // Actions (satisfied / violated)
    // ------------------------------------------------------------------

    private void addAction(boolean satisfied) {
        List<OptionPickerScreen.Option> options = new ArrayList<>();
        for (RuleEditorBridge.ComponentTypeInfo type : RuleEditorBridge.actionTypeCatalog()) {
            options.add(new OptionPickerScreen.Option(Component.literal(type.type), null, () -> promptActionName(type.type, satisfied)));
        }
        this.minecraft.setScreen(new OptionPickerScreen(this, Component.translatable("fmod.misc.add"), options));
    }

    private void promptActionName(String type, boolean satisfied) {
        this.minecraft.setScreen(new TextPromptScreen(this, Component.translatable("fmod.misc.add"),
            Component.translatable("fmod.rulegui.rule.name.prompt"), "", name -> {
                boolean exists = satisfied ? this.workingManager.getRule().hasActionIfSatisfied(name) : this.workingManager.getRule().hasActionIfViolated(name);
                Component error = validateNewActionName(name, exists);
                if (error != null) {
                    this.statusMessage = error;
                    this.minecraft.setScreen(this);
                    return;
                }
                this.minecraft.setScreen(ParamEditorScreen.forCreate(this, ParamEditorScreen.actionAdapter(), type, name,
                    this.workingManager.getRule().getName(), this.workingManager.getRule().getEvent(), edited -> {
                        beforeMutate();
                        if (satisfied) {
                            this.workingManager.addActionIfSatisfied(edited);
                        } else {
                            this.workingManager.addActionIfViolated(edited);
                        }
                        refreshAll();
                    }));
            }));
    }

    private void editAction(boolean satisfied) {
        String name = satisfied ? this.selectedSatisfiedAction : this.selectedViolatedAction;
        if (name == null) {
            return;
        }
        RuleAction existing = satisfied ? this.workingManager.getRule().getActionIfSatisfied(name) : this.workingManager.getRule().getActionIfViolated(name);
        if (existing == null) {
            return;
        }
        this.minecraft.setScreen(ParamEditorScreen.forEdit(this, ParamEditorScreen.actionAdapter(), existing,
            this.workingManager.getRule().getName(), this.workingManager.getRule().getEvent(), edited -> {
                beforeMutate();
                if (satisfied) {
                    this.workingManager.removeActionIfSatisfied(name);
                    this.workingManager.addActionIfSatisfied(edited);
                } else {
                    this.workingManager.removeActionIfViolated(name);
                    this.workingManager.addActionIfViolated(edited);
                }
                refreshAll();
            }));
    }

    private void renameAction(boolean satisfied) {
        String oldName = satisfied ? this.selectedSatisfiedAction : this.selectedViolatedAction;
        if (oldName == null) {
            return;
        }
        this.minecraft.setScreen(new TextPromptScreen(this, Component.translatable("fmod.misc.rename"),
            Component.translatable("fmod.rulegui.rule.name.prompt"), oldName, newName -> {
                boolean exists = satisfied ? this.workingManager.getRule().hasActionIfSatisfied(newName) : this.workingManager.getRule().hasActionIfViolated(newName);
                Component error = validateNewActionName(newName, exists);
                if (error == null) {
                    beforeMutate();
                    if (satisfied) {
                        this.workingManager.renameActionIfSatisfied(oldName, newName);
                        this.selectedSatisfiedAction = newName;
                    } else {
                        this.workingManager.renameActionIfViolated(oldName, newName);
                        this.selectedViolatedAction = newName;
                    }
                    refreshAll();
                } else {
                    this.statusMessage = error;
                }
                this.minecraft.setScreen(this);
            }));
    }

    private void removeAction(boolean satisfied) {
        String name = satisfied ? this.selectedSatisfiedAction : this.selectedViolatedAction;
        if (name == null) {
            return;
        }
        beforeMutate();
        if (satisfied) {
            this.workingManager.removeActionIfSatisfied(name);
            this.selectedSatisfiedAction = null;
        } else {
            this.workingManager.removeActionIfViolated(name);
            this.selectedViolatedAction = null;
        }
        refreshAll();
    }

    // ------------------------------------------------------------------
    // Commit / exit
    // ------------------------------------------------------------------

    private void save(boolean persist) {
        CustomRule copy = this.workingManager.getRule().copy();
        RuleEditorBridge.onClient(RuleEditorBridge.commitEdit(this.session.originalName, copy, persist), result -> {
            this.statusMessage = result.message;
            this.statusText.setMessage(this.statusMessage);
        });
    }

    private void exit() {
        if (this.parent instanceof RuleEditorScreen ruleEditorScreen) {
            ruleEditorScreen.refresh();
        }
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Background, then the lists (added via addWidget, not addRenderableWidget, so they aren't
        // auto-rendered by super.render()), then super.render() last so the actual renderable widgets
        // (buttons, labels, formula box) draw on top instead of being masked by the lists' full-width
        // fade above/below their own bounds, or by the dirt background.
        this.renderDirtBackground(context);
        this.extraList.render(context, mouseX, mouseY, delta);
        this.satisfiedList.render(context, mouseX, mouseY, delta);
        this.violatedList.render(context, mouseX, mouseY, delta);
        this.statusText.setMessage(this.statusMessage);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        exit();
    }

    /**
     * Minimal single-selection list of named rows, shared by all three sections (extra
     * conditions, satisfied actions, violated actions).
     */
    private static class NamedListWidget extends ObjectSelectionList<NamedListWidget.Row> {

        record NameLabel(String name, Component label) {
        }

        private final java.util.function.Supplier<String> selectedSupplier;
        private final java.util.function.Consumer<String> onSelectToggle;

        NamedListWidget(Minecraft client, int width, int height, int top, int bottom,
                java.util.function.Supplier<String> selectedSupplier, java.util.function.Consumer<String> onSelectToggle) {
            super(client, width, height, top, bottom, 18);
            this.selectedSupplier = selectedSupplier;
            this.onSelectToggle = onSelectToggle;
        }

        void setItems(List<NameLabel> items) {
            this.clearEntries();
            for (NameLabel item : items) {
                this.addEntry(new Row(item.name(), item.label()));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 8;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.x0 + this.width - 6;
        }

        class Row extends ObjectSelectionList.Entry<Row> {
            private final String name;
            private final Component label;

            Row(String name, Component label) {
                this.name = name;
                this.label = label;
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                if (this.name.equals(selectedSupplier.get())) {
                    context.fill(x - 2, y - 1, x + entryWidth + 2, y + entryHeight + 1, 0x552277FF);
                } else if (hovered) {
                    context.fill(x - 2, y - 1, x + entryWidth + 2, y + entryHeight + 1, 0x30FFFFFF);
                }
                context.drawString(Minecraft.getInstance().font, this.label, x + 2, y + (entryHeight - 9) / 2, 0xFFFFFF, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                onSelectToggle.accept(this.name);
                return true;
            }

            @Override
            public Component getNarration() {
                return this.label;
            }
        }
    }
}
