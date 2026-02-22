/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.base.gui;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.slf4j.LoggerFactory;

import com.ykn.fmod.server.base.config.ConfigEntry;
import com.ykn.fmod.server.base.config.ConfigReader;
import com.ykn.fmod.server.base.config.ServerConfigRegistry;
import com.ykn.fmod.server.base.util.PlayerMessageType;
import com.ykn.fmod.server.base.util.ServerMessageType;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * The client-side options screen that renders interactive widgets for every config entry
 * registered via {@link ServerConfigRegistry}.
 *
 * <p>The screen contains a scrollable {@link ConfigWidget} list in the centre and a
 * "Done" button at the bottom. The config is persisted (via {@link Util#saveServerConfig()})
 * whenever the screen is closed, removed, or the Done button is pressed.</p>
 *
 * <p>Each config entry is displayed according to its {@link ConfigEntry#type()}:</p>
 * <ul>
 *   <li>{@link ConfigEntry.ConfigType#DOUBLE} / {@link ConfigEntry.ConfigType#INTEGER}
 *       – a {@link net.minecraft.client.gui.widget.SliderWidget}</li>
 *   <li>{@link ConfigEntry.ConfigType#STRING}
 *       – a {@link net.minecraft.client.gui.widget.TextFieldWidget}</li>
 *   <li>{@link ConfigEntry.ConfigType#BOOLEAN}
 *       – a toggle {@link net.minecraft.client.gui.widget.ButtonWidget}</li>
 *   <li>{@link ConfigEntry.ConfigType#SERVERMESSAGE} / {@link ConfigEntry.ConfigType#PLAYERMESSAGE}
 *       – three cycle-buttons for main location, other location, and receiver</li>
 * </ul>
 *
 * @see ServerConfigRegistry
 * @see ConfigEntry
 */
public class OptionScreen extends Screen {

    private final Screen parent;
    private ConfigWidget configWidget;
    
    public OptionScreen(Screen parent) {
        super(Text.translatable("fmod.options.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        this.configWidget = new ConfigWidget(this.client, this.width, this.height - 80, 40, this.height - 40);
        this.addSelectableChild(this.configWidget);

        this.addDrawableChild(
            ButtonWidget.builder(ScreenTexts.DONE, button -> {
                Util.saveServerConfig();
                this.client.setScreen(this.parent);
            }).position(this.width / 2 - 100, this.height - 30).size(200, 20).build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        this.configWidget.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xffffff);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        Util.saveServerConfig();
        super.removed();
    }

    @Override
    public void close() {
        Util.saveServerConfig();
        this.client.setScreen(this.parent);
    }

    /**
     * Scrollable list widget that builds and renders one row per registered
     * {@link ConfigEntry}-annotated config field.
     *
     * <p>Each row consists of a left-aligned label ({@link net.minecraft.client.gui.widget.TextWidget})
     * with a tooltip containing the i18n hint text, and a right-aligned interactive
     * control widget whose type depends on {@link ConfigEntry#type()}.</p>
     */
    private class ConfigWidget extends ElementListWidget<ConfigWidget.Entry> {

        public ConfigWidget(MinecraftClient client, int width, int height, int top, int bottom) {
            // 630 234 40 274
            super(client, width, height, top, bottom, 24);
            // Copyright Info
            this.addEntry(new TextHintEntry(
                Text.translatable("fmod.misc.version", Util.getMinecraftVersion(), Util.getModVersion(), Util.getModAuthors()),
                Text.translatable("fmod.options.tip")
            ));
            // Config entries
            this.buildConfigEntry();
        };

        @Override
        public int getRowWidth() {
            return this.width - 40;
        }

        @Override
        protected int getScrollbarPositionX() {
            return this.width - 5;
        }

        /**
         * Creates and adds a slider-based config row for numeric ({@link ConfigEntry.ConfigType#DOUBLE}
         * or {@link ConfigEntry.ConfigType#INTEGER}) config entries.
         *
         * <p>If the entry is not editable ({@link ConfigEntry#isEditableInUI()} is {@code false})
         * a disabled button showing the current display value is rendered instead of a slider.</p>
         *
         * <p>The slider position maps to the configured value range via either the custom
         * {@link ConfigEntry#toSliderValue()} / {@link ConfigEntry#fromSliderValue()} methods
         * (if specified) or a simple linear interpolation between {@code min} and {@code max}.</p>
         *
         * @param configCodeEntry    the code-entry name used to read/write the value
         * @param configAnnotation   the {@link ConfigEntry} annotation of the field
         * @param configInstance     the {@link ConfigReader} that owns this field
         * @param i18nTitleKey       translation key for the row label
         * @param i18nHintKey        translation key for the tooltip hint text
         * @param isEditable         whether the slider should be interactive
         * @param rawValue           the current raw config value (used for display fallback)
         * @param min                the minimum numeric value for linear interpolation
         * @param max                the maximum numeric value for linear interpolation
         * @param configValueAsDouble the current config value cast to {@code double}
         * @param sliderToValue      a function that converts a normalised slider position
         *                           ({@code [0.0, 1.0]}) to the actual config value;
         *                           used as the fallback when no {@link ConfigEntry#fromSliderValue()}
         *                           method is available
         */
        private void buildNumericSliderEntry(
                String configCodeEntry,
                ConfigEntry configAnnotation,
                ConfigReader configInstance,
                String i18nTitleKey,
                String i18nHintKey,
                boolean isEditable,
                Object rawValue,
                double min,
                double max,
                double configValueAsDouble,
                Function<Double, Object> sliderToValue) {
            if (!isEditable) {
                Text displayText = ServerConfigRegistry.getDisplayValue(configCodeEntry, rawValue);
                ButtonWidget button = ButtonWidget.builder(displayText, btn -> {}).size(200, 20).build();
                button.active = false;
                if (!configAnnotation.notEditableReason().isEmpty()) {
                    button.setTooltip(Tooltip.of(Text.translatable(configAnnotation.notEditableReason())));
                }
                this.addEntry(new ButtonConfigEntry(
                    button,
                    Text.translatable(i18nTitleKey),
                    Text.translatable(i18nHintKey).append("\n").append(Text.translatable("fmod.options.link", "/f options " + configAnnotation.commandEntry()))
                ));
                return;
            }

            double sliderInitValue = 0.0;
            if (configAnnotation.toSliderValue().isEmpty()) {
                sliderInitValue = (max == min) ? 0.0 : (configValueAsDouble - min) / (max - min);
            } else {
                try {
                    final Method toSliderValueMethod = configInstance.getClass().getDeclaredMethod(configAnnotation.toSliderValue(), rawValue.getClass());
                    toSliderValueMethod.setAccessible(true);
                    sliderInitValue = (double) toSliderValueMethod.invoke(configInstance, rawValue);
                } catch (Exception e) {
                    LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Failed to invoke toSliderValue method for " + configCodeEntry, e);
                    sliderInitValue = (max == min) ? 0.0 : (configValueAsDouble - min) / (max - min);
                }
            }
            sliderInitValue = Math.max(0.0, Math.min(1.0, sliderInitValue));

            final String fromSliderMethod = configAnnotation.fromSliderValue();

            SliderWidget slider = new SliderWidget(0, 0, 200, 20,
                ServerConfigRegistry.getDisplayValue(configCodeEntry, rawValue),
                sliderInitValue
            ) {
                private Object computeNewValue() {
                    if (fromSliderMethod.isEmpty()) {
                        return sliderToValue.apply(this.value);
                    } else {
                        try {
                            Method method = configInstance.getClass().getDeclaredMethod(fromSliderMethod, double.class);
                            method.setAccessible(true);
                            return method.invoke(configInstance, this.value);
                        } catch (Exception e) {
                            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Failed to invoke fromSliderValue method for " + configCodeEntry, e);
                            return sliderToValue.apply(this.value);
                        }
                    }
                }

                @Override
                protected void updateMessage() {
                    this.setMessage(ServerConfigRegistry.getDisplayValue(configCodeEntry, computeNewValue()));
                }

                @Override
                protected void applyValue() {
                    ServerConfigRegistry.setValue(configCodeEntry, computeNewValue());
                }
            };

            this.addEntry(new NumberConfigEntry(
                slider,
                Text.translatable(i18nTitleKey),
                Text.translatable(i18nHintKey).append("\n").append(Text.translatable("fmod.options.link", "/f options " + configAnnotation.commandEntry()))
            ));
        }

        /**
         * Iterates over all registered config entries and adds a row for each one
         * by delegating to {@link #buildConfigEntry(String)}.
         */
        public void buildConfigEntry() {
            for (String configCodeEntry : ServerConfigRegistry.getConfigAnnotations().keySet()) {
                buildConfigEntry(configCodeEntry);
            }
        }

        /**
         * Adds a single config row for the entry identified by {@code configCodeEntry}.
         *
         * <p>The row type is chosen according to {@link ConfigEntry#type()}. Unknown types
         * (and entries that are missing from either registry map) are silently skipped.
         * Any unexpected exception during row construction is caught and logged.</p>
         *
         * @param configCodeEntry the code-entry name of the config entry to render
         */
        public void buildConfigEntry(String configCodeEntry) {
            try {
                final ConfigEntry configAnnotation = ServerConfigRegistry.getConfigAnnotations().get(configCodeEntry);
                final ConfigReader configInstance = ServerConfigRegistry.getConfigInstances().get(configCodeEntry);
                if (configAnnotation == null || configInstance == null) {
                    return;
                }
                final String i18nEntry = configAnnotation.i18nEntry().isEmpty() ? configCodeEntry : configAnnotation.i18nEntry();
                final String i18nTitleKey = "fmod.options." + i18nEntry;
                final String i18nHintKey = "fmod.options.hint." + i18nEntry;
                final Object rawValue = ServerConfigRegistry.getValue(configCodeEntry);
                final boolean isEditable = configAnnotation.isEditableInUI();

                switch (configAnnotation.type()) {
                    case DOUBLE:
                        {
                            final double min = configAnnotation.minSliderDouble();
                            final double max = configAnnotation.maxSliderDouble();
                            double configValue = (rawValue instanceof Double) ? (double) rawValue : 0.0;
                            buildNumericSliderEntry(
                                configCodeEntry, configAnnotation, configInstance,
                                i18nTitleKey, i18nHintKey, isEditable, rawValue,
                                min, max, configValue,
                                v -> v * (max - min) + min
                            );
                        }
                        return;
                    case INTEGER:
                        {
                            final int min = configAnnotation.minSliderInt();
                            final int max = configAnnotation.maxSliderInt();
                            int configValue = (rawValue instanceof Integer) ? (int) rawValue : 0;
                            buildNumericSliderEntry(
                                configCodeEntry, configAnnotation, configInstance,
                                i18nTitleKey, i18nHintKey, isEditable, rawValue,
                                min, max, configValue,
                                v -> (int) Math.round(v * (max - min) + min)
                            );
                        }
                        return;
                    case STRING:
                        {
                            String configValue = ServerConfigRegistry.getDisplayValue(configCodeEntry, rawValue).getString();
                            final int maxLength = configAnnotation.maxStringLength();

                            TextFieldWidget textField = new TextFieldWidget(client.textRenderer, 0, 0, 200, 20, Text.empty());
                            textField.setMaxLength(maxLength);
                            textField.setEditable(isEditable);
                            textField.setText(configValue);
                            if (isEditable == false && configAnnotation.notEditableReason().isEmpty() == false) {
                                textField.setTooltip(Tooltip.of(Text.translatable(configAnnotation.notEditableReason())));
                            }
                            textField.setChangedListener(s -> ServerConfigRegistry.setValue(configCodeEntry, s));
                            this.addEntry(new StringConfigEntry(
                                textField,
                                Text.translatable(i18nTitleKey),
                                Text.translatable(i18nHintKey).append("\n").append(Text.translatable("fmod.options.link", "/f options " + configAnnotation.commandEntry()))
                            ));
                        }
                        return;
                    case BOOLEAN:
                        {
                            Text displayText = ServerConfigRegistry.getDisplayValue(configCodeEntry, rawValue);
                            ButtonWidget button = ButtonWidget.builder(displayText, btn -> {
                                Object currentValue = ServerConfigRegistry.getValue(configCodeEntry);
                                boolean newValue = false;
                                if (currentValue instanceof Boolean) {
                                    newValue = (boolean) currentValue;
                                    newValue = !newValue;
                                }
                                ServerConfigRegistry.setValue(configCodeEntry, newValue);
                                btn.setMessage(ServerConfigRegistry.getDisplayValue(configCodeEntry, newValue));
                            }).size(200, 20).build();
                            button.active = isEditable;
                            if (isEditable == false && configAnnotation.notEditableReason().isEmpty() == false) {
                                button.setTooltip(Tooltip.of(Text.translatable(configAnnotation.notEditableReason())));
                            }
                            this.addEntry(new ButtonConfigEntry(
                                button,
                                Text.translatable(i18nTitleKey),
                                Text.translatable(i18nHintKey).append("\n").append(Text.translatable("fmod.options.link", "/f options " + configAnnotation.commandEntry()))
                            ));
                        }
                        return;
                    case SERVERMESSAGE:
                        {
                            if (rawValue instanceof ServerMessageType == false) {
                                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Config entry " + configCodeEntry + " is annotated as SERVERMESSAGE but the value is not of type ServerMessageType");
                                return;
                            }
                            ServerMessageType messageType = (ServerMessageType) rawValue;
                            ButtonWidget mainLocationButton = ButtonWidget.builder(ServerMessageType.getMessageLocationI18n(messageType.mainPlayerLocation), btn -> {
                                final List<Enum<?>> values = Arrays.asList(ServerMessageType.Location.values());
                                ServerMessageType currentValue = (ServerMessageType) ServerConfigRegistry.getValue(configCodeEntry);
                                int currentIndex = values.indexOf(currentValue.mainPlayerLocation);
                                int nextIndex = (currentIndex + 1) % values.size();
                                ServerMessageType.Location newLocation = (ServerMessageType.Location) values.get(nextIndex);
                                ServerConfigRegistry.setValue(configCodeEntry, currentValue.updateMain(newLocation));
                                btn.setMessage(ServerMessageType.getMessageLocationI18n(newLocation));
                            }).size(60, 20).build();
                            ButtonWidget otherLocationButton = ButtonWidget.builder(ServerMessageType.getMessageLocationI18n(messageType.otherPlayerLocation), btn -> {
                                final List<Enum<?>> values = Arrays.asList(ServerMessageType.Location.values());
                                ServerMessageType currentValue = (ServerMessageType) ServerConfigRegistry.getValue(configCodeEntry);
                                int currentIndex = values.indexOf(currentValue.otherPlayerLocation);
                                int nextIndex = (currentIndex + 1) % values.size();
                                ServerMessageType.Location newLocation = (ServerMessageType.Location) values.get(nextIndex);
                                ServerConfigRegistry.setValue(configCodeEntry, currentValue.updateOther(newLocation));
                                btn.setMessage(ServerMessageType.getMessageLocationI18n(newLocation));
                            }).size(60, 20).build();
                            ButtonWidget receiverButton = ButtonWidget.builder(ServerMessageType.getMessageReceiverI18n(messageType.receiver), btn -> {
                                final List<Enum<?>> values = Arrays.asList(ServerMessageType.Receiver.values());
                                ServerMessageType currentValue = (ServerMessageType) ServerConfigRegistry.getValue(configCodeEntry);
                                int currentIndex = values.indexOf(currentValue.receiver);
                                int nextIndex = (currentIndex + 1) % values.size();
                                ServerMessageType.Receiver newReceiver = (ServerMessageType.Receiver) values.get(nextIndex);
                                ServerConfigRegistry.setValue(configCodeEntry, currentValue.updateReceiver(newReceiver));
                                btn.setMessage(ServerMessageType.getMessageReceiverI18n(newReceiver));
                            }).size(60, 20).build();
                            mainLocationButton.active = isEditable;
                            otherLocationButton.active = isEditable;
                            receiverButton.active = isEditable;
                            if (isEditable == false && configAnnotation.notEditableReason().isEmpty() == false) {
                                Tooltip tooltip = Tooltip.of(Text.translatable(configAnnotation.notEditableReason()));
                                mainLocationButton.setTooltip(tooltip);
                                otherLocationButton.setTooltip(tooltip);
                                receiverButton.setTooltip(tooltip);
                            } else {
                                mainLocationButton.setTooltip(Tooltip.of(Text.translatable("fmod.options.message.main")));
                                otherLocationButton.setTooltip(Tooltip.of(Text.translatable("fmod.options.message.other")));
                                receiverButton.setTooltip(Tooltip.of(Text.translatable("fmod.options.message.receiver")));
                            }
                            this.addEntry(new MessageConfigEntry(
                                mainLocationButton,
                                otherLocationButton,
                                receiverButton,
                                Text.translatable(i18nTitleKey),
                                Text.translatable(i18nHintKey).append("\n").append(Text.translatable("fmod.options.link", "/f options " + configAnnotation.commandEntry()))
                            ));
                        }
                        return;
                    case PLAYERMESSAGE:
                        {
                            if (rawValue instanceof PlayerMessageType == false) {
                                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Config entry " + configCodeEntry + " is annotated as PLAYERMESSAGE but the value is not of type PlayerMessageType");
                                return;
                            }
                            PlayerMessageType messageType = (PlayerMessageType) rawValue;
                            ButtonWidget mainLocationButton = ButtonWidget.builder(PlayerMessageType.getMessageLocationI18n(messageType.mainPlayerLocation), btn -> {
                                final List<Enum<?>> values = Arrays.asList(PlayerMessageType.Location.values());
                                PlayerMessageType currentValue = (PlayerMessageType) ServerConfigRegistry.getValue(configCodeEntry);
                                int currentIndex = values.indexOf(currentValue.mainPlayerLocation);
                                int nextIndex = (currentIndex + 1) % values.size();
                                PlayerMessageType.Location newLocation = (PlayerMessageType.Location) values.get(nextIndex);
                                ServerConfigRegistry.setValue(configCodeEntry, currentValue.updateMain(newLocation));
                                btn.setMessage(PlayerMessageType.getMessageLocationI18n(newLocation));
                            }).size(60, 20).build();
                            ButtonWidget otherLocationButton = ButtonWidget.builder(PlayerMessageType.getMessageLocationI18n(messageType.otherPlayerLocation), btn -> {
                                final List<Enum<?>> values = Arrays.asList(PlayerMessageType.Location.values());
                                PlayerMessageType currentValue = (PlayerMessageType) ServerConfigRegistry.getValue(configCodeEntry);
                                int currentIndex = values.indexOf(currentValue.otherPlayerLocation);
                                int nextIndex = (currentIndex + 1) % values.size();
                                PlayerMessageType.Location newLocation = (PlayerMessageType.Location) values.get(nextIndex);
                                ServerConfigRegistry.setValue(configCodeEntry, currentValue.updateOther(newLocation));
                                btn.setMessage(PlayerMessageType.getMessageLocationI18n(newLocation));
                            }).size(60, 20).build();
                            ButtonWidget receiverButton = ButtonWidget.builder(PlayerMessageType.getMessageReceiverI18n(messageType.receiver), btn -> {
                                final List<Enum<?>> values = Arrays.asList(PlayerMessageType.Receiver.values());
                                PlayerMessageType currentValue = (PlayerMessageType) ServerConfigRegistry.getValue(configCodeEntry);
                                int currentIndex = values.indexOf(currentValue.receiver);
                                int nextIndex = (currentIndex + 1) % values.size();
                                PlayerMessageType.Receiver newReceiver = (PlayerMessageType.Receiver) values.get(nextIndex);
                                ServerConfigRegistry.setValue(configCodeEntry, currentValue.updateReceiver(newReceiver));
                                btn.setMessage(PlayerMessageType.getMessageReceiverI18n(newReceiver));
                            }).size(60, 20).build();
                            mainLocationButton.active = isEditable;
                            otherLocationButton.active = isEditable;
                            receiverButton.active = isEditable;
                            if (isEditable == false && configAnnotation.notEditableReason().isEmpty() == false) {
                                Tooltip tooltip = Tooltip.of(Text.translatable(configAnnotation.notEditableReason()));
                                mainLocationButton.setTooltip(tooltip);
                                receiverButton.setTooltip(tooltip);
                                otherLocationButton.setTooltip(tooltip);
                            } else {
                                mainLocationButton.setTooltip(Tooltip.of(Text.translatable("fmod.options.message.main")));
                                otherLocationButton.setTooltip(Tooltip.of(Text.translatable("fmod.options.message.other")));
                                receiverButton.setTooltip(Tooltip.of(Text.translatable("fmod.options.message.receiver")));
                            }
                            this.addEntry(new MessageConfigEntry(
                                mainLocationButton,
                                otherLocationButton,
                                receiverButton,
                                Text.translatable(i18nTitleKey),
                                Text.translatable(i18nHintKey).append("\n").append(Text.translatable("fmod.options.link", "/f options " + configAnnotation.commandEntry()))
                            ));
                        }
                        return;
                    default:
                        return;
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Failed to build config entry for " + configCodeEntry, e);
            }
        }
    
        /**
         * Base class for all scrollable list entries in {@link ConfigWidget}.
         * Subclasses provide the concrete rendering and child-widget implementations.
         */
        abstract static class Entry extends ElementListWidget.Entry<Entry> {}
    
        /**
         * A read-only text row used for the version/tip header at the top of the list.
         * The text is left-aligned and carries a tooltip with additional information.
         */
        private class TextHintEntry extends Entry {
            private final TextWidget textWidget;

            /**
             * Constructs a {@code TextHintEntry}.
             *
             * @param text the primary text to display in the row
             * @param tips the tooltip text shown on hover
             */
            public TextHintEntry(Text text, Text tips) {
                this.textWidget = new TextWidget(0, 0, 200, 20, text, client.textRenderer);
                this.textWidget.alignLeft();
                this.textWidget.setTooltip(Tooltip.of(tips));
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                textWidget.setX(x);
                textWidget.setY(y);
                textWidget.render(context, mouseX, mouseY, tickDelta);
            }
    
            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of(textWidget);
            }

            @Override
            public List<? extends Element> children() {
                return List.of(textWidget);
            }
        }

        /**
         * A config row backed by a {@link ButtonWidget}; used for
         * {@link ConfigEntry.ConfigType#BOOLEAN} entries and for non-editable numeric entries.
         *
         * <p>The label is rendered on the left and the button on the right.</p>
         */
        private class ButtonConfigEntry extends Entry {
            private final ButtonWidget button;
            private final TextWidget textWidget;

            /**
             * Constructs a {@code ButtonConfigEntry}.
             *
             * @param button the button widget to display on the right side of the row
             * @param text   the label text shown on the left side
             * @param hint   the tooltip shown when hovering over the label
             */
            ButtonConfigEntry(ButtonWidget button, Text text, Text hint) {
                this.button = button;
                this.textWidget = new TextWidget(0, 0, 200, 20, text, client.textRenderer);
                this.textWidget.alignLeft();
                this.textWidget.setTooltip(Tooltip.of(hint));
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                textWidget.setX(x);
                textWidget.setY(y);
                textWidget.render(context, mouseX, mouseY, tickDelta);
                button.setX(x + entryWidth - button.getWidth());
                button.setY(y);
                button.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of(button, textWidget);
            }

            @Override
            public List<? extends Element> children() {
                return List.of(button, textWidget);
            }
        }

        /**
         * A config row backed by three {@link ButtonWidget}s; used for
         * {@link ConfigEntry.ConfigType#SERVERMESSAGE} and
         * {@link ConfigEntry.ConfigType#PLAYERMESSAGE} entries.
         *
         * <p>The label is on the left; the three buttons (main-location, other-location,
         * receiver) are packed to the right edge of the row.</p>
         */
        private class MessageConfigEntry extends Entry {
            private final ButtonWidget mainLocationButton;
            private final ButtonWidget otherLocationButton;
            private final ButtonWidget receiverButton;
            private final TextWidget textWidget;

            /**
             * Constructs a {@code MessageConfigEntry}.
             *
             * @param mainLocationButton  cycle-button for the "main" message location
             * @param otherLocationButton cycle-button for the "other" message location
             * @param receiverButton      cycle-button for the message receiver group
             * @param text                the label text shown on the left
             * @param hint                the tooltip shown when hovering over the label
             */
            MessageConfigEntry(ButtonWidget mainLocationButton, ButtonWidget otherLocationButton, ButtonWidget receiverButton, Text text, Text hint) {
                this.mainLocationButton = mainLocationButton;
                this.otherLocationButton = otherLocationButton;
                this.receiverButton = receiverButton;
                this.textWidget = new TextWidget(0, 0, 200, 20, text, client.textRenderer);
                this.textWidget.alignLeft();
                this.textWidget.setTooltip(Tooltip.of(hint));
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                textWidget.setX(x);
                textWidget.setY(y);
                textWidget.render(context, mouseX, mouseY, tickDelta);
                mainLocationButton.setX(x + entryWidth - mainLocationButton.getWidth() - otherLocationButton.getWidth() - receiverButton.getWidth() - 20);
                mainLocationButton.setY(y);
                mainLocationButton.render(context, mouseX, mouseY, tickDelta);
                otherLocationButton.setX(x + entryWidth - otherLocationButton.getWidth() - receiverButton.getWidth() - 10);
                otherLocationButton.setY(y);
                otherLocationButton.render(context, mouseX, mouseY, tickDelta);
                receiverButton.setX(x + entryWidth - receiverButton.getWidth());
                receiverButton.setY(y);
                receiverButton.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of(mainLocationButton, otherLocationButton, receiverButton, textWidget);
            }

            @Override
            public List<? extends Element> children() {
                return List.of(mainLocationButton, otherLocationButton, receiverButton, textWidget);
            }
        }

        /**
         * A config row backed by a {@link TextFieldWidget}; used for
         * {@link ConfigEntry.ConfigType#STRING} entries.
         *
         * <p>The label is on the left and the text field is on the right. Changes are
         * pushed to {@link ServerConfigRegistry#setValue(String, Object)} via a
         * changed-listener so the value is always up to date.</p>
         */
        private class StringConfigEntry extends Entry {
            private final TextFieldWidget textField;
            private final TextWidget textWidget;

            /**
             * Constructs a {@code StringConfigEntry}.
             *
             * @param textField the text field widget for string input
             * @param text      the label text shown on the left
             * @param hint      the tooltip shown when hovering over the label
             */
            StringConfigEntry(TextFieldWidget textField, Text text, Text hint) {
                this.textField = textField;
                this.textWidget = new TextWidget(0, 0, 200, 20, text, client.textRenderer);
                this.textWidget.alignLeft();
                this.textWidget.setTooltip(Tooltip.of(hint));
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                textWidget.setX(x);
                textWidget.setY(y);
                textWidget.render(context, mouseX, mouseY, tickDelta);
                textField.setX(x + entryWidth - textField.getWidth());
                textField.setY(y);
                textField.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of(textField, textWidget);
            }

            @Override
            public List<? extends Element> children() {
                return List.of(textField, textWidget);
            }
        }

        /**
         * A config row backed by a {@link SliderWidget}; used for
         * {@link ConfigEntry.ConfigType#DOUBLE} and {@link ConfigEntry.ConfigType#INTEGER}
         * entries when they are editable.
         *
         * <p>The label is on the left and the slider is on the right. Value changes are
         * applied immediately via {@link ServerConfigRegistry#setValue(String, Object)}.</p>
         */
        private class NumberConfigEntry extends Entry {
            private final TextWidget textWidget;
            private final SliderWidget sliderWidget;

            /**
             * Constructs a {@code NumberConfigEntry}.
             *
             * @param sliderWidget the slider widget for numeric input
             * @param text         the label text shown on the left
             * @param hint         the tooltip shown when hovering over the label
             */
            NumberConfigEntry(SliderWidget sliderWidget, Text text, Text hint) {
                this.sliderWidget = sliderWidget;
                this.textWidget = new TextWidget(0, 0, 200, 20, text, client.textRenderer);
                this.textWidget.alignLeft();
                this.textWidget.setTooltip(Tooltip.of(hint));
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                textWidget.setX(x);
                textWidget.setY(y);
                textWidget.render(context, mouseX, mouseY, tickDelta);
                sliderWidget.setX(x + entryWidth - sliderWidget.getWidth());
                sliderWidget.setY(y);
                sliderWidget.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of(sliderWidget, textWidget);
            }

            @Override
            public List<? extends Element> children() {
                return List.of(sliderWidget, textWidget);
            }
        }
    }

    /**
     * Returns the appropriate on/off {@link Text} for a boolean config value.
     * Delegates to {@link ScreenTexts#ON} and {@link ScreenTexts#OFF} so that the
     * labels are automatically localised.
     *
     * @param state the boolean value to convert
     * @return {@link ScreenTexts#ON} if {@code state} is {@code true},
     *         {@link ScreenTexts#OFF} otherwise
     */
    public static Text getBoolStateText(boolean state) {
        return state ? ScreenTexts.ON : ScreenTexts.OFF;
    }
}
