/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;

import com.ykn.fmod.server.base.util.GameMath;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.Components;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.Params;
import com.ykn.fmod.server.rule.tool.Ranges;
import com.ykn.fmod.server.rule.tool.RuleGameAccess;
import com.ykn.fmod.server.rule.tool.RuleNbt;
import com.ykn.fmod.server.rule.tool.RuleState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Entity, block, and permission predicates that answer a yes/no question directly.
 *
 * <p>Every type here shares one operational shape: resolve the parameters, look the subject up,
 * compare, and return a boolean. Nothing is written to a variable; components that store a value
 * belong to {@link NumericConditions} or the getter family instead.
 *
 * <p>Each type keeps the null policy of the component it replaces: strict types reject a missing
 * parameter with the standard warning, while {@code CheckBlockType} and {@code CheckEntityType}
 * opt out because a missing value has a defined meaning for them.
 */
public final class CheckConditions {

    private CheckConditions() {
    }

    /**
     * Registers every entity, block, and permission predicate.
     */
    public static void register() {
        Components.predicate("EntityPosition", Params.of("fmod.rule.condition.entityposition")
                .param("entity", ParamKind.ENTITY)
                .param("dimension", ParamKind.DIMENSION)
                .param("position", ParamKind.POSITION)
                .param("radius", ParamKind.doubleAtLeast(0)),
                (call) -> {
                    ServerLevel world = call.context().getServer()
                            .getLevel(ResourceKey.create(Registries.DIMENSION, call.resourceId(1)));
                    Entity entity = world == null ? null : world.getEntity(call.uuid(0));
                    if (entity == null) {
                        call.warnNull(0);
                        return false;
                    }
                    return call.vector(2).distanceTo(entity.position()) <= call.number(3);
                });

        Components.predicate("CheckPermission", Params.of("fmod.rule.condition.checkpermission")
                .param("player", ParamKind.PLAYER)
                .param("min", ParamKind.INT)
                .param("max", ParamKind.INT),
                (call) -> {
                    int min = call.integer(1);
                    int max = call.integer(2);
                    if (min > max) {
                        call.warn(Util.parseTranslatableText("fmod.rule.condition.checkpermission.minmax", min, max));
                    }
                    ServerPlayer player = call.context().getServer().getPlayerList().getPlayer(call.uuid(0));
                    if (player == null) {
                        return false;
                    }
                    int level;
                    try {
                        level = call.context().getServer().getProfilePermissions(player.getGameProfile());
                    } catch (RuntimeException exception) {
                        // Security-relevant: an unreadable level must never be treated as "allowed".
                        Util.LOGGER.error("FMinecraftMod: Could not read the permission level of "
                                + player.getGameProfile().getName(), exception);
                        call.warn(Util.parseTranslatableText("fmod.rule.condition.checkpermission.lookupfailed"));
                        level = 0;
                    }
                    return level >= min && level <= max;
                });

        Components.predicate("CheckBlockType", Params.of("fmod.rule.condition.blocktype")
                .param("dimension", ParamKind.DIMENSION)
                .param("position", ParamKind.POSITION)
                .param("block", ParamKind.BLOCK_ID)
                .param("default", ParamKind.BOOLEAN)
                .allowNulls(),
                (call) -> {
                    Boolean fallback = call.bool(3);
                    boolean fallbackValue = fallback != null && fallback;
                    if (call.isNull(0)) {
                        call.warnNull(0);
                        return fallbackValue;
                    }
                    if (call.isNull(1)) {
                        call.warnNull(1);
                        return fallbackValue;
                    }
                    if (call.isNull(2)) {
                        call.warnNull(2);
                        return fallbackValue;
                    }
                    ServerLevel world = call.context().getServer()
                            .getLevel(ResourceKey.create(Registries.DIMENSION, call.resourceId(0)));
                    if (world == null) {
                        return fallbackValue;
                    }
                    BlockPos position = BlockPos.containing(call.vector(1));
                    if (!world.isInWorldBounds(position)
                            || !world.hasChunk(SectionPos.blockToSectionCoord(position.getX()),
                                    SectionPos.blockToSectionCoord(position.getZ()))) {
                        call.warn(Util.parseTranslatableText("fmod.rule.condition.blocktype.unloaded",
                                position.toShortString(), call.resourceId(0).toString()));
                        return fallbackValue;
                    }
                    ResourceLocation actual = BuiltInRegistries.BLOCK
                            .getKey(world.getBlockState(position).getBlock());
                    return call.resourceId(2).equals(actual);
                });

        Components.predicate("CheckEntityType", Params.of("fmod.rule.condition.checkentitytype")
                .param("entity", ParamKind.ENTITY)
                .param("type", ParamKind.ENTITY_TYPE_ID)
                .allowNulls(),
                (call) -> {
                    if (call.isNull(0) && call.isNull(1)) {
                        call.warnNull(0);
                        call.warnNull(1);
                        return true;
                    }
                    if (call.isNull(0)) {
                        call.warnNull(0);
                        return false;
                    }
                    if (call.isNull(1)) {
                        call.warnNull(1);
                        return false;
                    }
                    for (ServerLevel world : call.context().getServer().getAllLevels()) {
                        Entity found = world.getEntity(call.uuid(0));
                        if (found != null) {
                            return call.resourceId(1).equals(EntityType.getKey(found.getType()));
                        }
                    }
                    return false;
                });

        Components.predicate("HasEntityType", Params.of("fmod.rule.condition.hasentitytype")
                .param("dimension", ParamKind.DIMENSION)
                .param("position", ParamKind.POSITION)
                .param("radius", ParamKind.doubleAtLeast(0))
                .param("type", ParamKind.ENTITY_TYPE_ID),
                (call) -> {
                    ServerLevel world = call.context().getServer()
                            .getLevel(ResourceKey.create(Registries.DIMENSION, call.resourceId(0)));
                    if (world == null) {
                        call.warnNull(0);
                        return false;
                    }
                    Vec3 center = call.vector(1);
                    double radius = call.number(2);
                    AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
                            center.x + radius, center.y + radius, center.z + radius);
                    ResourceLocation type = call.resourceId(3);
                    return !world.getEntitiesOfClass(Entity.class, box,
                            (entity) -> EntityType.getKey(entity.getType()).equals(type)).isEmpty();
                });

