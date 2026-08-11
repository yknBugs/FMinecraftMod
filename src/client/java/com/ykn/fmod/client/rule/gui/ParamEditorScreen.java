/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.rule.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleEvent;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.tool.RuleRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Generic parameter-editing panel shared by both {@link SourceCondition} and {@link RuleAction},
 * via a small {@link Adapter} rather than two near-duplicate screens.
 *
 * <p>Reached one of two ways:
 * <ul>
 *   <li>{@link #forCreate} - the component doesn't exist yet. Rows are seeded from the type's
 *       {@link RequiredParamMetadata} (resolved without constructing anything).</li>
 *   <li>{@link #forEdit} - the component already exists; its current values seed the rows.</li>
 * </ul>
 * Either way, no {@code T} instance is built until Done is clicked, at which point exactly one is
 * built directly from name+type+values via {@link Adapter#build} - there's no intermediate
 * "blank" instance to construct-then-discard-the-shell-of. Done itself stays disabled
 * ({@link #tick()}) until every parameter row has a value, so a build attempt with missing data
 * can't happen.
 *
 * <p>Builds one {@link ParamWidgetFactory} row per declared parameter inside a scrollable
 * {@link ContainerObjectSelectionList} - so an arbitrary number of parameters never pushes the
 * Done/Cancel buttons off screen - and shows a header line naming the component, its type (with
 * a tooltip carrying the type's summary description), the owning rule, and the rule's event (with
 * a tooltip carrying the event's variable list). On Cancel, nothing the caller owns is ever touched.
 */
public final class ParamEditorScreen<T> extends Screen {

    /** 
     * Bridges {@link ParamEditorScreen} over either {@link SourceCondition} or {@link RuleAction}. 
     */
    public interface Adapter<T> {

        /**
         * Returns the current values of the component.
         */
        List<RuleParameter<?>> values(T component);

        /**
         * Returns the component's name.
         */
        String name(T component);

        /**
         * Returns the component's type.
         */
        String type(T component);

        /**
         *  Looks up a type's parameter shape without constructing any instance of it. 
         */
        RequiredParamMetadata metadataForType(String type);

        /** 
         * Builds the real, final instance directly from its name and values. 
         */
        T build(String type, String name, List<RuleParameter<?>> values);
    }

    public static Adapter<SourceCondition> conditionAdapter() {
        return new Adapter<SourceCondition>() {
            @Override
            public List<RuleParameter<?>> values(SourceCondition component) {
                return component.getParameterValues();
            }

            @Override
            public String name(SourceCondition component) {
                return component.getName();
            }

            @Override
            public String type(SourceCondition component) {
                return component.getType();
            }

            @Override
            public RequiredParamMetadata metadataForType(String type) {
                return RuleRegistry.getRequiredParamMetadata(type);
            }

            @Override
            public SourceCondition build(String type, String name, List<RuleParameter<?>> values) {
                return RuleRegistry.createCondition(type, name, values);
            }
        };
    }

    public static Adapter<RuleAction> actionAdapter() {
        return new Adapter<RuleAction>() {
            @Override
            public List<RuleParameter<?>> values(RuleAction component) {
                return component.getParameterValues();
            }

            @Override
            public String name(RuleAction component) {
                return component.getName();
            }

            @Override
            public String type(RuleAction component) {
                return component.getType();
            }

            @Override
            public RequiredParamMetadata metadataForType(String type) {
                return RuleRegistry.getRequiredParamMetadata(type);
            }

            @Override
            public RuleAction build(String type, String name, List<RuleParameter<?>> values) {
                return RuleRegistry.createRuleAction(type, name, values);
            }
        };
    }

    private final Screen parent;
    private final Adapter<T> adapter;
    private final String type;
    private final String componentName;
    private final RequiredParamMetadata metadata;
    private final List<RuleParameter<?>> initialValues;
    private final String ruleName;
    private final RuleEvent event;
    private final Consumer<T> onDone;
    private ParamList list;
    private Button doneButton;

    /**
     * Working copy of every row's current value, kept alive across re-{@link #init()} calls (which
     * {@link Minecraft#setScreen} triggers every time a picker sub-screen returns here) so that
     * picking a variable/constant - or any edit made in another row before opening a picker - is
     * never silently discarded by the rebuild. {@code null} until the first {@link #init()}.
     */
    private List<RuleParameter<?>> workingValues;

    /**
     * Parallel to {@link #workingValues}: each row's raw, possibly-invalid-or-half-entered widget
     * state (see {@link ParamWidgetFactory.ParamRow#captureRawState}), or {@code null} if the row
     * has no pending raw edits to restore. {@link #workingValues} alone can't survive a round trip
     * through a picker sub-screen faithfully - a half-typed "12." or a toggle flipped ON with an
     * empty box both collapse to a null {@link RuleParameter} in {@code readValue()}, since that's
     * all a {@code RuleParameter} can represent - so this restores the exact widget state instead
     * whenever it's present, falling back to {@link #workingValues} only for a row that has none
     * (its very first build, or the one row a picker just supplied a fresh value for).
     */
    private List<List<Object>> workingRawState;

    private ParamEditorScreen(Screen parent, Adapter<T> adapter, String type, String componentName,
            RequiredParamMetadata metadata, List<RuleParameter<?>> initialValues,
            String ruleName, RuleEvent event, Consumer<T> onDone) {
        super(Component.translatable("fmod.rulegui.param.title", componentName));
        this.parent = parent;
        this.adapter = adapter;
        this.type = type;
        this.componentName = componentName;
        this.metadata = metadata;
        this.initialValues = initialValues;
        this.ruleName = ruleName;
        this.event = event;
        this.onDone = onDone;
    }

    /**
     * Opens the editor for a brand-new {@code type}-typed component named {@code name}. No real
     * {@code T} instance is built until Done is clicked.
     */
    public static <T> ParamEditorScreen<T> forCreate(Screen parent, Adapter<T> adapter, String type, String name,
            String ruleName, RuleEvent event, Consumer<T> onDone) {
        RequiredParamMetadata metadata = adapter.metadataForType(type);
        List<RuleParameter<?>> initial = new ArrayList<>();
        for (RequiredParamMetadata.Entry entry : metadata.getArgumentList()) {
            initial.add(entry.defaultValue != null ? RuleParameter.ofConstant(entry.defaultValue) : RuleParameter.ofNull());
        }
        return new ParamEditorScreen<>(parent, adapter, type, name, metadata, initial, ruleName, event, onDone);
    }

    /** Opens the editor over the already-real {@code component}, seeding rows from its current values. */
    public static <T> ParamEditorScreen<T> forEdit(Screen parent, Adapter<T> adapter, T component,
            String ruleName, RuleEvent event, Consumer<T> onDone) {
        String type = adapter.type(component);
        RequiredParamMetadata metadata = adapter.metadataForType(type);
        return new ParamEditorScreen<>(parent, adapter, type, adapter.name(component), metadata,
            adapter.values(component), ruleName, event, onDone);
    }

    @Override
    protected void init() {
        int headerY = 14;

        Component titleText = Component.translatable("fmod.rulegui.param.header.title", this.componentName, this.type);
        StringWidget titleWidget = new StringWidget(15, headerY, this.font.width(titleText) + 4, 12, titleText, this.font).alignLeft();
        titleWidget.setTooltip(Tooltip.create(Component.translatable(this.metadata.getSummaryI18nKey())));
        this.addRenderableWidget(titleWidget);

        Component ruleText = Component.translatable("fmod.rulegui.param.header.rule", this.ruleName);
        StringWidget ruleWidget = new StringWidget((this.width - this.font.width(ruleText)) / 2 - 2, headerY, this.font.width(ruleText) + 4, 12, ruleText, this.font).alignCenter();
        this.addRenderableWidget(ruleWidget);

        Component eventText = Component.translatable("fmod.rulegui.rule.event", this.event.getType());
        StringWidget eventWidget = new StringWidget(this.width - this.font.width(eventText) - 15, headerY, this.font.width(eventText) + 4, 12, eventText, this.font).alignRight();
        eventWidget.setTooltip(Tooltip.create(this.event.render()));
        this.addRenderableWidget(eventWidget);

        int buttonHeight = 20;
        int bottomY = this.height - 8 - buttonHeight;
        int listTop = headerY + 18;
        int listBottom = bottomY - 6;
        int rowWidth = Math.min(this.width - 40, 500);

        this.list = new ParamList(this.minecraft, this.width, this.height, listTop, listBottom, ParamWidgetFactory.ROW_HEIGHT, rowWidth);
        List<RequiredParamMetadata.Entry> entries = this.metadata.getArgumentList();
        if (this.workingValues == null) {
            this.workingValues = new ArrayList<>();
            this.workingRawState = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) {
                this.workingValues.add(i < this.initialValues.size() && this.initialValues.get(i) != null
                    ? this.initialValues.get(i) : RuleParameter.ofNull());
                this.workingRawState.add(null);
            }
        }
        for (int i = 0; i < entries.size(); i++) {
            RequiredParamMetadata.Entry entry = entries.get(i);
            ParamWidgetFactory.ParamRow row = ParamWidgetFactory.build(this, i, entry, this.workingValues.get(i), this.event, rowWidth);
            List<Object> rawState = this.workingRawState.get(i);
            if (rawState != null) {
                row.restoreRawState(rawState);
            }
            this.list.addRow(new ParamEntry(row));
        }
        this.addWidget(this.list);

        this.doneButton = Button.builder(CommonComponents.GUI_DONE, b -> confirm())
            .pos(this.width / 2 - 105, bottomY).size(100, buttonHeight).build();
        this.addRenderableWidget(this.doneButton);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.minecraft.setScreen(this.parent))
            .pos(this.width / 2 + 5, bottomY).size(100, buttonHeight).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.doneButton != null) {
            this.doneButton.active = allRowsFilled();
        }
    }

    /**
     * Every row must have at least one enabled side, and every enabled side must actually be
     * valid - a toggle left ON with an unparsable/blank box doesn't count, even though it would
     * silently fall back to "not present" if read via {@link ParamWidgetFactory.ParamRow#readValue}.
     */
    private boolean allRowsFilled() {
        for (ParamEntry entry : this.list.children()) {
            if (!entry.row.isValid()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Snapshots every row's current state into {@link #workingValues}/{@link #workingRawState},
     * from the live widgets the user was just looking at. Called right before navigating to a
     * picker sub-screen, so that edits made in other rows - even half-typed or otherwise unparsable
     * ones - survive the {@link #init()} rebuild {@link Minecraft#setScreen} triggers when the
     * picker returns here.
     */
    void captureWorkingValues() {
        List<ParamEntry> children = this.list.children();
        for (int i = 0; i < children.size(); i++) {
            ParamWidgetFactory.ParamRow row = children.get(i).row;
            this.workingValues.set(i, row.readValue());
            this.workingRawState.set(i, row.captureRawState());
        }
    }

    RuleParameter<?> getWorkingValue(int index) {
        return this.workingValues.get(index);
    }

    /** 
     * Sets a row's value from a picker selection, clearing any pending raw edits it had. 
     */
    void setWorkingValue(int index, RuleParameter<?> value) {
        this.workingValues.set(index, value);
        this.workingRawState.set(index, null);
    }

    private void confirm() {
        List<RuleParameter<?>> collected = new ArrayList<>();
        for (ParamEntry entry : this.list.children()) {
            collected.add(entry.row.readValue());
        }
        T result = this.adapter.build(this.type, this.componentName, collected);
        this.onDone.accept(result);
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // this.list is added via addWidget (not addRenderableWidget) so it isn't auto-rendered by
        // super.render(); it must render first since AbstractSelectionList paints a full-width fade
        // above/below its own bounds that would otherwise mask the renderable widgets drawn by super.render().
        this.list.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private static final class ParamList extends ContainerObjectSelectionList<ParamEntry> {
        private final int rowWidth;

        ParamList(net.minecraft.client.Minecraft client, int width, int height, int top, int bottom, int itemHeight, int rowWidth) {
            super(client, width, height, top, bottom, itemHeight);
            this.rowWidth = rowWidth;
        }

        void addRow(ParamEntry entry) {
            this.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 5;
        }
    }

    private static final class ParamEntry extends ContainerObjectSelectionList.Entry<ParamEntry> {
        private final ParamWidgetFactory.ParamRow row;

        ParamEntry(ParamWidgetFactory.ParamRow row) {
            this.row = row;
        }

        @Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.row.reposition(x, y);
            for (AbstractWidget widget : this.row.widgets()) {
                widget.render(context, mouseX, mouseY, tickDelta);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.row.widgets();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }
}
