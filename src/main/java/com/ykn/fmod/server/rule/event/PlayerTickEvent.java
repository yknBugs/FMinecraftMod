/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleEvent;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link RuleEvent} that fires every server tick, once per online player.
 *
 * <p>Variable contract:
 * <table border="1">
 *   <tr><th>Name</th><th>Type</th><th>Nullable</th><th>Description</th></tr>
 *   <tr><td>{@code tick}</td><td>{@code Integer}</td><td>No</td><td>The server tick counter at the time of dispatch.</td></tr>
 *   <tr><td>{@code player}</td><td>{@code UUID}</td><td>No</td><td>The player.</td></tr>
 *   <tr><td>{@code x}/{@code y}/{@code z}</td><td>{@code Double}</td><td>No</td><td>The player's coordinates.</td></tr>
 *   <tr><td>{@code position}</td><td>{@code Vec3}</td><td>No</td><td>The player's position.</td></tr>
 *   <tr><td>{@code dimension}</td><td>{@code ResourceLocation}</td><td>No</td><td>The dimension the player is in.</td></tr>
 *   <tr><td>{@code biome}</td><td>{@code ResourceLocation}</td><td>Yes</td><td>The biome the player is in.</td></tr>
 *   <tr><td>{@code pitch}/{@code yaw}</td><td>{@code Double}</td><td>No</td><td>The player's rotation angles.</td></tr>
 *   <tr><td>{@code rotation}</td><td>{@code Vec2}</td><td>No</td><td>The player's rotation vector.</td></tr>
 *   <tr><td>{@code health}</td><td>{@code Double}</td><td>No</td><td>The player's health.</td></tr>
 *   <tr><td>{@code name}</td><td>{@code String}</td><td>No</td><td>The player's display name.</td></tr>
 *   <tr><td>{@code afk}</td><td>{@code Integer}</td><td>No</td><td>How long the player has been afk, in ticks.</td></tr>
 *   <tr><td>{@code lastpitch}/{@code lastyaw}</td><td>{@code Double}</td><td>No</td><td>The player's rotation angles on the previous tick.</td></tr>
 *   <tr><td>{@code lastrotation}</td><td>{@code Vec2}</td><td>No</td><td>The player's rotation vector on the previous tick.</td></tr>
 *   <tr><td>{@code lastbiome}</td><td>{@code ResourceLocation}</td><td>Yes</td><td>The biome the player was in on the previous tick.</td></tr>
 *   <tr><td>{@code lastdimension}</td><td>{@code ResourceLocation}</td><td>No</td><td>The dimension the player was in on the previous tick.</td></tr>
 *   <tr><td>{@code cansleep}</td><td>{@code Boolean}</td><td>Yes</td><td>Whether the player can sleep right now; nullable until first computed.</td></tr>
 * </table>
 *
 * <p>This class is a singleton; obtain the instance via {@link #getInstance()}.
 */
public class PlayerTickEvent implements RuleEvent {

    private static final PlayerTickEvent INSTANCE = new PlayerTickEvent();

    private final Map<String, Class<? extends Object>> variableTypes;

    private final Set<String> variablesList;

    private static final Map<String, Class<? extends Object>> createVariablesType() {
        Map<String, Class<? extends Object>> map = new HashMap<>();
        map.put("tick", Integer.class);
        map.put("player", UUID.class);
        map.put("x", Double.class);
        map.put("y", Double.class);
        map.put("z", Double.class);
        map.put("position", Vec3.class);
        map.put("dimension", ResourceLocation.class);
        map.put("biome", ResourceLocation.class);
        map.put("pitch", Double.class);
        map.put("yaw", Double.class);
        map.put("rotation", Vec2.class);
        map.put("health", Double.class);
        map.put("name", String.class);
        map.put("afk", Integer.class);
        map.put("lastpitch", Double.class);
        map.put("lastyaw", Double.class);
        map.put("lastrotation", Vec2.class);
        map.put("lastbiome", ResourceLocation.class);
        map.put("lastdimension", ResourceLocation.class);
        map.put("cansleep", Boolean.class);
        return Collections.unmodifiableMap(map);
    }

    private static final Set<String> createVariablesList() {
        // Biome and lastBiome are nullable, can sleep is nullable if in a non-sleepable dimension
        Set<String> set = new HashSet<>();
        set.add("tick");
        set.add("player");
        set.add("x");
        set.add("y");
        set.add("z");
        set.add("position");
        set.add("dimension");
        set.add("pitch");
        set.add("yaw");
        set.add("rotation");
        set.add("health");
        set.add("name");
        set.add("afk");
        set.add("lastpitch");
        set.add("lastyaw");
        set.add("lastrotation");
        set.add("lastdimension");
        return Collections.unmodifiableSet(set);
    }

    private PlayerTickEvent() {
        this.variableTypes = createVariablesType();
        this.variablesList = createVariablesList();
    }

    public static PlayerTickEvent getInstance() {
        return INSTANCE;
    }

    @Override
    public String getType() {
        return "PlayerTickEvent";
    }

    @Override
    public Set<String> variablesList() {
        return variablesList;
    }

    @Override
    public Map<String, Class<? extends Object>> variablesType() {
        return variableTypes;
    }

    @Override
    public Component render() {
        return Util.parseTranslatableText("fmod.rule.event.playertick");
    }
}
