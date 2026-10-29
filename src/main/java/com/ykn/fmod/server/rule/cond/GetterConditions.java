/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.Locale;
import java.util.function.Function;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.rule.core.ComponentCall;
import com.ykn.fmod.server.rule.core.Components;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.Params;
import com.ykn.fmod.server.rule.tool.RuleGameAccess;
import com.ykn.fmod.server.rule.tool.RuleManager;
import com.ykn.fmod.server.rule.tool.RuleState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Data-flow conditions that read one value from the game state and store it in a variable.
 *
 * <p>Every type here shares one operational shape: resolve the subject, read one property, hand it
 * to the writer wrapper. Where the value is absent - an entity that is not loaded, a block
 * position outside loaded terrain, a score that does not exist - the body returns {@code null},
 * which the wrapper reports as {@code fmod.rule.warning.operationfailed} and turns into a failed
 * component.
 *
 * <p>Three properties are read through one helper each: {@link #entityGetter}, the main-hand pair,
 * and {@link #blockGetter}.
 *
 * <p>{@code FlowEnabledStatus} is the one type here that reads flow state; it is the documented
 * exception to the "no flow imports in rule code" rule, because operating on a flow is its entire
 * purpose.
 */
public final class GetterConditions {

    private GetterConditions() {
    }

    /**
     * Registers every getter condition type.
     */
    public static void register() {
        entityGetter("GetEntityHealth",
                (entity) -> entity instanceof LivingEntity living ? (double) living.getHealth() : null);
        entityGetter("GetEntityName", (entity) -> entity.getName().getString());
        entityGetter("GetEntityPosition", Entity::position);
        entityGetter("GetEntityVelocity", Entity::getDeltaMovement);
        entityGetter("GetEntityRotation", Entity::getRotationVector);
        entityGetter("GetEntityVehicle",
                (entity) -> entity.getVehicle() == null ? null : entity.getVehicle().getUUID());
        entityGetter("GetEntityDimension", (entity) -> entity.level().dimension().location());
        entityGetter("GetEntityType",
                (entity) -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));

        Components.writer("GetMainHandItem", Params.of("fmod.rule.condition.getmainhanditem")
                .param("entity", ParamKind.ENTITY)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    ItemStack stack = RuleGameAccess.mainHand(call.context(), call.uuid(0));
                    return stack.isEmpty()
                            ? null
                            : BuiltInRegistries.ITEM.getKey(stack.getItem());
                });

        Components.writer("GetMainHandItemDisplayName",
                Params.of("fmod.rule.condition.getmainhanditemdisplayname")
                        .param("entity", ParamKind.ENTITY)
                        .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    ItemStack stack = RuleGameAccess.mainHand(call.context(), call.uuid(0));
                    return stack.isEmpty() ? null : stack.getHoverName().getString();
                });

        Components.writer("GetSelectedSlot", Params.of("fmod.rule.condition.getselectedslot")
                .param("player", ParamKind.PLAYER)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    ServerPlayer player = player(call);
                    return player == null ? null : player.getInventory().selected;
                });

        Components.writer("GetItemNumber", Params.of("fmod.rule.condition.getitemnumber")
                .param("player", ParamKind.PLAYER)
                .param("item", ParamKind.RESOURCE_ID)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    ServerPlayer player = player(call);
                    return player == null
                            ? null
                            : RuleState.itemCount(player.getInventory(), call.resourceId(1));
                });

        Components.writer("GetSlotItemCount", Params.of("fmod.rule.condition.getslotitemcount")
                .param("player", ParamKind.PLAYER)
                .param("slot", ParamKind.INT)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    ServerPlayer player = player(call);
                    if (player == null) {
                        return null;
                    }
                    ItemStack stack = RuleState.slot(player.getInventory(), call.integer(1));
                    // A valid but empty slot holds zero items; only an out-of-range slot fails.
                    return stack == null ? null : stack.getCount();
                });

        Components.writer("GetWorldTime", Params.of("fmod.rule.condition.getworldtime")
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    long time = call.context().getServer().overworld().getDayTime();
                    try {
                        return Math.toIntExact(time);
                    } catch (ArithmeticException exception) {
                        call.fail(Util.parseTranslatableText("fmod.rule.warning.operationfailed"));
                        return null;
                    }
                });

        Components.writer("GetTimeOfDay", Params.of("fmod.rule.condition.gettimeofday")
                .param("dimension", ParamKind.DIMENSION)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    ServerLevel world = RuleGameAccess.level(call.context(), call.resourceId(0));
                    return world == null
                            ? null
                            : (int) Math.floorMod(world.getDayTime(), 24000L);
                });

        blockGetter("GetLightLevel",
                (world, position) -> world.getMaxLocalRawBrightness(position));

        blockGetter("GetBlockType",
                (world, position) -> BuiltInRegistries.BLOCK.getKey(world.getBlockState(position).getBlock()));

        blockGetter("GetBiome",
                (world, position) -> world.getBiome(position).unwrapKey()
                        .map((key) -> key.location())
                        .orElse(null));

        Components.writer("GetBlockProperty", Params.of("fmod.rule.condition.getblockproperty")
                .param("dimension", ParamKind.DIMENSION)
                .param("position", ParamKind.POSITION)
                .param("property", ParamKind.STRING)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    ServerLevel world = RuleGameAccess.level(call.context(), call.resourceId(0));
                    Vec3 position = call.vector(1);
                    if (!RuleGameAccess.loaded(world, position)) {
                        return null;
                    }
                    return RuleState.property(world.getBlockState(BlockPos.containing(position)),
                            call.text(2));
                });

        Components.writer("GetScore", Params.of("fmod.rule.condition.getscore")
                .param("objective", ParamKind.STRING)
                .param("holder", ParamKind.STRING)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> RuleState.score(call.context().getServer().getScoreboard(),
                        call.text(0), call.text(1)));

        Components.writer("GetNumOnlinePlayers", Params.of("fmod.rule.condition.getnumonlineplayers")
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> call.context().getServer().getPlayerList().getPlayerCount());

        Components.writer("GetPlayerByName", Params.of("fmod.rule.condition.getplayerbyname")
                .param("playerName", ParamKind.STRING)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    ServerPlayer player = call.context().getServer().getPlayerList()
                            .getPlayerByName(call.text(0));
                    return player == null ? null : player.getUUID();
                });

        Components.writer("FlowEnabledStatus", Params.of("fmod.rule.condition.flowenabledstatus")
                .param("flow", ParamKind.STRING)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    FlowManager manager = Util.getServerData(call.context().getServer())
                            .getLogicFlows().get(call.text(0));
                    return manager == null ? null : manager.isEnabled();
                });

        Components.writer("RuleEnabledStatus", Params.of("fmod.rule.condition.ruleenabledstatus")
                .param("rule", ParamKind.STRING)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    RuleManager manager = Util.getServerData(call.context().getServer())
                            .getCustomRules().get(call.text(0));
                    return manager == null ? null : manager.isEnabled();
                });
    }

    /**
     * Registers a getter that reads one property of an entity.
     *
     * @param type   the type id; the i18n prefix is derived from it
     * @param getter the property reader
     */
    private static void entityGetter(String type, Function<Entity, Object> getter) {
        Components.writer(type, Params.of(prefix(type))
                .param("entity", ParamKind.ENTITY)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    Entity entity = RuleGameAccess.entity(call.context(), call.uuid(0));
                    return entity == null ? null : getter.apply(entity);
                });
    }

    /**
     * Registers a getter that reads one property of a loaded block position.
     *
     * @param type   the type id; the i18n prefix is derived from it
     * @param getter the property reader
     */
    private static void blockGetter(String type, BlockReader getter) {
        Components.writer(type, Params.of(prefix(type))
                .param("dimension", ParamKind.DIMENSION)
                .param("position", ParamKind.POSITION)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    ServerLevel world = RuleGameAccess.level(call.context(), call.resourceId(0));
                    Vec3 position = call.vector(1);
                    if (!RuleGameAccess.loaded(world, position)) {
                        return null;
                    }
                    return getter.read(world, BlockPos.containing(position));
                });
    }

    /**
     * Builds the i18n prefix of a condition type from its id.
     *
     * @param type the type id, e.g. {@code GetEntityHealth}
     * @return the prefix, e.g. {@code fmod.rule.condition.getentityhealth}
     */
    private static String prefix(String type) {
        return "fmod.rule.condition." + type.toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves the {@code player} parameter to an online player.
     *
     * @param call the current evaluation
     * @return the player, or {@code null} when they are offline
     */
    private static ServerPlayer player(ComponentCall call) {
        return call.context().getServer().getPlayerList().getPlayer(call.uuid(0));
    }

    /**
     * Reads one property of a block position.
     */
    @FunctionalInterface
    private interface BlockReader {

        /**
         * Reads the property.
         *
         * @param world    the level the position is in
         * @param position the block position
         * @return the property value, or {@code null} when it does not exist
         */
        Object read(ServerLevel world, BlockPos position);
    }
}