        Components.predicate("CheckPlayerOnline", Params.of("fmod.rule.condition.checkplayeronline")
                .param("player", ParamKind.PLAYER),
                (call) -> call.context().getServer().getPlayerList().getPlayer(call.uuid(0)) != null);

        Components.predicate("CheckIsPlayer", Params.of("fmod.rule.condition.checkisplayer")
                .param("identity", ParamKind.STRING)
                .param("expectedName", ParamKind.STRING),
                (call) -> {
                    ServerPlayer player = RuleGameAccess.player(call.context(), call.text(0));
                    return player != null
                            && player.getGameProfile().getName().equalsIgnoreCase(call.text(1));
                });

        Components.predicate("CheckMainHandItem", Params.of("fmod.rule.condition.checkmainhanditem")
                .param("entity", ParamKind.ENTITY)
                .param("item", ParamKind.RESOURCE_ID),
                (call) -> {
                    ItemStack stack = RuleGameAccess.mainHand(call.context(), call.uuid(0));
                    return !stack.isEmpty()
                            && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(call.resourceId(1));
                });

        Components.predicate("CheckBiome", Params.of("fmod.rule.condition.checkbiome")
                .param("dimension", ParamKind.DIMENSION)
                .param("position", ParamKind.POSITION)
                .param("biome", ParamKind.RESOURCE_ID),
                (call) -> {
                    ServerLevel world = RuleGameAccess.level(call.context(), call.resourceId(0));
                    Vec3 position = call.vector(1);
                    if (!RuleGameAccess.loaded(world, position)) {
                        return false;
                    }
                    return world.getBiome(BlockPos.containing(position)).unwrapKey()
                            .map((key) -> key.location().equals(call.resourceId(2)))
                            .orElse(false);
                });

