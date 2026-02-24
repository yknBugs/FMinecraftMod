/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.base.gui;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.ykn.fmod.server.base.config.ConfigEntry;
import com.ykn.fmod.server.base.config.ConfigReader;
import com.ykn.fmod.server.base.config.ServerConfigRegistry;
import com.ykn.fmod.server.base.util.MessageType;
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
        // Util.saveServerConfig();
        super.removed();
    }

    @Override
    public void close() {
        Util.saveServerConfig();
        if (this.client == null) {
            return;
        }
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
            Function<Double, Object> sliderToValue
        ) {
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
                    Util.LOGGER.error("FMinecraftMod: Failed to invoke toSliderValue method for " + configCodeEntry, e);
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
                            Util.LOGGER.error("FMinecraftMod: Failed to invoke fromSliderValue method for " + configCodeEntry, e);
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
         * Creates and adds a message-type config row for
         * {@link ConfigEntry.ConfigType#SERVERMESSAGE} and
         * {@link ConfigEntry.ConfigType#PLAYERMESSAGE} entries.
         *
         * <p>The row contains three cycle-buttons placed to the right of the label:</p>
         * <ol>
         *   <li><b>Main location</b> – cycles through {@link MessageType.Location} values
         *       and updates {@link MessageType#mainPlayerLocation} via
         *       {@link MessageType#updateMain(MessageType.Location)}.</li>
         *   <li><b>Other location</b> – cycles through {@link MessageType.Location} values
         *       and updates {@link MessageType#otherPlayerLocation} via
         *       {@link MessageType#updateOther(MessageType.Location)}.</li>
         *   <li><b>Receiver</b> – cycles through the caller-supplied {@code receiverValues}
         *       list and stores the result via the caller-supplied {@code updateReceiver}
         *       function, allowing the concrete receiver enum
         *       ({@link ServerMessageType.Receiver} or {@link PlayerMessageType.Receiver})
         *       to stay decoupled from this generic helper.</li>
         * </ol>
         *
         * <p>When {@code isEditable} is {@code false} all three buttons are disabled.
         * If {@link ConfigEntry#notEditableReason()} is non-empty its translation is shown
         * as a tooltip on every button; otherwise each button shows its own descriptive tooltip
         * ({@code fmod.options.message.main}, {@code fmod.options.message.other},
         * {@code fmod.options.message.receiver}).</p>
         *
         * @param configCodeEntry  the code-entry name used to read/write the value via
         *                         {@link ServerConfigRegistry}
         * @param configAnnotation the {@link ConfigEntry} annotation of the field
         * @param i18nTitleKey     translation key for the row label text
         * @param i18nHintKey      translation key for the label tooltip hint text
         * @param isEditable       whether the three buttons should be interactive
         * @param messageType      the current {@link MessageType} value of the config field;
         *                         used to initialise the button labels
         * @param receiverValues   the ordered list of receiver enum constants to cycle through
         *                         (e.g. {@code Arrays.asList(ServerMessageType.Receiver.values())})
         * @param receiverToText   a function that converts a receiver enum constant to its
         *                         localised display {@link Text}
         * @param updateReceiver   a {@link BiFunction} that takes the current {@link MessageType}
         *                         and the newly selected receiver enum constant and returns the
         *                         updated {@link MessageType} to be stored in the registry
         */
        private void buildMessageTypeEntry(
            String configCodeEntry,
            ConfigEntry configAnnotation,
            String i18nTitleKey,
            String i18nHintKey,
            boolean isEditable,
            MessageType messageType,
            List<Enum<?>> receiverValues,
            Function<Enum<?>, Text> receiverToText,
            BiFunction<MessageType, Enum<?>, MessageType> updateReceiver
        ) {
            final List<MessageType.Location> locationValues = Arrays.asList(MessageType.Location.values());
            ButtonWidget mainLocationButton = ButtonWidget.builder(MessageType.getMessageLocationI18n(messageType.mainPlayerLocation), btn -> {
                MessageType currentValue = (MessageType) ServerConfigRegistry.getValue(configCodeEntry);
                if (currentValue == null) {
                    Util.LOGGER.error("FMinecraftMod: Got unexpected null MessageType value for " + configCodeEntry);
                    return;
                }
                int currentIndex = locationValues.indexOf(currentValue.mainPlayerLocation);
                int nextIndex = (currentIndex + 1) % locationValues.size();
                MessageType.Location newLocation =  locationValues.get(nextIndex);
                ServerConfigRegistry.setValue(configCodeEntry, currentValue.updateMain(newLocation));
                btn.setMessage(MessageType.getMessageLocationI18n(newLocation));
            }).size(60, 20).build();

            ButtonWidget otherLocationButton = ButtonWidget.builder(MessageType.getMessageLocationI18n(messageType.otherPlayerLocation), btn -> {
                MessageType currentValue = (MessageType) ServerConfigRegistry.getValue(configCodeEntry);
                if (currentValue == null) {
                    Util.LOGGER.error("FMinecraftMod: Got unexpected null MessageType value for " + configCodeEntry);
                    return;
                }
                int currentIndex = locationValues.indexOf(currentValue.otherPlayerLocation);
                int nextIndex = (currentIndex + 1) % locationValues.size();
                MessageType.Location newLocation =  locationValues.get(nextIndex);
                ServerConfigRegistry.setValue(configCodeEntry, currentValue.updateOther(newLocation));
                btn.setMessage(MessageType.getMessageLocationI18n(newLocation));
            }).size(60, 20).build();
            ButtonWidget receiverButton = ButtonWidget.builder(receiverToText.apply(messageType.getReceiver()), btn -> {
                MessageType currentValue = (MessageType) ServerConfigRegistry.getValue(configCodeEntry);
                if (currentValue == null) {
                    Util.LOGGER.error("FMinecraftMod: Got unexpected null MessageType value for " + configCodeEntry);
                    return;
                }
                int currentIndex = receiverValues.indexOf(currentValue.getReceiver());
                int nextIndex = (currentIndex + 1) % receiverValues.size();
                Enum<?> newReceiver = receiverValues.get(nextIndex);
                ServerConfigRegistry.setValue(configCodeEntry, updateReceiver.apply(currentValue, newReceiver));
                btn.setMessage(receiverToText.apply(newReceiver));
            }).size(60, 20).build();
            mainLocationButton.active = isEditable;
            otherLocationButton.active = isEditable;
            receiverButton.active = isEditable;
            if (!isEditable && !configAnnotation.notEditableReason().isEmpty()) {
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
                            double configValue = 0.0;
                            if (rawValue == null) {
                                Util.LOGGER.error("FMinecraftMod: Got unexpected null Double value for " + configCodeEntry + ", falling back to 0.0");
                            } else if (rawValue instanceof Double) {
                                configValue = (double) rawValue;
                            } else {
                                Util.LOGGER.error("FMinecraftMod: Config entry " + configCodeEntry + " is annotated as DOUBLE but the value is not of type Double, falling back to 0.0");
                            }
                            buildNumericSliderEntry(
                                configCodeEntry, configAnnotation, configInstance,
                                i18nTitleKey, i18nHintKey, isEditable, Double.valueOf(configValue),
                                min, max, configValue,
                                v -> v * (max - min) + min
                            );
                        }
                        return;
                    case INTEGER:
                        {
                            final int min = configAnnotation.minSliderInt();
                            final int max = configAnnotation.maxSliderInt();
                            int configValue = 0;
                            if (rawValue == null) {
                                Util.LOGGER.error("FMinecraftMod: Got unexpected null Integer value for " + configCodeEntry + ", falling back to 0");
                            } else if (rawValue instanceof Integer) {
                                configValue = (int) rawValue;
                            } else {
                                Util.LOGGER.error("FMinecraftMod: Config entry " + configCodeEntry + " is annotated as INTEGER but the value is not of type Integer, falling back to 0");
                            }
                            buildNumericSliderEntry(
                                configCodeEntry, configAnnotation, configInstance,
                                i18nTitleKey, i18nHintKey, isEditable, Integer.valueOf(configValue),
                                min, max, configValue,
                                v -> (int) Math.round(v * (max - min) + min)
                            );
                        }
                        return;
                    case STRING:
                        {
                            String configValue = "";
                            if (rawValue == null) {
                                Util.LOGGER.error("FMinecraftMod: Got unexpected null String value for " + configCodeEntry + ", falling back to empty string");
                            } else if (rawValue instanceof String) {
                                configValue = ServerConfigRegistry.getDisplayValue(configCodeEntry, rawValue).getString();
                            } else {
                                Util.LOGGER.warn("FMinecraftMod: Config entry " + configCodeEntry + " is annotated as STRING but the value is not of type String, trying to auto-convert it to String");
                                configValue = ServerConfigRegistry.getDisplayValue(configCodeEntry, String.valueOf(rawValue)).getString();
                            }
                            
                            final int maxLength = configAnnotation.maxStringLength();

                            TextFieldWidget textField = new TextFieldWidget(client.textRenderer, 0, 0, 200, 20, Text.empty());
                            textField.setMaxLength(maxLength);
                            textField.setEditable(isEditable);
                            textField.setText(configValue);
                            if (!isEditable && !configAnnotation.notEditableReason().isEmpty()) {
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
                            boolean configValue = false;
                            if (rawValue == null) {
                                Util.LOGGER.error("FMinecraftMod: Got unexpected null Boolean value for " + configCodeEntry + ", falling back to false");
                            } else if (rawValue instanceof Boolean) {
                                configValue = (boolean) rawValue;
                            } else {
                                Util.LOGGER.error("FMinecraftMod: Config entry " + configCodeEntry + " is annotated as BOOLEAN but the value is not of type Boolean, falling back to false");
                            }
                            Text displayText = ServerConfigRegistry.getDisplayValue(configCodeEntry, configValue);
                            ButtonWidget button = ButtonWidget.builder(displayText, btn -> {
                                Object currentValue = ServerConfigRegistry.getValue(configCodeEntry);
                                if (currentValue == null) {
                                    Util.LOGGER.error("FMinecraftMod: Got unexpected null Boolean value for " + configCodeEntry);
                                    return;
                                }
                                boolean newValue = false;
                                if (currentValue instanceof Boolean) {
                                    newValue = (boolean) currentValue;
                                    newValue = !newValue;
                                }
                                ServerConfigRegistry.setValue(configCodeEntry, newValue);
                                btn.setMessage(ServerConfigRegistry.getDisplayValue(configCodeEntry, newValue));
                            }).size(200, 20).build();
                            button.active = isEditable;
                            if (!isEditable && !configAnnotation.notEditableReason().isEmpty()) {
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
                            if (!(rawValue instanceof ServerMessageType)) {
                                Util.LOGGER.error("FMinecraftMod: Config entry " + configCodeEntry + " is annotated as SERVERMESSAGE but the value is not of type ServerMessageType");
                                return;
                            }
                            buildMessageTypeEntry(
                                configCodeEntry, configAnnotation, i18nTitleKey, i18nHintKey, isEditable,
                                (ServerMessageType) rawValue, Arrays.asList(ServerMessageType.Receiver.values()),
                                receiver -> ServerMessageType.getMessageReceiverI18n((ServerMessageType.Receiver) receiver),
                                (messageType, newReceiver) -> ((ServerMessageType) messageType).updateReceiver((ServerMessageType.Receiver) newReceiver)
                            );
                        }
                        return;
                    case PLAYERMESSAGE:
                        {
                            if (!(rawValue instanceof PlayerMessageType)) {
                                Util.LOGGER.error("FMinecraftMod: Config entry " + configCodeEntry + " is annotated as PLAYERMESSAGE but the value is not of type PlayerMessageType");
                                return;
                            }
                            buildMessageTypeEntry(
                                configCodeEntry, configAnnotation, i18nTitleKey, i18nHintKey, isEditable,
                                (PlayerMessageType) rawValue, Arrays.asList(PlayerMessageType.Receiver.values()),
                                receiver -> PlayerMessageType.getMessageReceiverI18n((PlayerMessageType.Receiver) receiver),
                                (messageType, newReceiver) -> ((PlayerMessageType) messageType).updateReceiver((PlayerMessageType.Receiver) newReceiver)
                            );
                        }
                        return;
                    default:
                        return;
                }
            } catch (Exception e) {
                Util.LOGGER.error("FMinecraftMod: Failed to build config entry for " + configCodeEntry, e);
                this.addEntry(new TextHintEntry(
                    Text.literal("\u26A0 " + configCodeEntry).styled(s -> s.withColor(0xFF5555)),
                    Text.literal(e.getClass().getSimpleName() + ": " + e.getMessage())
                ));
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
