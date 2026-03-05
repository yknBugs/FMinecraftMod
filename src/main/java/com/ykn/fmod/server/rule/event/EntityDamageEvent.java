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

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class EntityDamageEvent implements RuleEvent {

    private static final EntityDamageEvent INSTANCE = new EntityDamageEvent();

    private final Map<String, Class<? extends Object>> variableTypes;

    private final Set<String> variablesList;

    private static final Map<String, Class<? extends Object>> createVariablesType() {
        Map<String, Class<? extends Object>> map = new HashMap<>();
        map.put("entity", UUID.class);
        map.put("damage", Double.class);
        map.put("cause", UUID.class);
        map.put("source", UUID.class);
        map.put("x", Double.class);
        map.put("y", Double.class);
        map.put("z", Double.class);
        map.put("position", Vec3d.class);
        map.put("dimension", Identifier.class);
        map.put("biome", Identifier.class);
        map.put("pitch", Double.class);
        map.put("yaw", Double.class);
        map.put("rotation", Vec2f.class);
        map.put("message", String.class);
        map.put("exhaustion", Double.class);
        map.put("health", Double.class);
        map.put("name", String.class);
        return Collections.unmodifiableMap(map);
    }
    
    private static final Set<String> createVariablesList() {
        // Cause, source and biome is Nullable, excluded from here
        Set<String> set = new HashSet<>();
        set.add("entity");
        set.add("damage");
        set.add("x");
        set.add("y");
        set.add("z");
        set.add("position");
        set.add("dimension");
        set.add("pitch");
        set.add("yaw");
        set.add("rotation");
        set.add("message");
        set.add("exhaustion");
        set.add("health");
        set.add("name");
        return Collections.unmodifiableSet(set);
    }

    private EntityDamageEvent() {
        this.variableTypes = createVariablesType();
        this.variablesList = createVariablesList();
    }

    public static EntityDamageEvent getInstance() {
        return INSTANCE;
    }

    @Override
    public String getType() {
        return "EntityDamageEvent";
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
    public Text render() {
        return Util.parseTranslatableText("fmod.rule.event.entitydamage");
    }
}
