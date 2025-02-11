package com.ykn.fmod.client.base.gui;

import java.util.Arrays;
import java.util.List;

import com.ykn.fmod.server.base.util.EnumI18n;
import com.ykn.fmod.server.base.util.MessageMethod;
import com.ykn.fmod.server.base.util.MessageType;
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

    private class ConfigWidget extends ElementListWidget<ConfigWidget.Entry> {
        public ConfigWidget(MinecraftClient client, int width, int height, int top, int bottom) {
            // 630 234 40 274
            super(client, width, height, top, bottom, 24);
            // Copyright Info
            this.addEntry(new TextHintEntry(Text.translatable("fmod.misc.version", Util.getMinecraftVersion(), Util.getModVersion(), Util.getModAuthors())));
            // Server Translation
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(getBoolStateText(Util.serverConfig.isEnableServerTranslation()), button -> {
                    Util.serverConfig.setEnableServerTranslation(!Util.serverConfig.isEnableServerTranslation());
                    button.setMessage(getBoolStateText(Util.serverConfig.isEnableServerTranslation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.translate"),
                Text.translatable("fmod.options.hint.translate")
            ));
            // Entity Death Message
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageTypeI18n(Util.serverConfig.getEntityDeathMessageType()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageType.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getEntityDeathMessageType());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setEntityDeathMessageType((MessageType) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageTypeI18n(Util.serverConfig.getEntityDeathMessageType()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.entdeathmsg"),
                Text.translatable("fmod.options.hint.entdeathmsg")
            ));
            // Boss Death Message
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageTypeI18n(Util.serverConfig.getBossDeathMessageType()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageType.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBossDeathMessageType());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBossDeathMessageType((MessageType) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageTypeI18n(Util.serverConfig.getBossDeathMessageType()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bcbossdeath"),
                Text.translatable("fmod.options.hint.bcbossdeath")
            ));
            // Named Entity Death Message
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageTypeI18n(Util.serverConfig.getNamedEntityDeathMessageType()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageType.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getNamedEntityDeathMessageType());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setNamedEntityDeathMessageType((MessageType) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageTypeI18n(Util.serverConfig.getNamedEntityDeathMessageType()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.nameddeath"),
                Text.translatable("fmod.options.hint.nameddeath")
            ));
            // Killer Entity Death Message
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageTypeI18n(Util.serverConfig.getKillerEntityDeathMessageType()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageType.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getKillerEntityDeathMessageType());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setKillerEntityDeathMessageType((MessageType) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageTypeI18n(Util.serverConfig.getKillerEntityDeathMessageType()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bckillerdeath"),
                Text.translatable("fmod.options.hint.bckillerdeath")
            ));
            // Boss Max Health Threshold
            SliderWidget bossMaxHealthSlider = new SliderWidget(0, 0, 400, 20, 
                Text.literal(String.format("%.1f", Util.serverConfig.getBossMaxHpThreshold())),
                Util.serverConfig.getBossMaxHpThreshold() / 1000.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.1f", this.value * 1000.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setBossMaxHpThreshold(this.value * 1000.0);
                }
            };
            this.addEntry(new NumberConfigEntry(
                bossMaxHealthSlider,
                Text.translatable("fmod.options.bossmaxhp"),
                Text.translatable("fmod.options.hint.bossmaxhp")
            ));
            // Player Death Coord
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageMethodI18n(Util.serverConfig.getPlayerDeathCoordMethod()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageMethod.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getPlayerDeathCoordMethod());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setPlayerDeathCoordMethod((MessageMethod) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageMethodI18n(Util.serverConfig.getPlayerDeathCoordMethod()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bcdeathcoord"),
                Text.translatable("fmod.options.hint.bcdeathcoord")
            ));
            // Projectile Hits Entity
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageMethodI18n(Util.serverConfig.getProjectileHitOthersMethod()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageMethod.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getProjectileHitOthersMethod());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setProjectileHitOthersMethod((MessageMethod) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageMethodI18n(Util.serverConfig.getProjectileHitOthersMethod()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.projhitting"),
                Text.translatable("fmod.options.hint.projhitting")
            ));
            // Projectile Being Hit
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageMethodI18n(Util.serverConfig.getProjectileBeingHitMethod()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageMethod.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getProjectileBeingHitMethod());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setProjectileBeingHitMethod((MessageMethod) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageMethodI18n(Util.serverConfig.getProjectileBeingHitMethod()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.projbeinghit"),
                Text.translatable("fmod.options.hint.projbeinghit")
            ));
            // Inform AFK
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageMethodI18n(Util.serverConfig.getInformAfkingMethod()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageMethod.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getInformAfkingMethod());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setInformAfkingMethod((MessageMethod) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageMethodI18n(Util.serverConfig.getInformAfkingMethod()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.informafk"),
                Text.translatable("fmod.options.hint.informafk")
            ));
            // Inform AFK Threshold
            SliderWidget afkThresholdSlider = new SliderWidget(0, 0, 400, 20, 
                Text.literal(String.format("%.2f", (double) Util.serverConfig.getInformAfkingThreshold() / 20.0)),
                Util.serverConfig.getInformAfkingThreshold() / 20.0 / 1800.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.2f", this.value * 1800.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setInformAfkingThreshold((int) (this.value * 1800.0 * 20.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                afkThresholdSlider,
                Text.translatable("fmod.options.informafkthres"),
                Text.translatable("fmod.options.hint.informafkthres")
            ));
            // Broadcast AFK
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageMethodI18n(Util.serverConfig.getBroadcastAfkingMethod()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageMethod.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBroadcastAfkingMethod());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBroadcastAfkingMethod((MessageMethod) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageMethodI18n(Util.serverConfig.getBroadcastAfkingMethod()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bcafk"),
                Text.translatable("fmod.options.hint.bcafk")
            ));
            // Broadcast AFK Threshold
            SliderWidget bcastAfkThresholdSlider = new SliderWidget(0, 0, 400, 20, 
                Text.literal(String.format("%.2f", (double) Util.serverConfig.getBroadcastAfkingThreshold() / 20.0)),
                Util.serverConfig.getBroadcastAfkingThreshold() / 20.0 / 1800.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.2f", this.value * 1800.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setBroadcastAfkingThreshold((int) (this.value * 1800.0 * 20.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                bcastAfkThresholdSlider,
                Text.translatable("fmod.options.bcafkthres"),
                Text.translatable("fmod.options.hint.bcafkthres")
            ));
            // Back From AFK
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageMethodI18n(Util.serverConfig.getStopAfkingMethod()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageMethod.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getStopAfkingMethod());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setStopAfkingMethod((MessageMethod) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageMethodI18n(Util.serverConfig.getStopAfkingMethod()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.stopafk"),
                Text.translatable("fmod.options.hint.stopafk")
            ));
            // Change Biome
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageMethodI18n(Util.serverConfig.getChangeBiomeMethod()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageMethod.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getChangeBiomeMethod());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setChangeBiomeMethod((MessageMethod) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageMethodI18n(Util.serverConfig.getChangeBiomeMethod()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.changebiome"),
                Text.translatable("fmod.options.hint.changebiome")
            ));
            // Change Biome Delay
            SliderWidget changeBiomeDelaySlider = new SliderWidget(0, 0, 400, 20, 
                Text.literal(String.format("%.2f", (double) Util.serverConfig.getChangeBiomeDelay() / 20.0)),
                Util.serverConfig.getChangeBiomeDelay() / 20.0 / 60.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.2f", this.value * 60.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setChangeBiomeDelay((int) (this.value * 60.0 * 20.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                changeBiomeDelaySlider,
                Text.translatable("fmod.options.biomedelay"),
                Text.translatable("fmod.options.hint.biomedelay")
            ));
            // GPT Server
            TextFieldWidget gptUrlTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 400, 20, Text.empty());
            gptUrlTxtWgt.setMaxLength(1024);
            gptUrlTxtWgt.setEditable(true);
            gptUrlTxtWgt.setText(Util.serverConfig.getGptUrl());
            gptUrlTxtWgt.setChangedListener(s -> Util.serverConfig.setGptUrl(s));
            this.addEntry(new StringConfigEntry(
                gptUrlTxtWgt,
                Text.translatable("fmod.options.gpturl"),
                Text.translatable("fmod.options.hint.gpturl")
            ));
            // GPT Tokens (For security reasons, this field is not editable)
            TextFieldWidget gptTknTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 400, 20, Text.empty());
            gptTknTxtWgt.setMaxLength(1024);
            gptTknTxtWgt.setEditable(false);
            gptTknTxtWgt.setText(Util.serverConfig.getSecureGptAccessTokens());
            gptTknTxtWgt.setTooltip(Tooltip.of(Text.translatable("fmod.options.gptkey.noedit")));
            this.addEntry(new StringConfigEntry(
                gptTknTxtWgt,
                Text.translatable("fmod.options.gptkey"),
                Text.translatable("fmod.options.hint.gptkey")
            ));
            // GPT Models
            TextFieldWidget gptMdlTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 400, 20, Text.empty());
            gptMdlTxtWgt.setMaxLength(1024);
            gptMdlTxtWgt.setEditable(true);
            gptMdlTxtWgt.setText(Util.serverConfig.getGptModel());
            gptMdlTxtWgt.setChangedListener(s -> Util.serverConfig.setGptModel(s));
            this.addEntry(new StringConfigEntry(
                gptMdlTxtWgt,
                Text.translatable("fmod.options.gptmodel"),
                Text.translatable("fmod.options.hint.gptmodel")
            ));
            // GPT System Prompt
            TextFieldWidget gptSysPrmptTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 400, 20, Text.empty());
            gptSysPrmptTxtWgt.setMaxLength(1024);
            gptSysPrmptTxtWgt.setEditable(true);
            gptSysPrmptTxtWgt.setText(Util.serverConfig.getGptSystemPrompt());
            gptSysPrmptTxtWgt.setChangedListener(s -> Util.serverConfig.setGptSystemPrompt(s));
            this.addEntry(new StringConfigEntry(
                gptSysPrmptTxtWgt,
                Text.translatable("fmod.options.gptsysprompt"),
                Text.translatable("fmod.options.hint.gptsysprompt")
            ));
            // GPT Temperature
            SliderWidget gptTempSlider = new SliderWidget(0, 0, 400, 20, 
                Text.literal(String.format("%.2f", Util.serverConfig.getGptTemperature())),
                Util.serverConfig.getGptTemperature()
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.2f", this.value)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setGptTemperature(this.value);
                }
            };
            this.addEntry(new NumberConfigEntry(
                gptTempSlider,
                Text.translatable("fmod.options.gpttemperature"),
                Text.translatable("fmod.options.hint.gpttemperature")
            ));
            // GPT Server Timeout
            SliderWidget gptTimeoutSlider = new SliderWidget(0, 0, 400, 20, 
                Text.literal(String.format("%.3f", Util.serverConfig.getGptServerTimeout() / 1000.0)),
                Util.serverConfig.getGptServerTimeout() / 1000.0 / 60.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.3f", this.value * 60.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setGptServerTimeout((int) (this.value * 60.0 * 1000.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                gptTimeoutSlider,
                Text.translatable("fmod.options.gpttimeout"),
                Text.translatable("fmod.options.hint.gpttimeout")
            ));
        };

        @Override
        public int getRowWidth() {
            return this.width - 40;
        }

        @Override
        protected int getScrollbarPositionX() {
            return this.width - 5;
        }
    
        abstract static class Entry extends ElementListWidget.Entry<Entry> {}
    
        private class TextHintEntry extends Entry {
            private final TextWidget textWidget;
    
            public TextHintEntry(Text text) {
                this.textWidget = new TextWidget(0, 0, 200, 20, text, client.textRenderer);
                this.textWidget.alignLeft();
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

        private class ButtonConfigEntry extends Entry {
            private final ButtonWidget button;
            private final TextWidget textWidget;

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

        private class StringConfigEntry extends Entry {
            private final TextFieldWidget textField;
            private final TextWidget textWidget;

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

        private class NumberConfigEntry extends Entry {
            private final TextWidget textWidget;
            private final SliderWidget sliderWidget;

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

    public static Text getBoolStateText(boolean state) {
        return state ? ScreenTexts.ON : ScreenTexts.OFF;
    }
}
