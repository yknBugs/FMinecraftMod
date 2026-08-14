/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.rule.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.core.RuleEvent;
import com.ykn.fmod.server.rule.core.RuleParameter;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import com.ykn.fmod.client.base.gui.OptionPickerScreen;

/**
 * Builds and reads back a single parameter-editing row for {@link ParamEditorScreen}: one row per
 * {@link RequiredParamMetadata.Entry}, dispatching the kind-specific constant widget(s) to
 * {@link ParamWidgetRegistry} - never switching on the concrete condition/action class - so no
 * per-component-type UI code is needed. The variable-picker half of each row (unrelated to which
 * {@link ParamKind} is in use) stays here.
 *
 * <p>Each row is: a name/info line, a "Variable: ON/OFF" line (toggle + textbox + Pick button), and
 * a "Constant: ON/OFF" line (toggle + kind-specific constant widget(s)) - so it's always visually
 * unambiguous which box is bound to which state. Toggling either switch off disables (greys out and
 * stops accepting input on) the widgets it governs, so it's equally unambiguous which box is
 * actually contributing to the row's value. Together the two toggles losslessly represent all four
 * {@link RuleParameter} states ({@code ofVariable}/{@code ofConstant}/{@code ofBoth}/{@code ofNull}).
 * {@link ParamKind#buildNode}/{@code extract} are never used here - this GUI bypasses Brigadier
 * entirely, so only {@link ParamKind#valueType()} and which named constant a kind <i>is</i> matter
 * for the widget switch below.
 *
 * <p>Rows live inside a {@code ContainerObjectSelectionList}, which repositions each entry's widgets
 * every frame as the list scrolls. Widgets are therefore built once at a relative (0,0) origin and
 * every widget's offset from that origin is captured at build time, so {@link ParamRow#reposition}
 * only ever needs to translate - never rebuild - the row.
 *
 * <p>Every "Pick" button hands its selection back through {@link ParamEditorScreen#setWorkingValue}
 * rather than writing into a textbox directly: {@link Minecraft#setScreen} re-runs
 * {@link ParamEditorScreen#init()} every time it switches back from the picker sub-screen, which
 * discards and rebuilds every row's widgets from scratch, so anything written only into a widget
 * that's about to be discarded would be silently lost. {@link ParamEditorScreen#captureWorkingValues()}
 * is called first so edits made in other rows before opening the picker survive the same rebuild.
 */
public final class ParamWidgetFactory {

    // label+info(12) + gap(4) + variable line(18) + gap(6) + constant line(18) = 58; padded to 64
    // so consecutive rows in the list don't touch.
    public static final int ROW_HEIGHT = 64;

    private static final int TOGGLE_WIDTH = 90;
    private static final int PICK_WIDTH = 50;
    private static final int GAP = 4;

    /** 
     * One built parameter row, call {@link #readValue()} to collect its current value. 
     */
    public static final class ParamRow {
        private final CycleButton<Boolean> useConstantToggle;
        private final CycleButton<Boolean> bindVariableToggle;
        private final EditBox variableBox;
        private final Supplier<Object> constantReader;
        private final List<PositionedWidget> widgets;
        private final List<AbstractWidget> variableWidgets;
        private final List<AbstractWidget> constantWidgets;

        private record PositionedWidget(AbstractWidget widget, int offsetX, int offsetY) {}

        private ParamRow(CycleButton<Boolean> useConstantToggle, CycleButton<Boolean> bindVariableToggle,
                EditBox variableBox, Supplier<Object> constantReader, List<PositionedWidget> widgets,
                List<AbstractWidget> variableWidgets, List<AbstractWidget> constantWidgets) {
            this.useConstantToggle = useConstantToggle;
            this.bindVariableToggle = bindVariableToggle;
            this.variableBox = variableBox;
            this.constantReader = constantReader;
            this.widgets = widgets;
            this.variableWidgets = variableWidgets;
            this.constantWidgets = constantWidgets;
        }

        /** 
         * All widgets belonging to this row, for a {@code ContainerObjectSelectionList.Entry#children()}. 
         */
        public List<AbstractWidget> widgets() {
            List<AbstractWidget> list = new ArrayList<>(this.widgets.size());
            for (PositionedWidget positioned : this.widgets) {
                list.add(positioned.widget());
            }
            return list;
        }

        /** 
         * Translates every widget in this row so its origin sits at ({@code x}, {@code y}). 
         */
        public void reposition(int x, int y) {
            for (PositionedWidget positioned : this.widgets) {
                positioned.widget().setX(x + positioned.offsetX());
                positioned.widget().setY(y + positioned.offsetY());
            }
        }

