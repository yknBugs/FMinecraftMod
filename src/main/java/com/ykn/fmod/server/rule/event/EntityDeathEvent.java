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
import net.minecraft.world.phys.Vec3;

public class EntityDeathEvent implements RuleEvent {

    private static final EntityDeathEvent INSTANCE = new EntityDeathEvent();

    private final Map<String, Class<? extends Object>> variableTypes;

    private final Set<String> variablesList;

    private static final Map<String, Class<? extends Object>> createVariablesType() {
        Map<String, Class<? extends Object>> map = new HashMap<>();
        map.put("entity", UUID.class);
        map.put("cause", UUID.class);
        map.put("source", UUID.class);
        map.put("x", Double.class);
        map.put("y", Double.class);
        map.put("z", Double.class);
        map.put("position", Vec3.class);
        map.put("dimension", ResourceLocation.class);
        map.put("biome", ResourceLocation.class);
        map.put("message", String.class);
        map.put("name", String.class);
        return Collections.unmodifiableMap(map);
    }
    
    private static final Set<String> createVariablesList() {
        // Cause, source and biome is Nullable, excluded from here
        Set<String> set = new HashSet<>();
        set.add("entity");
        set.add("x");
        set.add("y");
        set.add("z");
        set.add("position");
        set.add("dimension");
        set.add("message");
        set.add("name");
        return Collections.unmodifiableSet(set);
    }

    private EntityDeathEvent() {
        this.variableTypes = createVariablesType();
        this.variablesList = createVariablesList();
    }

    public static EntityDeathEvent getInstance() {
        return INSTANCE;
    }

    @Override
    public String getType() {
        return "EntityDeathEvent";
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
        return Util.parseTranslatableText("fmod.rule.event.entitydeath");
    }
}