        Components.predicate("HasTag", Params.of("fmod.rule.condition.hastag")
                .param("entity", ParamKind.ENTITY)
                .param("tag", ParamKind.STRING),
                (call) -> {
                    Entity entity = RuleGameAccess.entity(call.context(), call.uuid(0));
                    return entity != null && entity.getTags().contains(call.text(1));
                });

        Components.predicate("BooleanVariableAsCondition",
                Params.of("fmod.rule.condition.booleanvariableascondition")
                        .param("variableName", ParamKind.VARIABLE_NAME)
                        .param("defaultValue", ParamKind.BOOLEAN)
                        .allowNulls(),
                (call) -> {
                    Object value = call.isNull(0) ? null : call.context().getVariable(call.text(0));
                    Boolean actual = TypeAdaptor.parse(value).asBoolean();
                    if (actual != null) {
                        return actual;
                    }
                    Boolean fallback = call.bool(1);
                    if (fallback != null) {
                        return fallback;
                    }
                    if (call.isNull(0)) {
                        call.warnNull(0);
                    } else {
                        call.warn(Util.parseTranslatableText("fmod.rule.warning.variablemissing",
                                call.text(0)));
                    }
                    return false;
                });

        entityPredicate("IsSneaking", Entity::isShiftKeyDown);
        entityPredicate("IsSprinting", Entity::isSprinting);
        entityPredicate("IsOnGround", Entity::onGround);
        entityPredicate("IsFlying",
                (entity) -> entity instanceof ServerPlayer player && player.getAbilities().flying);

        worldPredicate("IsRaining", ServerLevel::isRaining);
        worldPredicate("IsThundering", ServerLevel::isThundering);
        worldPredicate("IsDay", ServerLevel::isDay);

        Components.predicate("CheckNbtValue", Params.of("fmod.rule.condition.checknbtvalue")
                .param("nbt", ParamKind.NBT)
                .param("path", ParamKind.NBT_PATH)
                .param("expected", ParamKind.NBT),
                (call) -> {
                    try {
                        return RuleNbt.check(call.tag(0), call.text(1), call.tag(2));
                    } catch (IllegalArgumentException exception) {
                        call.warn(Util.parseTranslatableText("fmod.rule.warning.nbtcheck", call.text(1)));
                        return false;
                    }
                });

        Components.predicate("CheckEntityNbtValue", Params.of("fmod.rule.condition.checkentitynbtvalue")
                .param("entity", ParamKind.ENTITY)
                .param("path", ParamKind.NBT_PATH)
                .param("expected", ParamKind.NBT),
                (call) -> {
                    try {
                        return RuleNbt.check(RuleGameAccess.entityNbt(call.context(), call.uuid(0)),
                                call.text(1), call.tag(2));
                    } catch (IllegalArgumentException exception) {
                        call.warn(Util.parseTranslatableText("fmod.rule.warning.nbtcheck", call.text(1)));
                        return false;
                    }
                });

        Components.predicate("CheckMainHandItemNbtValue",
                Params.of("fmod.rule.condition.checkmainhanditemnbtvalue")
                        .param("entity", ParamKind.ENTITY)
                        .param("path", ParamKind.NBT_PATH)
                        .param("expected", ParamKind.NBT),
                (call) -> {
                    try {
                        return RuleNbt.check(RuleGameAccess.mainHandNbt(call.context(), call.uuid(0)),
                                call.text(1), call.tag(2));
                    } catch (IllegalArgumentException exception) {
                        call.warn(Util.parseTranslatableText("fmod.rule.warning.nbtcheck", call.text(1)));
                        return false;
                    }
                });

