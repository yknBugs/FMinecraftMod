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

import net.minecraft.command.CommandException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class GetAndShareCommand {

    private static ServerPlayerEntity getShareCommandExecutor(CommandContext<ServerCommandSource> context) {
        if (context == null) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.share.playeronly"));
        }
        ServerCommandSource source = context.getSource();
        if (source == null) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.share.playeronly"));
        }
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.share.playeronly"));
        }
        return player;
        // return Optional.ofNullable(context)
        //     .map(CommandContext::getSource)
        //     .map(ServerCommandSource::getPlayer)
        //     .orElseThrow(() -> new CommandException(Util.parseTranslateableText("fmod.command.share.playeronly")));
    }

    private static int runGetCoordCommand(Collection<? extends Entity> entities, CommandContext<ServerCommandSource> context) {
        try {
            for (Entity entity : entities) {
                Text name = entity.getDisplayName();
                Text coord = Util.parseCoordText(entity);
                MutableText text = Util.parseTranslatableText("fmod.command.get.coord", name, coord);
                context.getSource().sendFeedback(() -> text, false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get coord", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return entities.size();
    }

    private static int runShareCoordCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            Text name = player.getDisplayName();
            Text coord = Util.parseCoordText(player);
            MutableText text = Util.parseTranslatableText("fmod.command.share.coord", name, coord);
            ServerMessageType.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share coord", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static MutableText getDirectionText(Vec3d source, Vec3d target) {
        double pitch = GameMath.getPitch(source, target);
        double yaw = GameMath.getYaw(source, target);
        MutableText direction = Text.empty();
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

    private static int runGetDistanceCommand(Collection<? extends Entity> entities, CommandContext<ServerCommandSource> context) {
        int result = 0;
        try {
            Vec3d source = context.getSource().getPosition();
            for (Entity entity : entities) {
                if (context.getSource().getWorld() != entity.getWorld()) {
                    final Text name = entity.getDisplayName();
                    context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.get.dimdistance", name), false);
                    continue;
                }
                Vec3d target = entity.getPos();
                double distance = GameMath.getEuclideanDistance(source, target);
                double pitch = GameMath.getPitch(source, target);
                double yaw = GameMath.getYaw(source, target);
                double degree = yaw;
                if (pitch > 60.0) {
                    degree = pitch;
                } else if (pitch < -60.0) {
                    degree = -pitch;
                }
                final Text name = entity.getDisplayName();
                final String degStr = String.format("%.2f°", degree);
                final String distStr = String.format("%.2f", distance);
                final MutableText dirTxt = getDirectionText(source, target);
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.get.distance", name, dirTxt, degStr, distStr), false);
                result++;
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get distance", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runShareDistanceCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            Vec3d target = player.getPos();
            List<ServerPlayerEntity> onlinePlayers = Util.getOnlinePlayers(context.getSource().getServer());
            for (ServerPlayerEntity onlinePlayer : onlinePlayers) {
                if (onlinePlayer.getUuid() == player.getUuid()) {
                    ServerMessageType.sendTextMessage(onlinePlayer, Util.parseTranslatableText("fmod.command.share.selfdistance"));
                    continue;
                }
                if (player.getWorld() != onlinePlayer.getWorld()) {
                    final Text name = player.getDisplayName();
                    ServerMessageType.sendTextMessage(onlinePlayer, Util.parseTranslatableText("fmod.command.share.dimdistance", name));
                    continue;
                }
                Vec3d source = onlinePlayer.getPos();
                double distance = GameMath.getEuclideanDistance(source, target);
                double pitch = GameMath.getPitch(source, target);
                double yaw = GameMath.getYaw(source, target);
                double degree = yaw;
                if (pitch > 60.0) {
                    degree = pitch;
                } else if (pitch < -60.0) {
                    degree = -pitch;
                }
                final Text name = player.getDisplayName();
                final String degStr = String.format("%.2f°", degree);
                final String distStr = String.format("%.2f", distance);
                final MutableText dirTxt = getDirectionText(source, target);
                final MutableText text = Util.parseTranslatableText("fmod.command.share.distance", name, dirTxt, degStr, distStr);
                ServerMessageType.sendTextMessage(onlinePlayer, text);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share distance", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetHealthCommand(Collection<? extends Entity> entities, CommandContext<ServerCommandSource> context) {
        try {
            for (Entity entity : entities) {
                final Text name = entity.getDisplayName();
                double hp = Util.getHealth(entity);
                double maxhp = Util.getMaxHealth(entity);
                final String hpStr = String.format("%.2f", hp);
                final String maxhpStr = String.format("%.2f", maxhp);
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.get.health", name, hpStr, maxhpStr), false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get health", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return entities.size();
    }

    private static int runShareHealthCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            double hp = Util.getHealth(player);
            double maxhp = Util.getMaxHealth(player);
            final Text name = player.getDisplayName();
            final String hpStr = String.format("%.2f", hp);
            final String maxhpStr = String.format("%.2f", maxhp);
            MutableText text = Util.parseTranslatableText("fmod.command.share.health", name, hpStr, maxhpStr);
            ServerMessageType.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share health", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetStatusCommand(Collection<ServerPlayerEntity> players, CommandContext<ServerCommandSource> context) {
        try {
            for (ServerPlayerEntity player : players) {
                double hp = player.getHealth();
                int hunger = player.getHungerManager().getFoodLevel();
                double saturation = player.getHungerManager().getSaturationLevel();
                int level = player.experienceLevel;
                final Text name = player.getDisplayName();
                final String hpStr = String.format("%.2f", hp);
                final String hungerStr = String.valueOf(hunger);
                final String saturationStr = String.format("%.2f", saturation);
                final String levelStr = String.valueOf(level);
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.get.status", name, hpStr, hungerStr, saturationStr, levelStr), false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get status", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return players.size();
    }

    private static int runShareStatusCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            double hp = player.getHealth();
            int hunger = player.getHungerManager().getFoodLevel();
            double saturation = player.getHungerManager().getSaturationLevel();
            int level = player.experienceLevel;
            final Text name = player.getDisplayName();
            final String hpStr = String.format("%.2f", hp);
            final String hungerStr = String.valueOf(hunger);
            final String saturationStr = String.format("%.2f", saturation);
            final String levelStr = String.valueOf(level);
            MutableText text = Util.parseTranslatableText("fmod.command.share.status", name, hpStr, hungerStr, saturationStr, levelStr);
            ServerMessageType.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share status", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static MutableText formatInventoryItemStack(ItemStack item) {
        MutableText itemText = Text.empty();
        try {
            if (item == null || item.isEmpty()) {
                itemText = Text.literal("00").formatted(Formatting.GRAY).styled(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.command.get.emptyslot")))
                );
            } else if (item.getCount() < 100) {
                String itemCount = String.format("%02d", item.getCount());
                itemText = Text.literal(itemCount).formatted(Formatting.AQUA).styled(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackContent(item)))
                );
            } else {
                itemText = Text.literal("9+").formatted(Formatting.AQUA).styled(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackContent(item)))
                );
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when formatting item stack", e);
            itemText = Text.literal("??").formatted(Formatting.RED);
        }
        return itemText;
    }

    private static List<MutableText> getInventoryTexts(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        // Text Structure:
        // [x] [x] [x] [x] [-] [-] [S] [+] [1]   (x: Armor, -: Placeholder, +: Offhand, 1: Current Chosen Slot Index)
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, S: Survival Gamemode [S: Survival, C: Creative, A: Adventure, V: Spectator])
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, '+' symbol formatting: [Has Item: Formatting.AQUA, Empty Slot: Formatting.GRAY])
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, '[]' bracket formmating: Formatting.GREEN)
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Hotbar, '[]' bracket formmating: [Selected: Formatting.GOLD, Other: Formatting.LIGHT_PURPLE])
        MutableText armorText = Text.empty();
        for (int i = 0; i < 4; i++) {
            ItemStack item = inventory.getArmorStack(i);
            armorText.append(Text.literal("[").formatted(Formatting.LIGHT_PURPLE));
            armorText.append(formatInventoryItemStack(item));
            armorText.append(Text.literal("]").formatted(Formatting.LIGHT_PURPLE));
            armorText.append(Text.literal(" ").formatted(Formatting.RESET));
        }
        // Placeholder
        for (int i = 0; i < 2; i++) {
            armorText.append(Text.literal("[--]").formatted(Formatting.GRAY));
            armorText.append(Text.literal(" ").formatted(Formatting.RESET));
        }
        // Gamemode
        armorText.append(Text.literal("[").formatted(Formatting.GOLD));
        GameMode gamemode = player.interactionManager.getGameMode();
        MutableText gamemodeText = Text.literal("+S");
        if (gamemode == GameMode.CREATIVE) {
            gamemodeText = Text.literal("+C");
        } else if (gamemode == GameMode.ADVENTURE) {
            gamemodeText = Text.literal("+A");
        } else if (gamemode == GameMode.SPECTATOR) {
            gamemodeText = Text.literal("+V");
        }
        gamemodeText = gamemodeText.formatted(Formatting.RED).styled(s -> s
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("gameMode." + gamemode.getName())))
        );
        armorText.append(gamemodeText);
        armorText.append(Text.literal("]").formatted(Formatting.GOLD));
        armorText.append(Text.literal(" ").formatted(Formatting.RESET));
        // Offhand
        ItemStack offhandItem = inventory.getStack(PlayerInventory.OFF_HAND_SLOT);
        armorText.append(Text.literal("[").formatted(Formatting.LIGHT_PURPLE));
        armorText.append(formatInventoryItemStack(offhandItem));
        armorText.append(Text.literal("]").formatted(Formatting.LIGHT_PURPLE));
        armorText.append(Text.literal(" ").formatted(Formatting.RESET));
        // Selected Slot
        armorText.append(Text.literal("[").formatted(Formatting.GOLD));
        armorText.append(Text.literal("0" + String.valueOf(inventory.selectedSlot + 1)).formatted(Formatting.RED));
        armorText.append(Text.literal("]").formatted(Formatting.GOLD));
        armorText.append(Text.literal(" ").formatted(Formatting.RESET));
        // Inventory
        MutableText[] inventoryText = {Text.empty(), Text.empty(), Text.empty()};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                // Index 0 ~ 8 belongs to Hotbar, Index 9 ~ 35 belongs to Inventory
                int index = (i + 1) * 9 + j;
                ItemStack item = inventory.getStack(index);
                inventoryText[i].append(Text.literal("[").formatted(Formatting.GREEN));
                inventoryText[i].append(formatInventoryItemStack(item));
                inventoryText[i].append(Text.literal("]").formatted(Formatting.GREEN));
                inventoryText[i].append(Text.literal(" ").formatted(Formatting.RESET));
            }
        }
        // Hotbar
        MutableText hotbarText = Text.empty();
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getStack(i);
            if (i == inventory.selectedSlot) {
                hotbarText.append(Text.literal("[").formatted(Formatting.GOLD));
            } else {
                hotbarText.append(Text.literal("[").formatted(Formatting.LIGHT_PURPLE));
            }
            hotbarText.append(formatInventoryItemStack(item));
            if (i == inventory.selectedSlot) {
                hotbarText.append(Text.literal("]").formatted(Formatting.GOLD));
            } else {
                hotbarText.append(Text.literal("]").formatted(Formatting.LIGHT_PURPLE));
            }
            hotbarText.append(Text.literal(" ").formatted(Formatting.RESET));
        }
        // Feedback
        final MutableText linea = armorText;
        final MutableText lineb = inventoryText[0];
        final MutableText linec = inventoryText[1];
        final MutableText lined = inventoryText[2];
        final MutableText linee = hotbarText;
        return Arrays.asList(linea, lineb, linec, lined, linee);
    }

    private static int runGetInventoryCommand(ServerPlayerEntity player, CommandContext<ServerCommandSource> context) {
        try {
            List<MutableText> inventoryText = getInventoryTexts(player);
            final Text name = player.getDisplayName();
            final Text title = Util.parseTranslatableText("fmod.command.get.inventory", name);
            context.getSource().sendFeedback(() -> title, false);
            for (MutableText text : inventoryText) {
                context.getSource().sendFeedback(() -> text, false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get inventory", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runShareInventoryCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            List<MutableText> inventoryText = getInventoryTexts(player);
            final Text name = player.getDisplayName();
            final Text title = Util.parseTranslatableText("fmod.command.share.inventory", name);
            ServerMessageType.broadcastTextMessage(context.getSource().getServer(), title);
            for (MutableText text : inventoryText) {
                ServerMessageType.broadcastTextMessage(context.getSource().getServer(), text);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share inventory", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetItemCommand(Collection<? extends Entity> entities, CommandContext<ServerCommandSource> context) {
        int result = 0;
        try {
            for (Entity entity : entities) {
                Iterable<ItemStack> items = entity.getHandItems();
                if (items == null || items.iterator().hasNext() == false) {
                    final Text name = entity.getDisplayName();
                    context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.get.noitem", name), false);
                    continue;
                }
                MutableText itemList = Text.empty();
                int itemCountSum = 0;
                for (ItemStack item : items) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    Text itemText = item.toHoverableText();
                    int itemCount = item.getCount();
                    result += itemCount;
                    itemCountSum += itemCount;
                    itemList.append(itemText);
                    if (itemCount > 1) {
                        itemList.append(Text.literal("x" + itemCount + " "));
                    } else {
                        itemList.append(Text.literal(" "));
                    }
                }
                final Text name = entity.getDisplayName();
                final MutableText itemTxt = itemList;
                if (itemCountSum <= 0) {
                    context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.get.noitem", name), false);
                } else {
                    context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.get.item", name, itemTxt), false);
                }
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get item", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runShareItemCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            Iterable<ItemStack> items = player.getHandItems();
            if (items == null || items.iterator().hasNext() == false) {
                final Text name = player.getDisplayName();
                ServerMessageType.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslatableText("fmod.command.share.noitem", name));
                return Command.SINGLE_SUCCESS;
            }
            MutableText itemList = Text.empty();
            int itemCountSum = 0;
            for (ItemStack item : items) {
                if (item.isEmpty()) {
                    continue;
                }
                Text itemText = item.toHoverableText();
                int itemCount = item.getCount();
                itemCountSum += itemCount;
                itemList.append(itemText);
                if (itemCount > 1) {
                    itemList.append(Text.literal("x" + itemCount + " "));
                } else {
                    itemList.append(Text.literal(" "));
                }
            }
            final Text name = player.getDisplayName();
            final MutableText itemTxt = itemList;
            if (itemCountSum <= 0) {
                ServerMessageType.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslatableText("fmod.command.share.noitem", name));
            } else {
                ServerMessageType.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslatableText("fmod.command.share.item", name, itemTxt));
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f share item", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetAfkTimeCommand(Collection<ServerPlayerEntity> players, CommandContext<ServerCommandSource> context) {
        try {
            for (ServerPlayerEntity player : players) {
                PlayerData data = Util.getServerData(context.getSource().getServer()).getPlayerData(player);
                double afkSeconds = data.afkTicks / 20.0;
                final String afkSecondsStr = String.format("%.1f", afkSeconds);
                final Text name = player.getDisplayName();
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.get.afk", name, afkSecondsStr), false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get afk", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return players.size();
    }

    private static int runGetTravelRecordCommand(Collection<ServerPlayerEntity> players, CommandContext<ServerCommandSource> context) {
        try {
            for (ServerPlayerEntity player : players) {
                PlayerData data = Util.getServerData(context.getSource().getServer()).getPlayerData(player);
                Vec3d[] snapshot = data.recentPositions.toArray(new Vec3d[0]);
                double seconds = (snapshot.length - 1) / 20.0;
                double totalDistance = GameMath.getHorizonalEuclideanDistance(snapshot[0], snapshot[snapshot.length - 1]);
                double totalTravelled = 0.0;
                for (int i = 1; i < snapshot.length; i++) {
                    totalTravelled += GameMath.getHorizonalEuclideanDistance(snapshot[i - 1], snapshot[i]);
                }
                final Text name = player.getDisplayName();
                final String secondsStr = String.format("%.1f", seconds);
                final String totalDistanceStr = String.format("%.1f", totalDistance);
                final String avgSpeedStr = String.format("%.1f", (totalDistance / seconds));
                final String totalTravelledStr = String.format("%.1f", totalTravelled);
                final String avgTravelSpeedStr = String.format("%.1f", (totalTravelled / seconds));
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.get.travel", name, secondsStr, totalDistanceStr, avgSpeedStr, totalTravelledStr, avgTravelSpeedStr), false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get travel", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return players.size();
    }

    private static int runGetCrowdedPlaceCommand(int number, double radius, CommandContext<ServerCommandSource> context) {
        try {
            List<Entity> allEntities = new ArrayList<>();
            for (ServerWorld world : context.getSource().getServer().getWorlds()) {
                List<Entity> entities = Util.getAllEntities(world);
                allEntities.addAll(entities);
            }
            EntityDensityCalculator calculator = new EntityDensityCalculator(context, allEntities, radius, number);
            ServerData serverData = Util.getServerData(context.getSource().getServer());
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.get.crowd"), false);
            serverData.submitAsyncTask(calculator);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f get crowd", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildGetCommand() {
        return CommandManager.literal("get")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("coord")
                .then(CommandManager.argument("entity", EntityArgumentType.entities())
                    .executes(context -> {return runGetCoordCommand(EntityArgumentType.getEntities(context, "entity"), context);})
                )
            )
            .then(CommandManager.literal("distance")
                .then(CommandManager.argument("entity", EntityArgumentType.entities())
                    .executes(context -> {return runGetDistanceCommand(EntityArgumentType.getEntities(context, "entity"), context);})
                )
            )
            .then(CommandManager.literal("health")
                .then(CommandManager.argument("entity", EntityArgumentType.entities())
                    .executes(context -> {return runGetHealthCommand(EntityArgumentType.getEntities(context, "entity"), context);})
                )
            )
            .then(CommandManager.literal("status")
                .then(CommandManager.argument("player", EntityArgumentType.players())
                    .executes(context -> {return runGetStatusCommand(EntityArgumentType.getPlayers(context, "player"), context);})
                )
            )
            .then(CommandManager.literal("inventory")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> {return runGetInventoryCommand(EntityArgumentType.getPlayer(context, "player"), context);})
                )
            )
            .then(CommandManager.literal("item")
                .then(CommandManager.argument("entity", EntityArgumentType.entities())
                    .executes(context -> {return runGetItemCommand(EntityArgumentType.getEntities(context, "entity"), context);})
                )
            )
            .then(CommandManager.literal("afk")
                .then(CommandManager.argument("player", EntityArgumentType.players())
                    .executes(context -> {return runGetAfkTimeCommand(EntityArgumentType.getPlayers(context, "player"), context);})
                )
            )
            .then(CommandManager.literal("travel")
                .then(CommandManager.argument("player", EntityArgumentType.players())
                    .executes(context -> {return runGetTravelRecordCommand(EntityArgumentType.getPlayers(context, "player"), context);})
                )
            )
            .then(CommandManager.literal("crowd")
                .then(CommandManager.argument("number", IntegerArgumentType.integer(1))
                    .then(CommandManager.argument("radius", DoubleArgumentType.doubleArg(0.0))
                        .executes(context -> {return runGetCrowdedPlaceCommand(IntegerArgumentType.getInteger(context, "number"), DoubleArgumentType.getDouble(context, "radius"), context);})
                    )
                )
                .executes(context -> {return runGetCrowdedPlaceCommand(Util.serverConfig.getEntityDensityNumber(), Util.serverConfig.getEntityDensityRadius(), context);})
            );
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildShareCommand() {
        return CommandManager.literal("share")
            .requires(source -> source.hasPermissionLevel(0))
            .then(CommandManager.literal("coord").executes(context -> {return runShareCoordCommand(context);}))
            .then(CommandManager.literal("distance").executes(context -> {return runShareDistanceCommand(context);}))
            .then(CommandManager.literal("health").executes(context -> {return runShareHealthCommand(context);}))
            .then(CommandManager.literal("status").executes(context -> {return runShareStatusCommand(context);}))
            .then(CommandManager.literal("inventory").executes(context -> {return runShareInventoryCommand(context);}))
            .then(CommandManager.literal("item").executes(context -> {return runShareItemCommand(context);}));
    }
    
}
