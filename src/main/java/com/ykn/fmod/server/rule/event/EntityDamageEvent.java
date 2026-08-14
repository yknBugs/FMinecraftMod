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
 * A {@link RuleEvent} that fires when a {@code LivingEntity} takes damage.
 *
 * <p>Variable contract:
 * <table border="1">
 *   <tr><th>Name</th><th>Type</th><th>Nullable</th><th>Description</th></tr>
 *   <tr><td>{@code entity}</td><td>{@code UUID}</td><td>No</td><td>The entity that took damage.</td></tr>
 *   <tr><td>{@code damage}</td><td>{@code Double}</td><td>No</td><td>The amount of damage dealt.</td></tr>
 *   <tr><td>{@code cause}</td><td>{@code UUID}</td><td>Yes</td><td>The entity causing the damage.</td></tr>
 *   <tr><td>{@code source}</td><td>{@code UUID}</td><td>Yes</td><td>The direct damage source entity.</td></tr>
 *   <tr><td>{@code x}/{@code y}/{@code z}</td><td>{@code Double}</td><td>No</td><td>The entity's coordinates.</td></tr>
 *   <tr><td>{@code position}</td><td>{@code Vec3}</td><td>No</td><td>The entity's position.</td></tr>
 *   <tr><td>{@code dimension}</td><td>{@code ResourceLocation}</td><td>No</td><td>The dimension the entity is in.</td></tr>
 *   <tr><td>{@code biome}</td><td>{@code ResourceLocation}</td><td>Yes</td><td>The biome the entity is in.</td></tr>
 *   <tr><td>{@code pitch}/{@code yaw}</td><td>{@code Double}</td><td>No</td><td>The entity's rotation angles.</td></tr>
 *   <tr><td>{@code rotation}</td><td>{@code Vec2}</td><td>No</td><td>The entity's rotation vector.</td></tr>
 *   <tr><td>{@code message}</td><td>{@code String}</td><td>No</td><td>The damage message id.</td></tr>
 *   <tr><td>{@code exhaustion}</td><td>{@code Double}</td><td>No</td><td>The exhaustion caused by the damage.</td></tr>
 *   <tr><td>{@code health}</td><td>{@code Double}</td><td>No</td><td>The entity's health before damage.</td></tr>
 *   <tr><td>{@code name}</td><td>{@code String}</td><td>No</td><td>The entity's display name.</td></tr>
 * </table>
 *
 * <p>This class is a singleton; obtain the instance via {@link #getInstance()}.
 */
public class EntityDamageEvent implements RuleEvent {

    private static final EntityDamageEvent INSTANCE = new EntityDamageEvent();

    private final Map<String, Class<? extends Object>> variableTypes;

    private final Set<String> variablesList;

    public static final String TYPE = "EntityDamageEvent";

    private static final Map<String, Class<? extends Object>> createVariablesType() {
        Map<String, Class<? extends Object>> map = new HashMap<>();
        map.put("entity", UUID.class);
        map.put("damage", Double.class);
        map.put("cause", UUID.class);
        map.put("source", UUID.class);
        map.put("x", Double.class);
        map.put("y", Double.class);
        map.put("z", Double.class);
        map.put("position", Vec3.class);
        map.put("dimension", ResourceLocation.class);
        map.put("biome", ResourceLocation.class);
        map.put("pitch", Double.class);
        map.put("yaw", Double.class);
        map.put("rotation", Vec2.class);
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
        return TYPE;
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
        return Util.parseTranslatableText("fmod.rule.event.entitydamage");
    }
}
