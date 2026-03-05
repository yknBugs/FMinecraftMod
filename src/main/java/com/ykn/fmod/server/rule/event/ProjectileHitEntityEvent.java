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

public class ProjectileHitEntityEvent implements RuleEvent {

    private static final ProjectileHitEntityEvent INSTANCE = new ProjectileHitEntityEvent();

    private final Map<String, Class<? extends Object>> variableTypes;

    private final Set<String> variablesList;

    private static final Map<String, Class<? extends Object>> createVariablesType() {
        Map<String, Class<? extends Object>> map = new HashMap<>();
        map.put("entity", UUID.class);
        map.put("shooter", UUID.class);
        map.put("projectile", UUID.class);
        map.put("x", Double.class);
        map.put("y", Double.class);
        map.put("z", Double.class);
        map.put("position", Vec3.class);
        map.put("dimension", ResourceLocation.class);
        map.put("biome", ResourceLocation.class);
        map.put("pitch", Double.class);
        map.put("yaw", Double.class);
        map.put("rotation", Vec2.class);
        map.put("sx", Double.class);
        map.put("sy", Double.class);
        map.put("sz", Double.class);
        map.put("sposition", Vec3.class);
        map.put("sdimension", ResourceLocation.class);
        map.put("sbiome", ResourceLocation.class);
        map.put("spitch", Double.class);
        map.put("syaw", Double.class);
        map.put("srotation", Vec2.class);
        map.put("distance", Double.class);
        map.put("health", Double.class);
        map.put("name", String.class);
        map.put("sname", String.class);
        return Collections.unmodifiableMap(map);
    }

    private static final Set<String> createVariablesList() {
        // Only biome is Nullable, if no shooter (e.g. from a dispenser), shooter == projectile
        Set<String> set = new HashSet<>();
        set.add("entity");
        set.add("shooter");
        set.add("projectile");
        set.add("x");
        set.add("y");
        set.add("z");
        set.add("position");
        set.add("dimension");
        set.add("pitch");
        set.add("yaw");
        set.add("rotation");
        set.add("sx");
        set.add("sy");
        set.add("sz");
        set.add("sposition");
        set.add("sdimension");
        set.add("spitch");
        set.add("syaw");
        set.add("srotation");
        set.add("distance");
        set.add("health");
        set.add("name");
        set.add("sname");
        return Collections.unmodifiableSet(set);
    }

    private ProjectileHitEntityEvent() {
        this.variableTypes = createVariablesType();
        this.variablesList = createVariablesList();
    }

    public static ProjectileHitEntityEvent getInstance() {
        return INSTANCE;
    }

    @Override
    public String getType() {
        return "ProjectileHitEntityEvent";
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
        return Util.parseTranslatableText("fmod.rule.event.projectilehitentity");
    }
}
