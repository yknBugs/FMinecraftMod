/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.List;
import java.util.UUID;

import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;

import net.minecraft.server.level.ServerPlayer;

/**
 * A {@link SourceCondition} that tests whether a player's permission level falls within
 * the range [{@code min}, {@code max}] (both inclusive).
 *
 * <p>All three parameters ({@code player}, {@code min}, {@code max}) are represented as
 * {@link RuleParameter}s, meaning each can be resolved from a context variable or fall back
 * to a constant.
 *
 * <p>The condition evaluates to {@code false} when any parameter resolves to {@code null},
 * or when no online player with the given UUID is found.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "player": {"variable": "playerId"},
 *   "min":    {"constant": 0},
 *   "max":    {"constant": 4}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class CheckPermission implements SourceCondition {

    private final String name;

    private final RuleParameter<UUID> player;

    private final RuleParameter<Integer> min;

    private final RuleParameter<Integer> max;

    public static final String TYPE = "CheckPermission";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.checkpermission.summary")
        .add(ParamKind.PLAYER, "fmod.rule.condition.checkpermission.param.player.name", "fmod.rule.condition.checkpermission.param.player.desc", "player", "var.player")
        .add(ParamKind.INT, "fmod.rule.condition.checkpermission.param.min.name", "fmod.rule.condition.checkpermission.param.min.desc", "min", "var.min")
        .add(ParamKind.INT, "fmod.rule.condition.checkpermission.param.max.name", "fmod.rule.condition.checkpermission.param.max.desc", "max", "var.max");

    /**
     * Creates a new {@code CheckPermission} condition.
     *
     * @param name   the unique name of this condition instance within the rule
     * @param values the parameter values, in {@link #getParameters()} order: {@code player},
     *               {@code min}, {@code max}
     */
    @SuppressWarnings("unchecked")
    public CheckPermission(String name, List<RuleParameter<?>> values) {
        this.name = name;
        this.player = (RuleParameter<UUID>) values.get(0);
        this.min = (RuleParameter<Integer>) values.get(1);
        this.max = (RuleParameter<Integer>) values.get(2);
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        UUID playerId = this.player.resolve(context, UUID.class);
        Integer min = this.min.resolve(context, Integer.class);
        Integer max = this.max.resolve(context, Integer.class);

        if (playerId == null || min == null || max == null) {
            return false;
        }

        ServerPlayer player = context.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) {
            return false;
        }

        int level = context.getServer().getProfilePermissions(player.getGameProfile());
        return level >= min && level <= max;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public RequiredParamMetadata getParameters() {
        return PARAM_METADATA;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new CheckPermission(name, getParameterValues());
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.player, this.min, this.max);
    }

}
