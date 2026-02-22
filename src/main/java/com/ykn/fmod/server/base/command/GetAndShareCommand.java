/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.async.EntityDensityCalculator;
import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.GameMath;
import com.ykn.fmod.server.base.util.ServerMessageType;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class GetAndShareCommand {

    private static ServerPlayer getShareCommandExecutor(CommandContext<CommandSourceStack> context) {
        if (context == null) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.share.playeronly"));
        }
        CommandSourceStack source = context.getSource();
        if (source == null) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.share.playeronly"));
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.share.playeronly"));
        }
        return player;
        // return Optional.ofNullable(context)
        //     .map(CommandContext::getSource)
        //     .map(CommandSourceStack::getPlayer)
        //     .orElseThrow(() -> new CommandRuntimeException(Util.parseTranslateableText("fmod.command.share.playeronly")));
    }

    private static int runGetCoordCommand(Collection<? extends Entity> entities, CommandContext<CommandSourceStack> context) {
        try {
            for (Entity entity : entities) {
                Component name = entity.getDisplayName();
                Component coord = Util.parseCoordText(entity);
                MutableComponent text = Util.parseTranslatableText("fmod.command.get.coord", name, coord);
                context.getSource().sendSuccess(() -> text, false);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get coord", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return entities.size();
    }

    private static int runShareCoordCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            Component name = player.getDisplayName();
            Component coord = Util.parseCoordText(player);
            MutableComponent text = Util.parseTranslatableText("fmod.command.share.coord", name, coord);
            ServerMessageType.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share coord", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static MutableComponent getDirectionText(Vec3 source, Vec3 target) {
        double pitch = GameMath.getPitch(source, target);
        double yaw = GameMath.getYaw(source, target);
        MutableComponent direction = Component.empty();
        if (pitch > 60.0) {
            direction = Util.parseTranslatableText("fmod.misc.diru");
        } else if (pitch < -60.0) {
            direction = Util.parseTranslatableText("fmod.misc.dird");
        } else if (yaw >= -157.5 && yaw < -112.5) {
            direction = Util.parseTranslatableText("fmod.misc.dirne");
        } else if (yaw >= -112.5 && yaw < -67.5) {
            direction = Util.parseTranslatableText("fmod.misc.dire");
        } else if (yaw >= -67.5 && yaw < -22.5) {
            direction = Util.parseTranslatableText("fmod.misc.dirse");
        } else if (yaw >= -22.5 && yaw < 22.5) {
            direction = Util.parseTranslatableText("fmod.misc.dirs");
        } else if (yaw >= 22.5 && yaw < 67.5) {
            direction = Util.parseTranslatableText("fmod.misc.dirsw");
        } else if (yaw >= 67.5 && yaw < 112.5) {
            direction = Util.parseTranslatableText("fmod.misc.dirw");
        } else if (yaw >= 112.5 && yaw < 157.5) {
            direction = Util.parseTranslatableText("fmod.misc.dirnw");
        } else {
            direction = Util.parseTranslatableText("fmod.misc.dirn");
        } 
        return direction;
    }

    private static int runGetDistanceCommand(Collection<? extends Entity> entities, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            Vec3 source = context.getSource().getPosition();
            for (Entity entity : entities) {
                if (context.getSource().getLevel() != entity.level()) {
                    final Component name = entity.getDisplayName();
                    context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.get.dimdistance", name), false);
                    continue;
                }
                Vec3 target = entity.position();
                double distance = GameMath.getEuclideanDistance(source, target);
                double pitch = GameMath.getPitch(source, target);
                double yaw = GameMath.getYaw(source, target);
                double degree = yaw;
                if (pitch > 60.0) {
                    degree = pitch;
                } else if (pitch < -60.0) {
                    degree = -pitch;
                }
                final Component name = entity.getDisplayName();
                final String degStr = String.format("%.2f°", degree);
                final String distStr = String.format("%.2f", distance);
                final MutableComponent dirTxt = getDirectionText(source, target);
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.get.distance", name, dirTxt, degStr, distStr), false);
                result++;
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get distance", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runShareDistanceCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            Vec3 target = player.position();
            List<ServerPlayer> onlinePlayers = Util.getOnlinePlayers(context.getSource().getServer());
            for (ServerPlayer onlinePlayer : onlinePlayers) {
                if (onlinePlayer.getUUID() == player.getUUID()) {
                    ServerMessageType.sendTextMessage(onlinePlayer, Util.parseTranslatableText("fmod.command.share.selfdistance"));
                    continue;
                }
                if (player.level() != onlinePlayer.level()) {
                    final Component name = player.getDisplayName();
                    ServerMessageType.sendTextMessage(onlinePlayer, Util.parseTranslatableText("fmod.command.share.dimdistance", name));
                    continue;
                }
                Vec3 source = onlinePlayer.position();
                double distance = GameMath.getEuclideanDistance(source, target);
                double pitch = GameMath.getPitch(source, target);
                double yaw = GameMath.getYaw(source, target);
                double degree = yaw;
                if (pitch > 60.0) {
                    degree = pitch;
                } else if (pitch < -60.0) {
                    degree = -pitch;
                }
                final Component name = player.getDisplayName();
                final String degStr = String.format("%.2f°", degree);
                final String distStr = String.format("%.2f", distance);
                final MutableComponent dirTxt = getDirectionText(source, target);
                final MutableComponent text = Util.parseTranslatableText("fmod.command.share.distance", name, dirTxt, degStr, distStr);
                ServerMessageType.sendTextMessage(onlinePlayer, text);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share distance", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetHealthCommand(Collection<? extends Entity> entities, CommandContext<CommandSourceStack> context) {
        try {
            for (Entity entity : entities) {
                final Component name = entity.getDisplayName();
                double hp = Util.getHealth(entity);
                double maxhp = Util.getMaxHealth(entity);
                final String hpStr = String.format("%.2f", hp);
                final String maxhpStr = String.format("%.2f", maxhp);
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.get.health", name, hpStr, maxhpStr), false);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get health", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return entities.size();
    }

    private static int runShareHealthCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            double hp = Util.getHealth(player);
            double maxhp = Util.getMaxHealth(player);
            final Component name = player.getDisplayName();
            final String hpStr = String.format("%.2f", hp);
            final String maxhpStr = String.format("%.2f", maxhp);
            MutableComponent text = Util.parseTranslatableText("fmod.command.share.health", name, hpStr, maxhpStr);
            ServerMessageType.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share health", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetStatusCommand(Collection<ServerPlayer> players, CommandContext<CommandSourceStack> context) {
        try {
            for (ServerPlayer player : players) {
                double hp = player.getHealth();
                int hunger = player.getFoodData().getFoodLevel();
                double saturation = player.getFoodData().getSaturationLevel();
                int level = player.experienceLevel;
                final Component name = player.getDisplayName();
                final String hpStr = String.format("%.2f", hp);
                final String hungerStr = String.valueOf(hunger);
                final String saturationStr = String.format("%.2f", saturation);
                final String levelStr = String.valueOf(level);
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.get.status", name, hpStr, hungerStr, saturationStr, levelStr), false);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get status", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return players.size();
    }

    private static int runShareStatusCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            double hp = player.getHealth();
            int hunger = player.getFoodData().getFoodLevel();
            double saturation = player.getFoodData().getSaturationLevel();
            int level = player.experienceLevel;
            final Component name = player.getDisplayName();
            final String hpStr = String.format("%.2f", hp);
            final String hungerStr = String.valueOf(hunger);
            final String saturationStr = String.format("%.2f", saturation);
            final String levelStr = String.valueOf(level);
            MutableComponent text = Util.parseTranslatableText("fmod.command.share.status", name, hpStr, hungerStr, saturationStr, levelStr);
            ServerMessageType.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share status", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static MutableComponent formatInventoryItemStack(ItemStack item) {
        MutableComponent itemText = Component.empty();
        try {
            if (item == null || item.isEmpty()) {
                itemText = Component.literal("00").withStyle(ChatFormatting.GRAY).withStyle(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.command.get.emptyslot")))
                );
            } else if (item.getCount() < 100) {
                String itemCount = String.format("%02d", item.getCount());
                itemText = Component.literal(itemCount).withStyle(ChatFormatting.AQUA).withStyle(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(item)))
                );
            } else {
                itemText = Component.literal("9+").withStyle(ChatFormatting.AQUA).withStyle(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(item)))
                );
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when formatting item stack", e);
            itemText = Component.literal("??").withStyle(ChatFormatting.RED);
        }
        return itemText;
    }

    private static List<MutableComponent> getInventoryTexts(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        // Text Structure:
        // [x] [x] [x] [x] [-] [-] [S] [+] [1]   (x: Armor, -: Placeholder, +: Offhand, 1: Current Chosen Slot Index)
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, S: Survival Gamemode [S: Survival, C: Creative, A: Adventure, V: Spectator])
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, '+' symbol formatting: [Has Item: Formatting.AQUA, Empty Slot: Formatting.GRAY])
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, '[]' bracket formmating: Formatting.GREEN)
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Hotbar, '[]' bracket formmating: [Selected: Formatting.GOLD, Other: Formatting.LIGHT_PURPLE])
        MutableComponent armorText = Component.empty();
        for (int i = 0; i < 4; i++) {
            ItemStack item = inventory.getArmor(i);
            armorText.append(Component.literal("[").withStyle(ChatFormatting.LIGHT_PURPLE));
            armorText.append(formatInventoryItemStack(item));
            armorText.append(Component.literal("]").withStyle(ChatFormatting.LIGHT_PURPLE));
            armorText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        }
        // Placeholder
        for (int i = 0; i < 2; i++) {
            armorText.append(Component.literal("[--]").withStyle(ChatFormatting.GRAY));
            armorText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        }
        // Gamemode
        armorText.append(Component.literal("[").withStyle(ChatFormatting.GOLD));
        GameType gamemode = player.gameMode.getGameModeForPlayer();
        MutableComponent gamemodeText = Component.literal("+S");
        if (gamemode == GameType.CREATIVE) {
            gamemodeText = Component.literal("+C");
        } else if (gamemode == GameType.ADVENTURE) {
            gamemodeText = Component.literal("+A");
        } else if (gamemode == GameType.SPECTATOR) {
            gamemodeText = Component.literal("+V");
        }
        gamemodeText = gamemodeText.withStyle(ChatFormatting.RED).withStyle(s -> s
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("gameMode." + gamemode.getName())))
        );
        armorText.append(gamemodeText);
        armorText.append(Component.literal("]").withStyle(ChatFormatting.GOLD));
        armorText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        // Offhand
        ItemStack offhandItem = inventory.getItem(Inventory.SLOT_OFFHAND);
        armorText.append(Component.literal("[").withStyle(ChatFormatting.LIGHT_PURPLE));
        armorText.append(formatInventoryItemStack(offhandItem));
        armorText.append(Component.literal("]").withStyle(ChatFormatting.LIGHT_PURPLE));
        armorText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        // Selected Slot
        armorText.append(Component.literal("[").withStyle(ChatFormatting.GOLD));
        armorText.append(Component.literal("0" + String.valueOf(inventory.selected + 1)).withStyle(ChatFormatting.RED));
        armorText.append(Component.literal("]").withStyle(ChatFormatting.GOLD));
        armorText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        // Inventory
        MutableComponent[] inventoryText = {Component.empty(), Component.empty(), Component.empty()};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                // Index 0 ~ 8 belongs to Hotbar, Index 9 ~ 35 belongs to Inventory
                int index = (i + 1) * 9 + j;
                ItemStack item = inventory.getItem(index);
                inventoryText[i].append(Component.literal("[").withStyle(ChatFormatting.GREEN));
                inventoryText[i].append(formatInventoryItemStack(item));
                inventoryText[i].append(Component.literal("]").withStyle(ChatFormatting.GREEN));
                inventoryText[i].append(Component.literal(" ").withStyle(ChatFormatting.RESET));
            }
        }
        // Hotbar
        MutableComponent hotbarText = Component.empty();
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (i == inventory.selected) {
                hotbarText.append(Component.literal("[").withStyle(ChatFormatting.GOLD));
            } else {
                hotbarText.append(Component.literal("[").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            hotbarText.append(formatInventoryItemStack(item));
            if (i == inventory.selected) {
                hotbarText.append(Component.literal("]").withStyle(ChatFormatting.GOLD));
            } else {
                hotbarText.append(Component.literal("]").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            hotbarText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        }
        // Feedback
        final MutableComponent linea = armorText;
        final MutableComponent lineb = inventoryText[0];
        final MutableComponent linec = inventoryText[1];
        final MutableComponent lined = inventoryText[2];
        final MutableComponent linee = hotbarText;
        return Arrays.asList(linea, lineb, linec, lined, linee);
    }

    private static int runGetInventoryCommand(ServerPlayer player, CommandContext<CommandSourceStack> context) {
        try {
            List<MutableComponent> inventoryText = getInventoryTexts(player);
            final Component name = player.getDisplayName();
            final Component title = Util.parseTranslatableText("fmod.command.get.inventory", name);
            context.getSource().sendSuccess(() -> title, false);
            for (MutableComponent text : inventoryText) {
                context.getSource().sendSuccess(() -> text, false);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get inventory", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runShareInventoryCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            List<MutableComponent> inventoryText = getInventoryTexts(player);
            final Component name = player.getDisplayName();
            final Component title = Util.parseTranslatableText("fmod.command.share.inventory", name);
            ServerMessageType.broadcastTextMessage(context.getSource().getServer(), title);
            for (MutableComponent text : inventoryText) {
                ServerMessageType.broadcastTextMessage(context.getSource().getServer(), text);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share inventory", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetItemCommand(Collection<? extends Entity> entities, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            for (Entity entity : entities) {
                Iterable<ItemStack> items = entity.getHandSlots();
                if (items == null || items.iterator().hasNext() == false) {
                    final Component name = entity.getDisplayName();
                    context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.get.noitem", name), false);
                    continue;
                }
                MutableComponent itemList = Component.empty();
                int itemCountSum = 0;
                for (ItemStack item : items) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    Component itemText = item.getDisplayName();
                    int itemCount = item.getCount();
                    result += itemCount;
                    itemCountSum += itemCount;
                    itemList.append(itemText);
                    if (itemCount > 1) {
                        itemList.append(Component.literal("x" + itemCount + " "));
                    } else {
                        itemList.append(Component.literal(" "));
                    }
                }
                final Component name = entity.getDisplayName();
                final MutableComponent itemTxt = itemList;
                if (itemCountSum <= 0) {
                    context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.get.noitem", name), false);
                } else {
                    context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.get.item", name, itemTxt), false);
                }
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get item", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runShareItemCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            Iterable<ItemStack> items = player.getHandSlots();
            if (items == null || items.iterator().hasNext() == false) {
                final Component name = player.getDisplayName();
                ServerMessageType.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslatableText("fmod.command.share.noitem", name));
                return Command.SINGLE_SUCCESS;
            }
            MutableComponent itemList = Component.empty();
            int itemCountSum = 0;
            for (ItemStack item : items) {
                if (item.isEmpty()) {
                    continue;
                }
                Component itemText = item.getDisplayName();
                int itemCount = item.getCount();
                itemCountSum += itemCount;
                itemList.append(itemText);
                if (itemCount > 1) {
                    itemList.append(Component.literal("x" + itemCount + " "));
                } else {
                    itemList.append(Component.literal(" "));
                }
            }
            final Component name = player.getDisplayName();
            final MutableComponent itemTxt = itemList;
            if (itemCountSum <= 0) {
                ServerMessageType.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslatableText("fmod.command.share.noitem", name));
            } else {
                ServerMessageType.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslatableText("fmod.command.share.item", name, itemTxt));
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share item", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetAfkTimeCommand(Collection<ServerPlayer> players, CommandContext<CommandSourceStack> context) {
        try {
            for (ServerPlayer player : players) {
                PlayerData data = Util.getServerData(context.getSource().getServer()).getPlayerData(player);
                double afkSeconds = data.afkTicks / 20.0;
                final String afkSecondsStr = String.format("%.1f", afkSeconds);
                final Component name = player.getDisplayName();
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.get.afk", name, afkSecondsStr), false);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get afk", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return players.size();
    }

    private static int runGetTravelRecordCommand(Collection<ServerPlayer> players, CommandContext<CommandSourceStack> context) {
        try {
            for (ServerPlayer player : players) {
                PlayerData data = Util.getServerData(context.getSource().getServer()).getPlayerData(player);
                Vec3[] snapshot = data.recentPositions.toArray(new Vec3[0]);
                double seconds = (snapshot.length - 1) / 20.0;
                double totalDistance = GameMath.getHorizonalEuclideanDistance(snapshot[0], snapshot[snapshot.length - 1]);
                double totalTravelled = 0.0;
                for (int i = 1; i < snapshot.length; i++) {
                    totalTravelled += GameMath.getHorizonalEuclideanDistance(snapshot[i - 1], snapshot[i]);
                }
                final Component name = player.getDisplayName();
                final String secondsStr = String.format("%.1f", seconds);
                final String totalDistanceStr = String.format("%.1f", totalDistance);
                final String avgSpeedStr = String.format("%.1f", (totalDistance / seconds));
                final String totalTravelledStr = String.format("%.1f", totalTravelled);
                final String avgTravelSpeedStr = String.format("%.1f", (totalTravelled / seconds));
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.get.travel", name, secondsStr, totalDistanceStr, avgSpeedStr, totalTravelledStr, avgTravelSpeedStr), false);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get travel", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return players.size();
    }

    private static int runGetCrowdedPlaceCommand(int number, double radius, CommandContext<CommandSourceStack> context) {
        try {
            List<Entity> allEntities = new ArrayList<>();
            for (ServerLevel world : context.getSource().getServer().getAllLevels()) {
                List<Entity> entities = Util.getAllEntities(world);
                allEntities.addAll(entities);
            }
            EntityDensityCalculator calculator = new EntityDensityCalculator(context, allEntities, radius, number);
            ServerData serverData = Util.getServerData(context.getSource().getServer());
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.get.crowd"), false);
            serverData.submitAsyncTask(calculator);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get crowd", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildGetCommand() {
        return Commands.literal("get")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("coord")
                .then(Commands.argument("entity", EntityArgument.entities())
                    .executes(context -> {return runGetCoordCommand(EntityArgument.getEntities(context, "entity"), context);})
                )
            )
            .then(Commands.literal("distance")
                .then(Commands.argument("entity", EntityArgument.entities())
                    .executes(context -> {return runGetDistanceCommand(EntityArgument.getEntities(context, "entity"), context);})
                )
            )
            .then(Commands.literal("health")
                .then(Commands.argument("entity", EntityArgument.entities())
                    .executes(context -> {return runGetHealthCommand(EntityArgument.getEntities(context, "entity"), context);})
                )
            )
            .then(Commands.literal("status")
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(context -> {return runGetStatusCommand(EntityArgument.getPlayers(context, "player"), context);})
                )
            )
            .then(Commands.literal("inventory")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {return runGetInventoryCommand(EntityArgument.getPlayer(context, "player"), context);})
                )
            )
            .then(Commands.literal("item")
                .then(Commands.argument("entity", EntityArgument.entities())
                    .executes(context -> {return runGetItemCommand(EntityArgument.getEntities(context, "entity"), context);})
                )
            )
            .then(Commands.literal("afk")
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(context -> {return runGetAfkTimeCommand(EntityArgument.getPlayers(context, "player"), context);})
                )
            )
            .then(Commands.literal("travel")
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(context -> {return runGetTravelRecordCommand(EntityArgument.getPlayers(context, "player"), context);})
                )
            )
            .then(Commands.literal("crowd")
                .then(Commands.argument("number", IntegerArgumentType.integer(1))
                    .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.0))
                        .executes(context -> {return runGetCrowdedPlaceCommand(IntegerArgumentType.getInteger(context, "number"), DoubleArgumentType.getDouble(context, "radius"), context);})
                    )
                )
                .executes(context -> {return runGetCrowdedPlaceCommand(Util.serverConfig.getEntityDensityNumber(), Util.serverConfig.getEntityDensityRadius(), context);})
            );
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildShareCommand() {
        return Commands.literal("share")
            .requires(source -> source.hasPermission(0))
            .then(Commands.literal("coord").executes(context -> {return runShareCoordCommand(context);}))
            .then(Commands.literal("distance").executes(context -> {return runShareDistanceCommand(context);}))
            .then(Commands.literal("health").executes(context -> {return runShareHealthCommand(context);}))
            .then(Commands.literal("status").executes(context -> {return runShareStatusCommand(context);}))
            .then(Commands.literal("inventory").executes(context -> {return runShareInventoryCommand(context);}))
            .then(Commands.literal("item").executes(context -> {return runShareItemCommand(context);}));
    }
    
}
