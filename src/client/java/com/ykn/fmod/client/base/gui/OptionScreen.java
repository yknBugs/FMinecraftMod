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
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class OptionScreen extends Screen {
    private final Screen parent;
    private ConfigWidget configWidget;
    
    public OptionScreen(Screen parent) {
        super(new TranslatableText("fmod.options.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        this.configWidget = new ConfigWidget(this.client, this.width, this.height - 80, 40, this.height - 40);
        this.addSelectableChild(this.configWidget);

        this.addDrawableChild(
            new ButtonWidget(this.width / 2 - 100, this.height - 30, 200, 20, ScreenTexts.DONE, button -> {
                Util.saveServerConfig();
                this.client.setScreen(this.parent);
            })
        );
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.configWidget.render(matrices, mouseX, mouseY, delta);
        DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 20, 0xffffff);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        Util.saveServerConfig();
        super.removed();
    }

    @Override
    public void onClose() {
        Util.saveServerConfig();
        this.client.setScreen(this.parent);
    }

    private class ConfigWidget extends ElementListWidget<ConfigWidget.Entry> {
        public ConfigWidget(MinecraftClient client, int width, int height, int top, int bottom) {
            // 630 234 40 274
            super(client, width, height, top, bottom, 24);
            // Copyright Info
            this.addEntry(new TextHintEntry(
                new TranslatableText("fmod.misc.version", Util.getMinecraftVersion(), Util.getModVersion(), Util.getModAuthors()),
                new TranslatableText("fmod.options.tip")
            ));
            // Server Translation
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, getBoolStateText(Util.serverConfig.isEnableServerTranslation()), button -> {
                    Util.serverConfig.setEnableServerTranslation(!Util.serverConfig.isEnableServerTranslation());
                    button.setMessage(getBoolStateText(Util.serverConfig.isEnableServerTranslation()));
                }),
                new TranslatableText("fmod.options.translate"),
                new TranslatableText("fmod.options.hint.translate")
            ));
            // Entity Death Message
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityDeathMessage()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getEntityDeathMessage());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setEntityDeathMessage((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityDeathMessage()));
                }),
                new TranslatableText("fmod.options.entdeathmsg"),
                new TranslatableText("fmod.options.hint.entdeathmsg")
            ));
            // Boss Death Message
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossDeathMessage()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBossDeathMessage());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBossDeathMessage((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossDeathMessage()));
                }),
                new TranslatableText("fmod.options.bcbossdeath"),
                new TranslatableText("fmod.options.hint.bcbossdeath")
            ));
            // Named Entity Death Message
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageLocationI18n(Util.serverConfig.getNamedEntityDeathMessage()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getNamedEntityDeathMessage());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setNamedEntityDeathMessage((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getNamedEntityDeathMessage()));
                }),
                new TranslatableText("fmod.options.nameddeath"),
                new TranslatableText("fmod.options.hint.nameddeath")
            ));
            // Killer Entity Death Message
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageLocationI18n(Util.serverConfig.getKillerEntityDeathMessage()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getKillerEntityDeathMessage());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setKillerEntityDeathMessage((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getKillerEntityDeathMessage()));
                }),
                new TranslatableText("fmod.options.bckillerdeath"),
                new TranslatableText("fmod.options.hint.bckillerdeath")
            ));
            // Boss Max Health Threshold
            SliderWidget bossMaxHealthSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(String.format("%.1f", Util.serverConfig.getBossMaxHpThreshold())),
                Util.serverConfig.getBossMaxHpThreshold() / 1000.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(String.format("%.1f", this.value * 1000.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setBossMaxHpThreshold(this.value * 1000.0);
                }
            };
            this.addEntry(new NumberConfigEntry(
                bossMaxHealthSlider,
                new TranslatableText("fmod.options.bossmaxhp"),
                new TranslatableText("fmod.options.hint.bossmaxhp")
            ));
            // Player Death Coord
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerDeathCoord()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getPlayerDeathCoord());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setPlayerDeathCoord((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerDeathCoord()));
                }),
                new TranslatableText("fmod.options.bcdeathcoord"),
                new TranslatableText("fmod.options.hint.bcdeathcoord")
            ));
            // Projectile Hits Entity
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileHitOthers()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getProjectileHitOthers());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setProjectileHitOthers((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileHitOthers()));
                }),
                new TranslatableText("fmod.options.projhitting"),
                new TranslatableText("fmod.options.hint.projhitting")
            ));
            // Projectile Being Hit
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileBeingHit()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getProjectileBeingHit());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setProjectileBeingHit((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileBeingHit()));
                }),
                new TranslatableText("fmod.options.projbeinghit"),
                new TranslatableText("fmod.options.hint.projbeinghit")
            ));
            // Inform AFK
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageReceiverI18n(Util.serverConfig.getInformAfking()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getInformAfking());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setInformAfking((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getInformAfking()));
                }),
                new TranslatableText("fmod.options.informafk"),
                new TranslatableText("fmod.options.hint.informafk")
            ));
            // Inform AFK Threshold
            SliderWidget afkThresholdSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(String.format("%.2f", (double) Util.serverConfig.getInformAfkingThreshold() / 20.0)),
                Util.serverConfig.getInformAfkingThreshold() / 20.0 / 1800.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(String.format("%.2f", this.value * 1800.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setInformAfkingThreshold((int) (this.value * 1800.0 * 20.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                afkThresholdSlider,
                new TranslatableText("fmod.options.informafkthres"),
                new TranslatableText("fmod.options.hint.informafkthres")
            ));
            // Broadcast AFK
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBroadcastAfking()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBroadcastAfking());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBroadcastAfking((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBroadcastAfking()));
                }),
                new TranslatableText("fmod.options.bcafk"),
                new TranslatableText("fmod.options.hint.bcafk")
            ));
            // Broadcast AFK Threshold
            SliderWidget bcastAfkThresholdSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(String.format("%.2f", (double) Util.serverConfig.getBroadcastAfkingThreshold() / 20.0)),
                Util.serverConfig.getBroadcastAfkingThreshold() / 20.0 / 1800.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(String.format("%.2f", this.value * 1800.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setBroadcastAfkingThreshold((int) (this.value * 1800.0 * 20.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                bcastAfkThresholdSlider,
                new TranslatableText("fmod.options.bcafkthres"),
                new TranslatableText("fmod.options.hint.bcafkthres")
            ));
            // Back From AFK
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageReceiverI18n(Util.serverConfig.getStopAfking()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getStopAfking());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setStopAfking((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getStopAfking()));
                }),
                new TranslatableText("fmod.options.stopafk"),
                new TranslatableText("fmod.options.hint.stopafk")
            ));
            // Change Biome
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageReceiverI18n(Util.serverConfig.getChangeBiome()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getChangeBiome());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setChangeBiome((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getChangeBiome()));
                }),
                new TranslatableText("fmod.options.changebiome"),
                new TranslatableText("fmod.options.hint.changebiome")
            ));
            // Change Biome Delay
            SliderWidget changeBiomeDelaySlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(String.format("%.2f", (double) Util.serverConfig.getChangeBiomeDelay() / 20.0)),
                Util.serverConfig.getChangeBiomeDelay() / 20.0 / 60.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(String.format("%.2f", this.value * 60.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setChangeBiomeDelay((int) (this.value * 60.0 * 20.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                changeBiomeDelaySlider,
                new TranslatableText("fmod.options.biomedelay"),
                new TranslatableText("fmod.options.hint.biomedelay")
            ));
            // Boss Fight Message Location
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossFightMessageLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBossFightMessageLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBossFightMessageLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossFightMessageLocation()));
                }),
                new TranslatableText("fmod.options.bossfightloc"),
                new TranslatableText("fmod.options.hint.bossfightloc")
            ));
            // Boss Fight Message Receiver
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBossFightMessageReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getBossFightMessageReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setBossFightMessageReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBossFightMessageReceiver()));
                }),
                new TranslatableText("fmod.options.bossfightreceiver"),
                new TranslatableText("fmod.options.hint.bossfightreceiver")
            ));
            // Boss Fight Message Interval
            SliderWidget bossFightMsgIntervalSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(String.format("%.2f", (double) Util.serverConfig.getBossFightInterval() / 20.0)),
                Util.serverConfig.getBossFightInterval() / 20.0 / 180.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(String.format("%.2f", this.value * 180.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setBossFightInterval((int) (this.value * 180.0 * 20.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                bossFightMsgIntervalSlider,
                new TranslatableText("fmod.options.bossfightinterval"),
                new TranslatableText("fmod.options.hint.bossfightinterval")
            ));
            // Monster Surrounded Message Location
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageLocationI18n(Util.serverConfig.getMonsterSurroundMessageLocation()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getMonsterSurroundMessageLocation());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setMonsterSurroundMessageLocation((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getMonsterSurroundMessageLocation()));
                }),
                new TranslatableText("fmod.options.monsterloc"),
                new TranslatableText("fmod.options.hint.monsterloc")
            ));
            // Monster Surrounded Message Receiver
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageReceiverI18n(Util.serverConfig.getMonsterSurroundMessageReceiver()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getMonsterSurroundMessageReceiver());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setMonsterSurroundMessageReceiver((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getMonsterSurroundMessageReceiver()));
                }),
                new TranslatableText("fmod.options.monsterreceiver"),
                new TranslatableText("fmod.options.hint.monsterreceiver")
            ));
            // Monster Surrounded Message Interval
            SliderWidget monsterSurroundMsgIntervalSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(String.format("%.2f", (double) Util.serverConfig.getMonsterSurroundInterval() / 20.0)),
                Util.serverConfig.getMonsterSurroundInterval() / 20.0 / 180.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(String.format("%.2f", this.value * 180.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setMonsterSurroundInterval((int) (this.value * 180.0 * 20.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                monsterSurroundMsgIntervalSlider,
                new TranslatableText("fmod.options.monsterinterval"),
                new TranslatableText("fmod.options.hint.monsterinterval")
            ));
            // Monster Number
            SliderWidget monsterNumSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(Integer.toString(Util.serverConfig.getMonsterNumberThreshold())),
                (double) Util.serverConfig.getMonsterNumberThreshold() / 100.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(Integer.toString((int) (this.value * 100.0))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setMonsterNumberThreshold((int) (this.value * 100.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                monsterNumSlider,
                new TranslatableText("fmod.options.monsternumber"),
                new TranslatableText("fmod.options.hint.monsternumber")
            ));
            // Monster Distance
            SliderWidget monsterDistanceSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(String.format("%.2f", Util.serverConfig.getMonsterDistanceThreshold())),
                Util.serverConfig.getMonsterDistanceThreshold() / 128.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(String.format("%.2f", this.value * 128.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setMonsterDistanceThreshold(this.value * 128.0);
                }
            };
            this.addEntry(new NumberConfigEntry(
                monsterDistanceSlider,
                new TranslatableText("fmod.options.monsterdistance"),
                new TranslatableText("fmod.options.hint.monsterdistance")
            ));
            // Entity Warning
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityNumberWarning()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageLocation.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getEntityNumberWarning());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setEntityNumberWarning((MessageLocation) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityNumberWarning()));
                }),
                new TranslatableText("fmod.options.entitywarning"),
                new TranslatableText("fmod.options.hint.entitywarning")
            ));
            // Entity Number
            SliderWidget entityNumSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(Integer.toString(Util.serverConfig.getEntityNumberThreshold())),
                (double) Util.serverConfig.getEntityNumberThreshold() / 10000.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(Integer.toString((int) (this.value * 10000.0))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setEntityNumberThreshold((int) (this.value * 10000.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                entityNumSlider,
                new TranslatableText("fmod.options.entitynumber"),
                new TranslatableText("fmod.options.hint.entitynumber")
            ));
            // Entity Interval (Range: 1 ~ 100 Ticks, show in Ticks)
            SliderWidget entityIntervalSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(Integer.toString(Util.serverConfig.getEntityNumberInterval())),
                ((double) Util.serverConfig.getEntityNumberInterval() - 1.0) / 99.0 
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(Integer.toString((int) (this.value * 99.0 + 1.0))));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setEntityNumberInterval((int) (this.value * 99.0 + 1.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                entityIntervalSlider,
                new TranslatableText("fmod.options.entityinterval"),
                new TranslatableText("fmod.options.hint.entityinterval")
            ));
            // Player Seriously Hurt
            this.addEntry(new ButtonConfigEntry(
                new ButtonWidget(0, 0, 200, 20, EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerSeriousHurt()), button -> {
                    final List<Enum<?>> values = Arrays.asList(MessageReceiver.values());
                    int currentIndex = values.indexOf(Util.serverConfig.getPlayerSeriousHurt());
                    currentIndex = (currentIndex + 1) % values.size();
                    Util.serverConfig.setPlayerSeriousHurt((MessageReceiver) values.get(currentIndex));
                    button.setMessage(EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerSeriousHurt()));
                }),
                new TranslatableText("fmod.options.playerhurt"),
                new TranslatableText("fmod.options.hint.playerhurt")
            ));
            // Damage Threshold
            SliderWidget damageThresholdSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(String.format("%.1f", Util.serverConfig.getPlayerHurtThreshold() * 100.0) + "%"),
                Util.serverConfig.getPlayerHurtThreshold()
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(String.format("%.1f", this.value * 100.0) + "%"));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setPlayerHurtThreshold(this.value);
                }
            };
            this.addEntry(new NumberConfigEntry(
                damageThresholdSlider,
                new TranslatableText("fmod.options.damagethres"),
                new TranslatableText("fmod.options.hint.damagethres")
            ));
            // GPT Server
            TextFieldWidget gptUrlTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 400, 20, new LiteralText(""));
            gptUrlTxtWgt.setMaxLength(1024);
            gptUrlTxtWgt.setEditable(true);
            gptUrlTxtWgt.setText(Util.serverConfig.getGptUrl());
            gptUrlTxtWgt.setChangedListener(s -> Util.serverConfig.setGptUrl(s));
            this.addEntry(new StringConfigEntry(
                gptUrlTxtWgt,
                new TranslatableText("fmod.options.gpturl"),
                new TranslatableText("fmod.options.hint.gpturl")
            ));
            // GPT Tokens (For security reasons, this field is not editable)
            TextFieldWidget gptTknTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 400, 20, new LiteralText(""));
            gptTknTxtWgt.setMaxLength(1024);
            gptTknTxtWgt.setEditable(false);
            gptTknTxtWgt.setText(Util.serverConfig.getSecureGptAccessTokens());
            // gptTknTxtWgt.setTooltip(Tooltip.of(new TranslatableText("fmod.options.gptkey.noedit")));
            this.addEntry(new StringConfigEntry(
                gptTknTxtWgt,
                new TranslatableText("fmod.options.gptkey"),
                new TranslatableText("fmod.options.hint.gptkey")
            ));
            // GPT Models
            TextFieldWidget gptMdlTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 400, 20, new LiteralText(""));
            gptMdlTxtWgt.setMaxLength(1024);
            gptMdlTxtWgt.setEditable(true);
            gptMdlTxtWgt.setText(Util.serverConfig.getGptModel());
            gptMdlTxtWgt.setChangedListener(s -> Util.serverConfig.setGptModel(s));
            this.addEntry(new StringConfigEntry(
                gptMdlTxtWgt,
                new TranslatableText("fmod.options.gptmodel"),
                new TranslatableText("fmod.options.hint.gptmodel")
            ));
            // GPT System Prompt
            TextFieldWidget gptSysPrmptTxtWgt = new TextFieldWidget(client.textRenderer, 0, 0, 400, 20, new LiteralText(""));
            gptSysPrmptTxtWgt.setMaxLength(1024);
            gptSysPrmptTxtWgt.setEditable(true);
            gptSysPrmptTxtWgt.setText(Util.serverConfig.getGptSystemPrompt());
            gptSysPrmptTxtWgt.setChangedListener(s -> Util.serverConfig.setGptSystemPrompt(s));
            this.addEntry(new StringConfigEntry(
                gptSysPrmptTxtWgt,
                new TranslatableText("fmod.options.gptsysprompt"),
                new TranslatableText("fmod.options.hint.gptsysprompt")
            ));
            // GPT Temperature
            SliderWidget gptTempSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(String.format("%.2f", Util.serverConfig.getGptTemperature())),
                Util.serverConfig.getGptTemperature()
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(String.format("%.2f", this.value)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setGptTemperature(this.value);
                }
            };
            this.addEntry(new NumberConfigEntry(
                gptTempSlider,
                new TranslatableText("fmod.options.gpttemperature"),
                new TranslatableText("fmod.options.hint.gpttemperature")
            ));
            // GPT Server Timeout
            SliderWidget gptTimeoutSlider = new SliderWidget(0, 0, 400, 20, 
                new LiteralText(String.format("%.3f", Util.serverConfig.getGptServerTimeout() / 1000.0)),
                Util.serverConfig.getGptServerTimeout() / 1000.0 / 60.0
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(new LiteralText(String.format("%.3f", this.value * 60.0)));
                }
                
                @Override
                protected void applyValue() {
                    Util.serverConfig.setGptServerTimeout((int) (this.value * 60.0 * 1000.0));
                }
            };
            this.addEntry(new NumberConfigEntry(
                gptTimeoutSlider,
                new TranslatableText("fmod.options.gpttimeout"),
                new TranslatableText("fmod.options.hint.gpttimeout")
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
            private final Text text;
    
            public TextHintEntry(MutableText text, MutableText tips) {
                this.text = text.styled(s -> s.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        tips
                    ))
                );
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                DrawableHelper.drawCenteredText(matrices, client.textRenderer, text, mouseX, mouseY, 0xffffff);
            }
    
            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of();
            }

            @Override
            public List<? extends Element> children() {
                return List.of();
            }
        }

        private class ButtonConfigEntry extends Entry {
            private final ButtonWidget button;
            private final Text text;

            ButtonConfigEntry(ButtonWidget button, MutableText text, MutableText hint) {
                this.button = button;
                this.text = text.styled(s -> s.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        hint
                    ))
                );
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                DrawableHelper.drawCenteredText(matrices, client.textRenderer, text, x, y, 0xffffff);
                // button.setX(x + entryWidth - button.getWidth());
                // button.setY(y);
                button.render(matrices, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of(button);
            }

            @Override
            public List<? extends Element> children() {
                return List.of(button);
            }
        }

        private class StringConfigEntry extends Entry {
            private final TextFieldWidget textField;
            private final Text text;

            StringConfigEntry(TextFieldWidget textField, MutableText text, MutableText hint) {
                this.textField = textField;
                this.text = text.styled(s -> s.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        hint
                    ))
                );
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                DrawableHelper.drawCenteredText(matrices, client.textRenderer, text, x, y, 0xffffff);
                textField.setX(x + entryWidth - textField.getWidth());
                // textField.setY(y);
                textField.render(matrices, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of(textField);
            }

            @Override
            public List<? extends Element> children() {
                return List.of(textField);
            }
        }

        private class NumberConfigEntry extends Entry {
            private final Text text;
            private final SliderWidget sliderWidget;

            NumberConfigEntry(SliderWidget sliderWidget, MutableText text, MutableText hint) {
                this.sliderWidget = sliderWidget;
                this.text = text.styled(s -> s.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        hint
                    ))
                );
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                DrawableHelper.drawCenteredText(matrices, client.textRenderer, text, x, y, 0xffffff);
                // sliderWidget.setX(x + entryWidth - sliderWidget.getWidth());
                // sliderWidget.setY(y);
                sliderWidget.render(matrices, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of(sliderWidget);
            }

            @Override
            public List<? extends Element> children() {
                return List.of(sliderWidget);
            }
        }
    }

    public static Text getBoolStateText(boolean state) {
        return state ? ScreenTexts.ON : ScreenTexts.OFF;
    }
}