        Components.predicate("HasItem", Params.of("fmod.rule.condition.hasitem")
                .param("player", ParamKind.PLAYER)
                .param("item", ParamKind.RESOURCE_ID)
                .param("min", ParamKind.INT)
                .param("max", ParamKind.INT)
                .allowNulls(),
                (call) -> {
                    ServerPlayer player = call.context().getServer().getPlayerList()
                            .getPlayer(call.uuid(0));
                    if (player == null) {
                        return false;
                    }
                    int lower = Ranges.lowerInt(call, 2);
                    int upper = Ranges.upperInt(call, 3);
                    if (!Ranges.requireOrder(call, lower, upper,
                            "fmod.rule.condition.hasitem.invalidrange")) {
                        return false;
                    }
                    Integer count;
                    try {
                        count = RuleState.itemCount(player.getInventory(), call.resourceId(1));
                    } catch (ArithmeticException exception) {
                        call.warn(Util.parseTranslatableText("fmod.rule.warning.operationfailed"));
                        return false;
                    }
                    return count != null && count >= lower && count <= upper;
                });

        distanceBetween("EuclideanDistance", GameMath::getEuclideanDistance);
        distanceBetween("ManhattanDistance", GameMath::getManhattanDistance);
        distanceBetween("ChebyshevDistance", GameMath::getChebyshevDistance);
        distanceBetween("HorizontalEuclideanDistance", GameMath::getHorizontalEuclideanDistance);
        distanceBetween("HorizontalManhattanDistance", GameMath::getHorizontalManhattanDistance);
        distanceBetween("HorizontalChebyshevDistance", GameMath::getHorizontalChebyshevDistance);
        distanceBetween("VerticalDistance", GameMath::getVerticalDistance);
    }

    /**
     * Registers a distance range check.
     *
     * <p>The two positions are required; the bounds follow the shared range rule, so either one may
     * be omitted and is then treated as unbounded.
     *
     * @param type      the distance name, e.g. {@code EuclideanDistance}; the type id is
     *                  {@code <type>Between} and the i18n prefix is derived from it
     * @param operation the distance function
     */
    private static void distanceBetween(String type, ToDoubleBiFunction<Vec3, Vec3> operation) {
        String id = type + "Between";
        String prefix = prefix(id);
        Components.predicate(id, Params.of(prefix)
                .param("a", ParamKind.POSITION)
                .param("b", ParamKind.POSITION)
                .param("min", ParamKind.DOUBLE)
                .param("max", ParamKind.DOUBLE)
                .allowNulls(),
                (call) -> {
                    if (call.isNull(0)) {
                        call.warnNull(0);
                        return false;
                    }
                    if (call.isNull(1)) {
                        call.warnNull(1);
                        return false;
                    }
                    double lower = Ranges.lowerDouble(call, 2);
                    double upper = Ranges.upperDouble(call, 3);
                    if (!Ranges.requireOrder(call, lower, upper, prefix + ".invalidrange")) {
                        return false;
                    }
                    double distance = operation.applyAsDouble(call.vector(0), call.vector(1));
                    return Double.isFinite(distance) && distance >= lower && distance <= upper;
                });
    }

    /**
     * Registers an entity predicate whose only parameter is the {@code entity} to inspect.
     *
     * @param type      the type id; the i18n prefix is derived from it
     * @param predicate the test applied to the resolved entity
     */
    private static void entityPredicate(String type, Predicate<Entity> predicate) {
        Components.predicate(type, Params.of(prefix(type))
                .param("entity", ParamKind.ENTITY),
                (call) -> {
                    Entity entity = RuleGameAccess.entity(call.context(), call.uuid(0));
                    return entity != null && predicate.test(entity);
                });
    }

    /**
     * Registers a world predicate whose only parameter is the {@code dimension} to inspect.
     *
     * @param type      the type id; the i18n prefix is derived from it
     * @param predicate the test applied to the resolved level
     */
    private static void worldPredicate(String type, Predicate<ServerLevel> predicate) {
        Components.predicate(type, Params.of(prefix(type))
                .param("dimension", ParamKind.DIMENSION),
                (call) -> {
                    ServerLevel world = RuleGameAccess.level(call.context(), call.resourceId(0));
                    return world != null && predicate.test(world);
                });
    }

    /**
     * Builds the i18n prefix of a condition type from its id.
     *
     * @param type the type id, e.g. {@code IsDay}
     * @return the prefix, e.g. {@code fmod.rule.condition.isday}
     */
    private static String prefix(String type) {
        return "fmod.rule.condition." + type.toLowerCase(Locale.ROOT);
    }
}