        /**
         * Snapshots every stateful widget's raw, possibly-invalid-or-half-entered value (an
         * {@link EditBox}'s text, a {@code CycleButton<Boolean>}'s boolean) in a fixed order -
         * unlike {@link #readValue()}, which collapses an unparsable/half-typed constant to
         * {@code null} (it has to: a {@link RuleParameter} can only hold a fully-parsed value),
         * this preserves exactly what the user typed so it can be restored verbatim by
         * {@link #restoreRawState} after a round trip through a picker sub-screen.
         */
        public List<Object> captureRawState() {
            List<Object> state = new ArrayList<>();
            for (PositionedWidget positioned : this.widgets) {
                AbstractWidget widget = positioned.widget();
                if (widget instanceof EditBox box) {
                    state.add(box.getValue());
                } else if (widget instanceof CycleButton<?> cycle) {
                    state.add(cycle.getValue());
                }
            }
            return state;
        }

        /**
         * Restores a snapshot taken by {@link #captureRawState}, in the same widget order.
         * {@code CycleButton.setValue} doesn't fire the change listener that normally
         * enables/disables the variable/constant widgets it governs, so that's re-applied
         * afterward from the restored toggle values.
         */
        public void restoreRawState(List<Object> state) {
            int i = 0;
            for (PositionedWidget positioned : this.widgets) {
                AbstractWidget widget = positioned.widget();
                if (widget instanceof EditBox box) {
                    box.setValue((String) state.get(i++));
                } else if (widget instanceof CycleButton<?> cycle) {
                    @SuppressWarnings("unchecked")
                    CycleButton<Object> typed = (CycleButton<Object>) cycle;
                    typed.setValue(state.get(i++));
                }
            }
            setAllEnabled(this.variableWidgets, this.bindVariableToggle.getValue());
            setAllEnabled(this.constantWidgets, this.useConstantToggle.getValue());
        }

        /**
         * Whether this row is ready to contribute to a submitted component: every <em>enabled</em>
         * side must actually parse - a toggle left ON with a blank/unparsable box is not the same
         * as that side being off, even though {@link #readValue()} has to collapse both to "not
         * present" (a {@link RuleParameter} can't distinguish "off" from "on but invalid"). At
         * least one side must be enabled, same as before.
         */
        public boolean isValid() {
            boolean varOn = this.bindVariableToggle.getValue();
            boolean varOk = !varOn || !this.variableBox.getValue().isBlank();
            boolean constOn = this.useConstantToggle.getValue();
            boolean constOk = !constOn || this.constantReader.get() != null;
            return varOk && constOk && (varOn || constOn);
        }

        /** 
         * Reads the row's current state back into a {@link RuleParameter}. 
         */
        public RuleParameter<Object> readValue() {
            String variableName = this.bindVariableToggle.getValue() && !this.variableBox.getValue().isBlank()
                ? this.variableBox.getValue().trim() : null;
            Object constantValue = this.useConstantToggle.getValue() ? this.constantReader.get() : null;
            if (variableName != null && constantValue != null) {
                return RuleParameter.ofBoth(variableName, constantValue);
            } else if (variableName != null) {
                return RuleParameter.ofVariable(variableName);
            } else if (constantValue != null) {
                return RuleParameter.ofConstant(constantValue);
            } else {
                return RuleParameter.ofNull();
            }
        }
    }

    /** 
     * Enables/disables a widget for interaction, dimming it the way vanilla disabled widgets render. 
     */
    private static void setEnabled(AbstractWidget widget, boolean enabled) {
        widget.active = enabled;
        if (widget instanceof EditBox box) {
            box.setEditable(enabled);
        }
    }

    private static void setAllEnabled(List<AbstractWidget> widgets, boolean enabled) {
        for (AbstractWidget widget : widgets) {
            setEnabled(widget, enabled);
        }
    }

