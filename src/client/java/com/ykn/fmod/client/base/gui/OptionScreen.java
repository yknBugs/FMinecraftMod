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
            SliderWidget flowLengthSlider = new SliderWidget(0, 0, 400, 20, 
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
            // Entity Death Message
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
            // Player Death Coord
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerDeathCoord()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getPlayerDeathCoord());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setPlayerDeathCoord((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerDeathCoord()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.bcdeathcoord"),
                Text.translatable("fmod.options.hint.bcdeathcoord")
            ));
            // Projectile Hits Entity
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileHitOthers()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getProjectileHitOthers());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setProjectileHitOthers((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileHitOthers()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.projhitting"),
                Text.translatable("fmod.options.hint.projhitting")
            ));
            // Projectile Being Hit
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileBeingHit()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getProjectileBeingHit());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setProjectileBeingHit((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileBeingHit()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.projbeinghit"),
                Text.translatable("fmod.options.hint.projbeinghit")
            ));
            // Inform AFK
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getInformAfking()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getInformAfking());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setInformAfking((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getInformAfking()));
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
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBroadcastAfking()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBroadcastAfking());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBroadcastAfking((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBroadcastAfking()));
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
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getStopAfking()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getStopAfking());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setStopAfking((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getStopAfking()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.stopafk"),
                Text.translatable("fmod.options.hint.stopafk")
            ));
            // Change Biome
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getChangeBiome()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getChangeBiome());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setChangeBiome((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getChangeBiome()));
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
            SliderWidget bossFightMsgIntervalSlider = new SliderWidget(0, 0, 400, 20, 
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
            SliderWidget monsterSurroundMsgIntervalSlider = new SliderWidget(0, 0, 400, 20, 
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
            SliderWidget monsterNumSlider = new SliderWidget(0, 0, 400, 20, 
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
            SliderWidget monsterDistanceSlider = new SliderWidget(0, 0, 400, 20, 
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
            // Entity Number
            SliderWidget entityNumSlider = new SliderWidget(0, 0, 400, 20, 
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
            // Entity Interval (Range: 1 ~ 100 Ticks, show in Ticks)
            SliderWidget entityIntervalSlider = new SliderWidget(0, 0, 400, 20, 
                Text.literal(Integer.toString(Util.serverConfig.getEntityNumberInterval())),
                ((double) Util.serverConfig.getEntityNumberInterval() - 1.0) / 99.0 
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.literal(Integer.toString((int) (this.value * 99.0 + 1.0))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setEntityNumberInterval((int) (this.value * 99.0 + 1.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                entityIntervalSlider,
                Text.translatable("fmod.options.entityinterval"),
                Text.translatable("fmod.options.hint.entityinterval")
            ));
            // Player Seriously Hurt
            this.addEntry(new ButtonConfigEntry(
                ButtonWidget.builder(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerSeriousHurt()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getPlayerSeriousHurt());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setPlayerSeriousHurt((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerSeriousHurt()));
                }).size(200, 20).build(),
                Text.translatable("fmod.options.playerhurt"),
                Text.translatable("fmod.options.hint.playerhurt")
            ));
            // Damage Threshold
            SliderWidget damageThresholdSlider = new SliderWidget(0, 0, 400, 20, 
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
