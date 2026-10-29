/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.Objects;
import java.util.UUID;

import com.ykn.fmod.server.rule.core.Components;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.Params;
import com.ykn.fmod.server.rule.tool.Ranges;
import com.ykn.fmod.server.rule.tool.RuleState;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Comparison and range predicates.
 *
 * <ul>
 *   <li>{@code EqualsTo} treats {@code null} as a value: two unresolved parameters are equal,
 *       exactly one is not. It therefore opts out of the strict null check.</li>
 *   <li>{@code SmallerThan} requires both operands; a missing one is reported and the
 *       condition is {@code false}.</li>
 *   <li>{@code NumberBetween}, {@code CheckTimeBetween}, {@code CheckNumOnlinePlayersBetween}
 *       and {@code CheckEntityHealth} take a range whose bounds are optional: a missing bound is
 *       reported and treated as unbounded, so a one-sided range keeps working.</li>
 * </ul>
 *
 * @see Components
 * @see Ranges
 */
public final class ComparisonConditions {

    private ComparisonConditions() {
    }

    /**
     * Registers every comparison condition type.
     */
    public static void register() {
        Components.predicate("EqualsTo", Params.of("fmod.rule.condition.equalsto")
                .param("left", ParamKind.AUTO)
                .param("right", ParamKind.AUTO)
                .allowNulls(),
                (call) -> Objects.equals(call.raw(0), call.raw(1)));

        Components.predicate("SmallerThan", Params.of("fmod.rule.condition.smallerthan")
                .param("left", ParamKind.DOUBLE)
                .param("right", ParamKind.DOUBLE),
                (call) -> call.number(0) < call.number(1));

        Components.predicate("NumberBetween", Params.of("fmod.rule.condition.numberbetween")
                .param("number", ParamKind.DOUBLE)
                .param("min", ParamKind.DOUBLE)
                .param("max", ParamKind.DOUBLE)
                .allowNulls(),
                (call) -> {
                    Double number = call.number(0);
                    if (number == null) {
                        call.warnNull(0);
                        return false;
                    }
                    double lower = Ranges.lowerDouble(call, 1);
                    double upper = Ranges.upperDouble(call, 2);
                    if (!Ranges.requireOrder(call, lower, upper,
                            "fmod.rule.condition.numberbetween.invalidrange")) {
                        return false;
                    }
                    return number >= lower && number <= upper;
                });

        Components.predicate("CheckTimeBetween", Params.of("fmod.rule.condition.checktimebetween")
                .param("min", ParamKind.INT)
                .param("max", ParamKind.INT)
                .allowNulls(),
                (call) -> {
                    int lower = Ranges.lowerInt(call, 0);
                    int upper = Ranges.upperInt(call, 1);
                    if (!Ranges.requireOrder(call, lower, upper,
                            "fmod.rule.condition.checktimebetween.invalidrange")) {
                        return false;
                    }
                    long time = call.context().getServer().overworld().getDayTime();
                    return time >= lower && time <= upper;
                });

        Components.predicate("CheckNumOnlinePlayersBetween",
                Params.of("fmod.rule.condition.checknumonlineplayersbetween")
                        .param("min", ParamKind.INT)
                        .param("max", ParamKind.INT)
                        .allowNulls(),
                (call) -> {
                    int lower = Ranges.lowerInt(call, 0);
                    int upper = Ranges.upperInt(call, 1);
                    if (!Ranges.requireOrder(call, lower, upper,
                            "fmod.rule.condition.checknumonlineplayersbetween.invalidrange")) {
                        return false;
                    }
                    int count = call.context().getServer().getPlayerList().getPlayerCount();
                    return count >= lower && count <= upper;
                });

        Components.predicate("ScoreBetween", Params.of("fmod.rule.condition.scorebetween")
                .param("objective", ParamKind.STRING)
                .param("holder", ParamKind.STRING)
                .param("min", ParamKind.INT)
                .param("max", ParamKind.INT)
                .allowNulls(),
                (call) -> {
                    int lower = Ranges.lowerInt(call, 2);
                    int upper = Ranges.upperInt(call, 3);
                    if (!Ranges.requireOrder(call, lower, upper,
                            "fmod.rule.condition.scorebetween.invalidrange")) {
                        return false;
                    }
                    Integer score = RuleState.score(call.context().getServer().getScoreboard(),
                            call.text(0), call.text(1));
                    return score != null && score >= lower && score <= upper;
                });

        Components.predicate("CheckEntityHealth", Params.of("fmod.rule.condition.checkentityhealth")
                .param("entity", ParamKind.ENTITY)
                .param("min", ParamKind.DOUBLE)
                .param("max", ParamKind.DOUBLE)
                .allowNulls(),
                (call) -> {
                    UUID id = call.uuid(0);
                    if (id == null) {
                        call.warnNull(0);
                    }
                    double lower = Ranges.lowerDouble(call, 1);
                    double upper = Ranges.upperDouble(call, 2);
                    if (id == null) {
                        return false;
                    }
                    if (!Ranges.requireOrder(call, lower, upper,
                            "fmod.rule.condition.checkentityhealth.invalidrange")) {
                        return false;
                    }
                    for (ServerLevel level : call.context().getServer().getAllLevels()) {
                        Entity found = level.getEntity(id);
                        if (found instanceof LivingEntity living) {
                            return living.getHealth() >= lower && living.getHealth() <= upper;
                        }
                    }
                    call.warnNull(0);
                    return false;
                });
    }
}
