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
