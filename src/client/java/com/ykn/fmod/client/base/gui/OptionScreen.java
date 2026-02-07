/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.base.gui;

import java.util.Arrays;
import java.util.List;

import com.ykn.fmod.server.base.util.EnumI18n;
import com.ykn.fmod.server.base.util.MessageReceiver;
import com.ykn.fmod.server.base.util.MessageLocation;
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
            this.addEntry(new TextHintEntry(
                Text.translatable("fmod.misc.version", Util.getMinecraftVersion(), Util.getModVersion(), Util.getModAuthors()),
                Text.translatable("fmod.options.tip")
            ));
            // Server Translation
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(getBoolStateText(Util.serverConfig.isEnableServerTranslation()), button -> {
                    Util.serverConfig.setEnableServerTranslation(!Util.serverConfig.isEnableServerTranslation());
                    button.setMessage(getBoolStateText(Util.serverConfig.isEnableServerTranslation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.translate"),
                Text.translatable("fmod.options.hint.translate")
            ));
            // Flow Length (non-linear slider)
            SliderWidget flowLengthSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(Integer.toString(Util.serverConfig.getMaxFlowLength())),
                Math.log((double) Util.serverConfig.getMaxFlowLength()) / Math.log(2147483647.0)
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(Integer.toString((int) Math.exp(this.value * Math.log(2147483647.0)))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setMaxFlowLength((int) Math.exp(this.value * Math.log(2147483647.0)));
                }
            };
            this.addEntry(new NumberConfigEntry(
                flowLengthSlider,
                Text.translatable("fmod.options.flowlength"),
                Text.translatable("fmod.options.hint.flowlength")
            ));
            // Flow Recursion Depth (linear slider with 0 ~ 256)
            SliderWidget flowRecursionSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(Integer.toString(Util.serverConfig.getMaxFlowRecursionDepth())),
                Util.serverConfig.getMaxFlowRecursionDepth() / 256.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(Integer.toString((int) (this.value * 256.0))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setMaxFlowRecursionDepth((int) (this.value * 256.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                flowRecursionSlider,
                Text.translatable("fmod.options.flowrecursion"),
                Text.translatable("fmod.options.hint.flowrecursion")
            ));
            // Flow History Size (non-linear slider)
            SliderWidget flowHistorySlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(Integer.toString(Util.serverConfig.getKeepFlowHistoryNumber())),
                Math.log((double) Util.serverConfig.getKeepFlowHistoryNumber()) / Math.log(2147483647.0)
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(Integer.toString((int) Math.exp(this.value * Math.log(2147483647.0)))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setKeepFlowHistoryNumber((int) Math.exp(this.value * Math.log(2147483647.0)));
                }
            };
            this.addEntry(new NumberConfigEntry(
                flowHistorySlider,
                Text.translatable("fmod.options.flowhistory"),
                Text.translatable("fmod.options.hint.flowhistory")
            ));
            // Normal Entity Death Message
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityDeathMessage()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getEntityDeathMessage());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setEntityDeathMessage((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityDeathMessage()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.entdeathmsg"),
                Text.translatable("fmod.options.hint.entdeathmsg")
            ));
            // Passive Entity Death Message
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getPassiveDeathMessage()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getPassiveDeathMessage());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setPassiveDeathMessage((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getPassiveDeathMessage()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.passivedeathmsg"),
                Text.translatable("fmod.options.hint.passivedeathmsg")
            ));
            // Hostile Entity Death Message
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getHostileDeathMessage()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getHostileDeathMessage());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setHostileDeathMessage((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getHostileDeathMessage()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.hostiledeathmsg"),
                Text.translatable("fmod.options.hint.hostiledeathmsg")
            ));
            // Boss Death Message
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossDeathMessage()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBossDeathMessage());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBossDeathMessage((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossDeathMessage()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bcbossdeath"),
                Text.translatable("fmod.options.hint.bcbossdeath")
            ));
            // Named Entity Death Message
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getNamedEntityDeathMessage()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getNamedEntityDeathMessage());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setNamedEntityDeathMessage((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getNamedEntityDeathMessage()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.nameddeath"),
                Text.translatable("fmod.options.hint.nameddeath")
            ));
            // Killer Entity Death Message
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getKillerEntityDeathMessage()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getKillerEntityDeathMessage());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setKillerEntityDeathMessage((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getKillerEntityDeathMessage()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bckillerdeath"),
                Text.translatable("fmod.options.hint.bckillerdeath")
            ));
            // Boss Max Health Threshold
            SliderWidget bossMaxHealthSlider = new SliderWidget(0, 0, 200, 20, 
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
            // Can Sleep Message
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getPlayerCanSleepMessage()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getPlayerCanSleepMessage());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setPlayerCanSleepMessage((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getPlayerCanSleepMessage()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.cansleepmsg"),
                Text.translatable("fmod.options.hint.cansleepmsg")
            ));
            // Player Death Coord Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getPlayerDeathCoordLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getPlayerDeathCoordLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setPlayerDeathCoordLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getPlayerDeathCoordLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bcdeathcoordloc"),
                Text.translatable("fmod.options.hint.bcdeathcoordloc")
            ));
            // Player Death Coord Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerDeathCoordReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getPlayerDeathCoordReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setPlayerDeathCoordReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerDeathCoordReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bcdeathcoordreceiver"),
                Text.translatable("fmod.options.hint.bcdeathcoordreceiver")
            ));
            // Projectile Hits Entity Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getProjectileHitOthersLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getProjectileHitOthersLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setProjectileHitOthersLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getProjectileHitOthersLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.projhittingloc"),
                Text.translatable("fmod.options.hint.projhittingloc")
            ));
            // Projectile Hits Entity Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileHitOthersReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getProjectileHitOthersReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setProjectileHitOthersReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileHitOthersReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.projhittingreceiver"),
                Text.translatable("fmod.options.hint.projhittingreceiver")
            ));
            // Projectile Being Hit Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getProjectileBeingHitLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getProjectileBeingHitLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setProjectileBeingHitLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getProjectileBeingHitLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.projbeinghitloc"),
                Text.translatable("fmod.options.hint.projbeinghitloc")
            ));
            // Projectile Being Hit Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileBeingHitReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getProjectileBeingHitReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setProjectileBeingHitReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileBeingHitReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.projbeinghitreceiver"),
                Text.translatable("fmod.options.hint.projbeinghitreceiver")
            ));
            // Inform AFK Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getInformAfkingLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getInformAfkingLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setInformAfkingLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getInformAfkingLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.informafkloc"),
                Text.translatable("fmod.options.hint.informafkloc")
            ));
            // Inform AFK Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getInformAfkingReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getInformAfkingReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setInformAfkingReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getInformAfkingReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.informafkreceiver"),
                Text.translatable("fmod.options.hint.informafkreceiver")
            ));
            // Inform AFK Threshold
            SliderWidget afkThresholdSlider = new SliderWidget(0, 0, 200, 20, 
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
            // Broadcast AFK Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getBroadcastAfkingLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBroadcastAfkingLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBroadcastAfkingLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getBroadcastAfkingLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bcafkloc"),
                Text.translatable("fmod.options.hint.bcafkloc")
            ));
            // Broadcast AFK Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBroadcastAfkingReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBroadcastAfkingReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBroadcastAfkingReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBroadcastAfkingReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bcafkreceiver"),
                Text.translatable("fmod.options.hint.bcafkreceiver")
            ));
            // Broadcast AFK Threshold
            SliderWidget bcastAfkThresholdSlider = new SliderWidget(0, 0, 200, 20, 
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
            // Back From AFK Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getStopAfkingLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getStopAfkingLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setStopAfkingLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getStopAfkingLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.stopafkloc"),
                Text.translatable("fmod.options.hint.stopafkloc")
            ));
            // Back From AFK Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getStopAfkingReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getStopAfkingReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setStopAfkingReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getStopAfkingReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.stopafkreceiver"),
                Text.translatable("fmod.options.hint.stopafkreceiver")
            ));
            // Change Biome Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getChangeBiomeLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getChangeBiomeLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setChangeBiomeLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getChangeBiomeLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.changebiomeloc"),
                Text.translatable("fmod.options.hint.changebiomeloc")
            ));
            // Change Biome Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getChangeBiomeReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getChangeBiomeReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setChangeBiomeReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getChangeBiomeReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.changebiomereceiver"),
                Text.translatable("fmod.options.hint.changebiomereceiver")
            ));
            // Change Biome Delay
            SliderWidget changeBiomeDelaySlider = new SliderWidget(0, 0, 200, 20, 
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
            // Boss Fight Message Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossFightMessageLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBossFightMessageLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBossFightMessageLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossFightMessageLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bossfightloc"),
                Text.translatable("fmod.options.hint.bossfightloc")
            ));
            // Boss Fight Message Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBossFightMessageReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBossFightMessageReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBossFightMessageReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBossFightMessageReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bossfightreceiver"),
                Text.translatable("fmod.options.hint.bossfightreceiver")
            ));
            // Boss Fight Message Interval
            SliderWidget bossFightMsgIntervalSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(String.format("%.2f", (double) Util.serverConfig.getBossFightInterval() / 20.0)),
                Util.serverConfig.getBossFightInterval() / 20.0 / 180.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.2f", this.value * 180.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setBossFightInterval((int) (this.value * 180.0 * 20.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                bossFightMsgIntervalSlider,
                Text.translatable("fmod.options.bossfightinterval"),
                Text.translatable("fmod.options.hint.bossfightinterval")
            ));
            // Monster Surrounded Message Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getMonsterSurroundMessageLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getMonsterSurroundMessageLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setMonsterSurroundMessageLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getMonsterSurroundMessageLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.monsterloc"),
                Text.translatable("fmod.options.hint.monsterloc")
            ));
            // Monster Surrounded Message Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getMonsterSurroundMessageReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getMonsterSurroundMessageReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setMonsterSurroundMessageReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getMonsterSurroundMessageReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.monsterreceiver"),
                Text.translatable("fmod.options.hint.monsterreceiver")
            ));
            // Monster Surrounded Message Interval
            SliderWidget monsterSurroundMsgIntervalSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(String.format("%.2f", (double) Util.serverConfig.getMonsterSurroundInterval() / 20.0)),
                Util.serverConfig.getMonsterSurroundInterval() / 20.0 / 180.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.2f", this.value * 180.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setMonsterSurroundInterval((int) (this.value * 180.0 * 20.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                monsterSurroundMsgIntervalSlider,
                Text.translatable("fmod.options.monsterinterval"),
                Text.translatable("fmod.options.hint.monsterinterval")
            ));
            // Monster Number
            SliderWidget monsterNumSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(Integer.toString(Util.serverConfig.getMonsterNumberThreshold())),
                (double) Util.serverConfig.getMonsterNumberThreshold() / 100.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(Integer.toString((int) (this.value * 100.0))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setMonsterNumberThreshold((int) (this.value * 100.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                monsterNumSlider,
                Text.translatable("fmod.options.monsternumber"),
                Text.translatable("fmod.options.hint.monsternumber")
            ));
            // Monster Distance
            SliderWidget monsterDistanceSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(String.format("%.2f", Util.serverConfig.getMonsterDistanceThreshold())),
                Util.serverConfig.getMonsterDistanceThreshold() / 128.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.2f", this.value * 128.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setMonsterDistanceThreshold(this.value * 128.0);
                }
            };
            this.addEntry(new NumberConfigEntry(
                monsterDistanceSlider,
                Text.translatable("fmod.options.monsterdistance"),
                Text.translatable("fmod.options.hint.monsterdistance")
            ));
            // Entity Warning
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityNumberWarning()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getEntityNumberWarning());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setEntityNumberWarning((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityNumberWarning()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.entitywarning"),
                Text.translatable("fmod.options.hint.entitywarning")
            ));
            // Density Warning
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityDensityWarning()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getEntityDensityWarning());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setEntityDensityWarning((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityDensityWarning()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.densitywarning"),
                Text.translatable("fmod.options.hint.densitywarning")
            ));
            // Entity Number
            SliderWidget entityNumSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(Integer.toString(Util.serverConfig.getEntityNumberThreshold())),
                (double) Util.serverConfig.getEntityNumberThreshold() / 10000.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(Integer.toString((int) (this.value * 10000.0))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setEntityNumberThreshold((int) (this.value * 10000.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                entityNumSlider,
                Text.translatable("fmod.options.entitynumber"),
                Text.translatable("fmod.options.hint.entitynumber")
            ));
            // Entity Density
            SliderWidget entityDensitySlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(Integer.toString(Util.serverConfig.getEntityDensityThreshold())),
                (double) Util.serverConfig.getEntityDensityThreshold() / 10000.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(Integer.toString((int) (this.value * 10000.0))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setEntityDensityThreshold((int) (this.value * 10000.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                entityDensitySlider,
                Text.translatable("fmod.options.entitydensity"),
                Text.translatable("fmod.options.hint.entitydensity")
            ));
            // Density Number
            SliderWidget densityNumberSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(Integer.toString(Util.serverConfig.getEntityDensityNumber())),
                (double) Util.serverConfig.getEntityDensityNumber() / 1000.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(Integer.toString((int) (this.value * 1000.0))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setEntityDensityNumber((int) (this.value * 1000.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                densityNumberSlider,
                Text.translatable("fmod.options.densitynumber"),
                Text.translatable("fmod.options.hint.densitynumber")
            ));
            // Density Radius
            SliderWidget densityRadiusSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(String.format("%.2f", Util.serverConfig.getEntityDensityRadius())),
                Util.serverConfig.getEntityDensityRadius() / 256.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.2f", this.value * 256.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setEntityDensityRadius(this.value * 256.0);
                }
            };
            this.addEntry(new NumberConfigEntry(
                densityRadiusSlider,
                Text.translatable("fmod.options.densityradius"),
                Text.translatable("fmod.options.hint.densityradius")
            ));
            // Entity Interval (Range: 1 ~ 1200 Ticks, show in Ticks)
            SliderWidget entityIntervalSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(Integer.toString(Util.serverConfig.getEntityNumberInterval())),
                ((double) Util.serverConfig.getEntityNumberInterval() - 1.0) / 1199.0 
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(Integer.toString((int) (this.value * 1199.0 + 1.0))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setEntityNumberInterval((int) (this.value * 1199.0 + 1.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                entityIntervalSlider,
                Text.translatable("fmod.options.entityinterval"),
                Text.translatable("fmod.options.hint.entityinterval")
            ));
            // Density Interval (Range: 1 ~ 1200 Ticks, show in Ticks)
            SliderWidget densityIntervalSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(Integer.toString(Util.serverConfig.getEntityDensityInterval())),
                ((double) Util.serverConfig.getEntityDensityInterval() - 1.0) / 1199.0 
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(Integer.toString((int) (this.value * 1199.0 + 1.0))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setEntityDensityInterval((int) (this.value * 1199.0 + 1.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                densityIntervalSlider,
                Text.translatable("fmod.options.densityinterval"),
                Text.translatable("fmod.options.hint.densityinterval")
            ));
            // Player Seriously Hurt Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getPlayerSeriousHurtLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getPlayerSeriousHurtLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setPlayerSeriousHurtLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getPlayerSeriousHurtLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.playerhurtloc"),
                Text.translatable("fmod.options.hint.playerhurtloc")
            ));
            // Player Seriously Hurt Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerSeriousHurtReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getPlayerSeriousHurtReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setPlayerSeriousHurtReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerSeriousHurtReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.playerhurtreceiver"),
                Text.translatable("fmod.options.hint.playerhurtreceiver")
            ));
            // Damage Threshold
            SliderWidget damageThresholdSlider = new SliderWidget(0, 0, 200, 20, 
                Text.literal(String.format("%.1f", Util.serverConfig.getPlayerHurtThreshold() * 100.0) + "%"),
                Util.serverConfig.getPlayerHurtThreshold()
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.1f", this.value * 100.0) + "%"));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setPlayerHurtThreshold(this.value);
                }
            };
            this.addEntry(new NumberConfigEntry(
                damageThresholdSlider,
                Text.translatable("fmod.options.damagethres"),
                Text.translatable("fmod.options.hint.damagethres")
            ));
            // Travel Message Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getTravelMessageLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getTravelMessageLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setTravelMessageLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getTravelMessageLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.travelmsg.loc"),
                Text.translatable("fmod.options.hint.travelmsg.loc")
            ));
            // Travel Message Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getTravelMessageReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getTravelMessageReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setTravelMessageReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getTravelMessageReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.travelmsg.receiver"),
                Text.translatable("fmod.options.hint.travelmsg.receiver")
            ));
            // Travel Message Interval (ticks, shown in seconds)
            SliderWidget travelMsgIntervalSlider = new SliderWidget(0, 0, 200, 20,
                Text.literal(String.format("%.2f", Util.serverConfig.getTravelMessageInterval() / 20.0)),
                ((double) Util.serverConfig.getTravelMessageInterval() - 1.0) / 1199.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.2f", (this.value * 1199.0 + 1.0) / 20.0)));
                }

                @Override
                protected void applyValue() {
                    Util.serverConfig.setTravelMessageInterval((int) (this.value * 1199.0 + 1.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                travelMsgIntervalSlider,
                Text.translatable("fmod.options.travelmsg.msginterval"),
                Text.translatable("fmod.options.hint.travelmsg.msginterval")
            ));
            // Travel Window (ticks, shown in seconds)
            SliderWidget travelWindowSlider = new SliderWidget(0, 0, 200, 20,
                Text.literal(String.format("%.2f", Util.serverConfig.getTravelWindowTicks() / 20.0)),
                ((double) Util.serverConfig.getTravelWindowTicks() - 1.0) / 11999.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.2f", (this.value * 11999.0 + 1.0) / 20.0)));
                }

                @Override
                protected void applyValue() {
                    Util.serverConfig.setTravelWindowTicks((int) (this.value * 11999.0 + 1.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                travelWindowSlider,
                Text.translatable("fmod.options.travelmsg.window"),
                Text.translatable("fmod.options.hint.travelmsg.window")
            ));
            // Travel Total Distance
            SliderWidget travelTotalDistSlider = new SliderWidget(0, 0, 200, 20,
                Text.literal(String.format("%.1f", Util.serverConfig.getTravelTotalDistanceThreshold())),
                Util.serverConfig.getTravelTotalDistanceThreshold() / 1000.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.1f", this.value * 1000.0)));
                }

                @Override
                protected void applyValue() {
                    Util.serverConfig.setTravelTotalDistanceThreshold(this.value * 1000.0);
                }
            };
            this.addEntry(new NumberConfigEntry(
                travelTotalDistSlider,
                Text.translatable("fmod.options.travelmsg.total"),
                Text.translatable("fmod.options.hint.travelmsg.total")
            ));
            // Travel Partial Interval (ticks)
            SliderWidget travelPartialIntervalSlider = new SliderWidget(0, 0, 200, 20,
                Text.literal(String.format("%.2f", Util.serverConfig.getTravelPartialInterval() / 20.0)),
                ((double) Util.serverConfig.getTravelPartialInterval() - 1.0) / 11999.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.2f", (this.value * 11999.0 + 1.0) / 20.0)));
                }

                @Override
                protected void applyValue() {
                    Util.serverConfig.setTravelPartialInterval((int) (this.value * 11999.0 + 1.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                travelPartialIntervalSlider,
                Text.translatable("fmod.options.travelmsg.interval"),
                Text.translatable("fmod.options.hint.travelmsg.interval")
            ));
            // Travel Partial Distance
            SliderWidget travelPartialDistSlider = new SliderWidget(0, 0, 200, 20,
                Text.literal(String.format("%.1f", Util.serverConfig.getTravelPartialDistanceThreshold())),
                Util.serverConfig.getTravelPartialDistanceThreshold() / 1000.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.1f", this.value * 1000.0)));
                }

                @Override
                protected void applyValue() {
                    Util.serverConfig.setTravelPartialDistanceThreshold(this.value * 1000.0);
                }
            };
            this.addEntry(new NumberConfigEntry(
                travelPartialDistSlider,
                Text.translatable("fmod.options.travelmsg.partial"),
                Text.translatable("fmod.options.hint.travelmsg.partial")
            )); 
            // Player Teleport Threshold
            SliderWidget travelTeleportSlider = new SliderWidget(0, 0, 200, 20,
                Text.literal(String.format("%.1f", Util.serverConfig.getTeleportThreshold())),
                Util.serverConfig.getTeleportThreshold() / 300.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(String.format("%.1f", this.value * 300.0)));
                }

                @Override
                protected void applyValue() {
                    Util.serverConfig.setTeleportThreshold(this.value * 300.0);
                }
            };
            this.addEntry(new NumberConfigEntry(
                travelTeleportSlider,
                Text.translatable("fmod.options.teleport"),
                Text.translatable("fmod.options.hint.teleport")
            ));
            // Player Teleport Message Location
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageLocationI18n(Util.serverConfig.getTeleportMessageLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getTeleportMessageLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setTeleportMessageLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getTeleportMessageLocation()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.tpmsgloc"),
                Text.translatable("fmod.options.hint.tpmsgloc")
            ));
            // Player Teleport Message Receiver
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getTeleportMessageReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getTeleportMessageReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setTeleportMessageReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getTeleportMessageReceiver()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.tpmsgreceiver"),
                Text.translatable("fmod.options.hint.tpmsgreceiver")
            ));
            // GPT Server
            TextFieldWidget gptUrlTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 200, 20, Text.empty());
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
            TextFieldWidget gptTknTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 200, 20, Text.empty());
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
            TextFieldWidget gptMdlTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 200, 20, Text.empty());
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
            TextFieldWidget gptSysPrmptTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 200, 20, Text.empty());
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
            SliderWidget gptTempSlider = new SliderWidget(0, 0, 200, 20, 
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
            SliderWidget gptTimeoutSlider = new SliderWidget(0, 0, 200, 20, 
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
