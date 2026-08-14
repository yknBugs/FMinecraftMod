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

import com.ykn.fmod.client.flow.gui.FlowEditorBridge;
import com.ykn.fmod.client.flow.gui.FlowEditorScreen;
import com.ykn.fmod.client.rule.gui.RuleEditorBridge;
import com.ykn.fmod.client.rule.gui.RuleEditorScreen;
import com.ykn.fmod.server.base.config.ConfigEntry;
import com.ykn.fmod.server.base.config.ConfigReader;
import com.ykn.fmod.server.base.config.ServerConfigRegistry;
import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.PlayerMessageType;
import com.ykn.fmod.server.base.util.ServerMessageType;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
 *       – a {@link net.minecraft.client.gui.components.AbstractSliderButton}</li>
 *   <li>{@link ConfigEntry.ConfigType#STRING}
 *       – a {@link net.minecraft.client.gui.components.EditBox}</li>
 *   <li>{@link ConfigEntry.ConfigType#BOOLEAN}
 *       – a toggle {@link net.minecraft.client.gui.components.Button}</li>
 *   <li>{@link ConfigEntry.ConfigType#SERVERMESSAGE} / {@link ConfigEntry.ConfigType#PLAYERMESSAGE}
 *       – three cycle-buttons for main location, other location, and receiver</li>
 * </ul>
 *
 * @see ServerConfigRegistry
 * @see ConfigEntry
 */
@OnlyIn(Dist.CLIENT)
public class OptionScreen extends Screen {

    private final Screen parent;
    private ConfigWidget configWidget;
    private Button doneButton;
    
    public OptionScreen(Screen parent) {
        super(Component.translatable("fmod.options.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        this.configWidget = new ConfigWidget(this.minecraft, this.width, this.height, 40, this.height - 40);
        this.addWidget(this.configWidget);

        this.doneButton = Button.builder(CommonComponents.GUI_DONE, button -> {
            Util.saveServerConfig();
            this.minecraft.setScreen(this.parent);
        }).pos(this.width / 2 - 100, this.height - 30).size(200, 20).build();
        this.addRenderableWidget(this.doneButton);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // this.configWidget is added via addWidget (not addRenderableWidget) so it isn't auto-rendered
        // by super.render(); it must render first since AbstractSelectionList paints a full-width fade
        // above/below its own bounds that would otherwise mask the renderable widgets drawn by super.render().
        this.configWidget.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xffffff);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        // Util.saveServerConfig();
        super.removed();
    }

    @Override
    public void onClose() {
        Util.saveServerConfig();
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.setScreen(this.parent);
    }

    /**
     * Scrollable list widget that builds and renders one row per registered
     * {@link ConfigEntry}-annotated config field.
     *
     * <p>Each row consists of a left-aligned label ({@link net.minecraft.client.gui.components.StringWidget})
     * with a tooltip containing the i18n hint text, and a right-aligned interactive
     * control widget whose type depends on {@link ConfigEntry#type()}.</p>
     */
    private class ConfigWidget extends ContainerObjectSelectionList<ConfigWidget.Entry> {

        public ConfigWidget(Minecraft client, int width, int height, int top, int bottom) {
            // 630 234 40 274
            super(client, width, height, top, bottom, 24);
            // Copyright info, with the flow/rule editor entry point on the right (singleplayer only)
            Button flowEditorButton = Button.builder(Component.translatable("fmod.flowgui.entrypoint"), b ->
                minecraft.setScreen(new FlowEditorScreen(OptionScreen.this))
            ).size(95, 20).build();
            if (!FlowEditorBridge.isAvailable()) {
                flowEditorButton.active = false;
                flowEditorButton.setTooltip(Tooltip.create(Component.translatable("fmod.flowgui.entrypoint.disabled")));
            }
            Button ruleEditorButton = Button.builder(Component.translatable("fmod.rulegui.entrypoint"), b ->
                minecraft.setScreen(new RuleEditorScreen(OptionScreen.this))
            ).size(95, 20).build();
            if (!RuleEditorBridge.isAvailable()) {
                ruleEditorButton.active = false;
                ruleEditorButton.setTooltip(Tooltip.create(Component.translatable("fmod.rulegui.entrypoint.disabled")));
            }
            this.addEntry(new TwoButtonConfigEntry(
                flowEditorButton,
                ruleEditorButton,
                Component.translatable("fmod.misc.version", Util.getMinecraftVersion(), Util.MOD_VERSION.toString(), Util.getModAuthors()),
                Component.translatable("fmod.options.tip")
            ));
            // Config entries
            this.buildConfigEntry();
        };

        @Override
        public int getRowWidth() {
            return this.width - 40;
        }

        @Override
        protected int getScrollbarPosition() {
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
                Component displayText = ServerConfigRegistry.getDisplayValue(configCodeEntry, rawValue);
                Button button = Button.builder(displayText, btn -> {}).size(200, 20).build();
                button.active = false;
                if (!configAnnotation.notEditableReason().isEmpty()) {
                    button.setTooltip(Tooltip.create(Component.translatable(configAnnotation.notEditableReason())));
                }
                this.addEntry(new ButtonConfigEntry(
                    button,
                    Component.translatable(i18nTitleKey),
                    Component.translatable(i18nHintKey).append("\n").append(Component.translatable("fmod.options.link", "/f options " + configAnnotation.commandEntry()))
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

            AbstractSliderButton slider = new AbstractSliderButton(0, 0, 200, 20,
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
                Component.translatable(i18nTitleKey),
                Component.translatable(i18nHintKey).append("\n").append(Component.translatable("fmod.options.link", "/f options " + configAnnotation.commandEntry()))
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
         *                         localised display {@link Component}
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
            Function<Enum<?>, Component> receiverToText,
            BiFunction<MessageType, Enum<?>, MessageType> updateReceiver
        ) {
            final List<MessageType.Location> locationValues = Arrays.asList(MessageType.Location.values());
            Button mainLocationButton = Button.builder(MessageType.getMessageLocationI18n(messageType.mainPlayerLocation), btn -> {
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

            Button otherLocationButton = Button.builder(MessageType.getMessageLocationI18n(messageType.otherPlayerLocation), btn -> {
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
            Button receiverButton = Button.builder(receiverToText.apply(messageType.getReceiver()), btn -> {
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
                Tooltip tooltip = Tooltip.create(Component.translatable(configAnnotation.notEditableReason()));
                mainLocationButton.setTooltip(tooltip);
                otherLocationButton.setTooltip(tooltip);
                receiverButton.setTooltip(tooltip);
            } else {
                mainLocationButton.setTooltip(Tooltip.create(Component.translatable("fmod.options.message.main")));
                otherLocationButton.setTooltip(Tooltip.create(Component.translatable("fmod.options.message.other")));
                receiverButton.setTooltip(Tooltip.create(Component.translatable("fmod.options.message.receiver")));
            }
            this.addEntry(new MessageConfigEntry(
                mainLocationButton,
                otherLocationButton,
                receiverButton,
                Component.translatable(i18nTitleKey),
                Component.translatable(i18nHintKey).append("\n").append(Component.translatable("fmod.options.link", "/f options " + configAnnotation.commandEntry()))
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

                            EditBox textField = new EditBox(minecraft.font, 0, 0, 200, 20, Component.empty());
                            textField.setMaxLength(maxLength);
                            textField.setEditable(isEditable);
                            textField.setValue(configValue);
                            if (!isEditable && !configAnnotation.notEditableReason().isEmpty()) {
                                textField.setTooltip(Tooltip.create(Component.translatable(configAnnotation.notEditableReason())));
                            }
                            textField.setResponder(s -> ServerConfigRegistry.setValue(configCodeEntry, s));
                            this.addEntry(new StringConfigEntry(
                                textField,
                                Component.translatable(i18nTitleKey),
                                Component.translatable(i18nHintKey).append("\n").append(Component.translatable("fmod.options.link", "/f options " + configAnnotation.commandEntry()))
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
                            Component displayText = ServerConfigRegistry.getDisplayValue(configCodeEntry, configValue);
                            Button button = Button.builder(displayText, btn -> {
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
                                button.setTooltip(Tooltip.create(Component.translatable(configAnnotation.notEditableReason())));
                            }
                            this.addEntry(new ButtonConfigEntry(
                                button,
                                Component.translatable(i18nTitleKey),
                                Component.translatable(i18nHintKey).append("\n").append(Component.translatable("fmod.options.link", "/f options " + configAnnotation.commandEntry()))
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
                    Component.literal("\u26A0 " + configCodeEntry).withStyle(style -> style.withColor(0xFF5555)),
                    Component.literal(e.getClass().getSimpleName() + ": " + e.getMessage())
                ));
            }
        }
    
        /**
         * Base class for all scrollable list entries in {@link ConfigWidget}.
         * Subclasses provide the concrete rendering and child-widget implementations.
         */
        abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(); 
            }
        }
    
        /**
         * A read-only text row used for the version/tip header at the top of the list.
         * The text is left-aligned and carries a tooltip with additional information.
         */
        private class TextHintEntry extends Entry {
            private final StringWidget textWidget;

            /**
             * Constructs a {@code TextHintEntry}.
             *
             * @param text the primary text to display in the row
             * @param tips the tooltip text shown on hover
             */
            public TextHintEntry(Component text, Component tips) {
                this.textWidget = new StringWidget(0, 0, 400, 20, text, minecraft.font);
                this.textWidget.alignLeft();
                this.textWidget.setTooltip(Tooltip.create(tips));
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                textWidget.setX(x);
                textWidget.setY(y);
                textWidget.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(textWidget);
            }
        }

        /**
         * A config row backed by a {@link Button}; used for
         * {@link ConfigEntry.ConfigType#BOOLEAN} entries and for non-editable numeric entries.
         *
         * <p>The label is rendered on the left and the button on the right.</p>
         */
        private class ButtonConfigEntry extends Entry {
            private final Button button;
            private final StringWidget textWidget;

            /**
             * Constructs a {@code ButtonConfigEntry}.
             *
             * @param button the button widget to display on the right side of the row
             * @param text   the label text shown on the left side
             * @param hint   the tooltip shown when hovering over the label
             */
            ButtonConfigEntry(Button button, Component text, Component hint) {
                this.button = button;
                this.textWidget = new StringWidget(0, 0, 200, 20, text, minecraft.font);
                this.textWidget.alignLeft();
                this.textWidget.setTooltip(Tooltip.create(hint));
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                textWidget.setX(x);
                textWidget.setY(y);
                textWidget.render(context, mouseX, mouseY, tickDelta);
                button.setX(x + entryWidth - button.getWidth());
                button.setY(y);
                button.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(button, textWidget);
            }
        }

        /**
         * A config row backed by two {@link Button}s packed to the right edge; used for the
         * flow-editor and rule-editor entry points, which share a single copyright/version label.
         */
        private class TwoButtonConfigEntry extends Entry {
            private final Button leftButton;
            private final Button rightButton;
            private final StringWidget textWidget;

            /**
             * Constructs a {@code TwoButtonConfigEntry}.
             *
             * @param leftButton  the button rendered to the left of {@code rightButton}
             * @param rightButton the button rendered flush against the row's right edge
             * @param text        the label text shown on the left side
             * @param hint        the tooltip shown when hovering over the label
             */
            TwoButtonConfigEntry(Button leftButton, Button rightButton, Component text, Component hint) {
                this.leftButton = leftButton;
                this.rightButton = rightButton;
                this.textWidget = new StringWidget(0, 0, 200, 20, text, minecraft.font);
                this.textWidget.alignLeft();
                this.textWidget.setTooltip(Tooltip.create(hint));
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                textWidget.setX(x);
                textWidget.setY(y);
                textWidget.render(context, mouseX, mouseY, tickDelta);
                rightButton.setX(x + entryWidth - rightButton.getWidth());
                rightButton.setY(y);
                rightButton.render(context, mouseX, mouseY, tickDelta);
                leftButton.setX(x + entryWidth - rightButton.getWidth() - leftButton.getWidth() - 10);
                leftButton.setY(y);
                leftButton.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(leftButton, rightButton, textWidget);
            }
        }

        /**
         * A config row backed by three {@link Button}s; used for
         * {@link ConfigEntry.ConfigType#SERVERMESSAGE} and
         * {@link ConfigEntry.ConfigType#PLAYERMESSAGE} entries.
         *
         * <p>The label is on the left; the three buttons (main-location, other-location,
         * receiver) are packed to the right edge of the row.</p>
         */
        private class MessageConfigEntry extends Entry {
            private final Button mainLocationButton;
            private final Button otherLocationButton;
            private final Button receiverButton;
            private final StringWidget textWidget;

            /**
             * Constructs a {@code MessageConfigEntry}.
             *
             * @param mainLocationButton  cycle-button for the "main" message location
             * @param otherLocationButton cycle-button for the "other" message location
             * @param receiverButton      cycle-button for the message receiver group
             * @param text                the label text shown on the left
             * @param hint                the tooltip shown when hovering over the label
             */
            MessageConfigEntry(Button mainLocationButton, Button otherLocationButton, Button receiverButton, Component text, Component hint) {
                this.mainLocationButton = mainLocationButton;
                this.otherLocationButton = otherLocationButton;
                this.receiverButton = receiverButton;
                this.textWidget = new StringWidget(0, 0, 200, 20, text, minecraft.font);
                this.textWidget.alignLeft();
                this.textWidget.setTooltip(Tooltip.create(hint));
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
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
            public List<? extends GuiEventListener> children() {
                return List.of(mainLocationButton, otherLocationButton, receiverButton, textWidget);
            }
        }

        /**
         * A config row backed by a {@link EditBox}; used for
         * {@link ConfigEntry.ConfigType#STRING} entries.
         *
         * <p>The label is on the left and the text field is on the right. Changes are
         * pushed to {@link ServerConfigRegistry#setValue(String, Object)} via a
         * changed-listener so the value is always up to date.</p>
         */
        private class StringConfigEntry extends Entry {
            private final EditBox textField;
            private final StringWidget textWidget;

            /**
             * Constructs a {@code StringConfigEntry}.
             *
             * @param textField the text field widget for string input
             * @param text      the label text shown on the left
             * @param hint      the tooltip shown when hovering over the label
             */
            StringConfigEntry(EditBox textField, Component text, Component hint) {
                this.textField = textField;
                this.textWidget = new StringWidget(0, 0, 200, 20, text, minecraft.font);
                this.textWidget.alignLeft();
                this.textWidget.setTooltip(Tooltip.create(hint));
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                textWidget.setX(x);
                textWidget.setY(y);
                textWidget.render(context, mouseX, mouseY, tickDelta);
                textField.setX(x + entryWidth - textField.getWidth());
                textField.setY(y);
                textField.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(textField, textWidget);
            }
        }

        /**
         * A config row backed by a {@link AbstractSliderButton}; used for
         * {@link ConfigEntry.ConfigType#DOUBLE} and {@link ConfigEntry.ConfigType#INTEGER}
         * entries when they are editable.
         *
         * <p>The label is on the left and the slider is on the right. Value changes are
         * applied immediately via {@link ServerConfigRegistry#setValue(String, Object)}.</p>
         */
        private class NumberConfigEntry extends Entry {
            private final StringWidget textWidget;
            private final AbstractSliderButton sliderWidget;

            /**
             * Constructs a {@code NumberConfigEntry}.
             *
             * @param sliderWidget the slider widget for numeric input
             * @param text         the label text shown on the left
             * @param hint         the tooltip shown when hovering over the label
             */
            NumberConfigEntry(AbstractSliderButton sliderWidget, Component text, Component hint) {
                this.sliderWidget = sliderWidget;
                this.textWidget = new StringWidget(0, 0, 200, 20, text, minecraft.font);
                this.textWidget.alignLeft();
                this.textWidget.setTooltip(Tooltip.create(hint));
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                textWidget.setX(x);
                textWidget.setY(y);
                textWidget.render(context, mouseX, mouseY, tickDelta);
                sliderWidget.setX(x + entryWidth - sliderWidget.getWidth());
                sliderWidget.setY(y);
                sliderWidget.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(sliderWidget);
            }
        }
    }

    /**
     * Returns the appropriate on/off {@link Component} for a boolean config value.
     * Delegates to {@link CommonComponents#OPTION_ON} and {@link CommonComponents#OPTION_OFF} so that the
     * labels are automatically localised.
     *
     * @param state the boolean value to convert
     * @return {@link CommonComponents#OPTION_ON} if {@code state} is {@code true},
     *         {@link CommonComponents#OPTION_OFF} otherwise
     */
    public static Component getBoolStateText(boolean state) {
        return state ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
    }
}