    /**
     * Builds one parameter row, {@link #ROW_HEIGHT} pixels tall and {@code width} pixels wide,
     * relative to its own (0,0) origin - the caller repositions it every frame via
     * {@link ParamRow#reposition}.
     *
     * @param owner   the screen this row belongs to, used to persist picker selections and to 
     *                navigate to picker sub-screens and back
     * @param index   this row's position among the component's declared parameters, i.e. its index
     *                into {@link ParamEditorScreen#getWorkingValue}
     * @param entry   the parameter's declared shape
     * @param initial the parameter's current value, or {@code null} for a brand-new parameter
     * @param event   the rule's current event, whose declared variables seed the variable picker
     * @param width   total row width
     */
    public static ParamRow build(ParamEditorScreen<?> owner, int index, RequiredParamMetadata.Entry entry,
            RuleParameter<?> initial, RuleEvent event, int width) {
        Minecraft minecraft = Minecraft.getInstance();
        if (initial == null) {
            initial = RuleParameter.ofNull();
        }

        List<ParamRow.PositionedWidget> collected = new ArrayList<>();
        Consumer<AbstractWidget> sink = w -> collected.add(new ParamRow.PositionedWidget(w, w.getX(), w.getY()));

        StringWidget label = new StringWidget(0, 0, width - 18, 12, Component.translatable(entry.nameI18nKey), minecraft.font);
        sink.accept(label);
        Button infoButton = Button.builder(Component.literal("?"), b -> {
        }).pos(width - 14, -1).size(14, 14).build();
        infoButton.setTooltip(Tooltip.create(Component.translatable(entry.descI18nKey)));
        sink.accept(infoButton);

        int varY = 16;
        int varBoxX = TOGGLE_WIDTH + GAP;
        int varBoxWidth = width - varBoxX - GAP - PICK_WIDTH;
        EditBox variableBox = new EditBox(minecraft.font, varBoxX, varY, varBoxWidth, 18, Component.empty());
        variableBox.setValue(initial.getVariableName() == null ? "" : initial.getVariableName());
        variableBox.setMaxLength(256);
        variableBox.setHint(Component.translatable("fmod.rulegui.param.hint.variable").withStyle(ChatFormatting.DARK_GRAY));

        Button pickVarButton = Button.builder(Component.translatable("fmod.rulegui.param.pick"), b -> {
            owner.captureWorkingValues();
            openVariablePicker(owner, index, event, entry.kind);
        }).pos(width - PICK_WIDTH, varY).size(PICK_WIDTH, 18).build();

        List<AbstractWidget> variableWidgets = List.of(variableBox, pickVarButton);
        boolean initialHasVar = initial.getVariableName() != null;
        CycleButton<Boolean> bindVariableToggle = CycleButton.onOffBuilder(initialHasVar)
            .create(0, varY, TOGGLE_WIDTH, 18, Component.translatable("fmod.misc.var"),
                (button, value) -> setAllEnabled(variableWidgets, value));
        setAllEnabled(variableWidgets, initialHasVar);
        sink.accept(bindVariableToggle);
        sink.accept(variableBox);
        sink.accept(pickVarButton);

        int constY = varY + 24;
        int constX = TOGGLE_WIDTH + GAP;
        int constWidth = width - constX;
        List<AbstractWidget> constantWidgets = new ArrayList<>();
        Consumer<AbstractWidget> constSink = w -> {
            sink.accept(w);
            constantWidgets.add(w);
        };
        Supplier<Object> constantReader = buildConstantWidgets(owner, index, entry, initial.getConstantValue(), constX, constY, constWidth, constSink);

        boolean initialHasConst = initial.getConstantValue() != null;
        CycleButton<Boolean> useConstantToggle = CycleButton.onOffBuilder(initialHasConst)
            .create(0, constY, TOGGLE_WIDTH, 18, Component.translatable("fmod.misc.const"),
                (button, value) -> setAllEnabled(constantWidgets, value));
        setAllEnabled(constantWidgets, initialHasConst);
        sink.accept(useConstantToggle);

        return new ParamRow(useConstantToggle, bindVariableToggle, variableBox, constantReader, collected,
            variableWidgets, constantWidgets);
    }

    private static void openVariablePicker(ParamEditorScreen<?> owner, int index, RuleEvent event, ParamKind<?> kind) {
        List<OptionPickerScreen.Option> options = new ArrayList<>();
        for (Map.Entry<String, Class<?>> variable : event.variablesType().entrySet()) {
            if (kind.valueType().isAssignableFrom(variable.getValue())) {
                String variableName = variable.getKey();
                options.add(new OptionPickerScreen.Option(Component.literal(variableName), null, () -> {
                    RuleParameter<?> current = owner.getWorkingValue(index);
                    Object constant = current.getConstantValue();
                    owner.setWorkingValue(index, constant != null
                        ? RuleParameter.ofBoth(variableName, constant) : RuleParameter.ofVariable(variableName));
                    Minecraft.getInstance().setScreen(owner);
                }));
            }
        }
        Minecraft.getInstance().setScreen(new OptionPickerScreen(owner, Component.translatable("fmod.rulegui.param.pickvariable"), options));
    }

    private static Supplier<Object> buildConstantWidgets(ParamEditorScreen<?> owner, int index, RequiredParamMetadata.Entry entry,
            Object initialConstant, int x, int y, int width, Consumer<AbstractWidget> widgetSink) {
        return ParamWidgetRegistry.build(owner, index, entry, initialConstant, x, y, width, widgetSink);
    }
}
