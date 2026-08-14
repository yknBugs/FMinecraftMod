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
 * A {@link RuleEvent} that fires when a projectile hits an entity.
 *
 * <p>Variable contract:
 * <table border="1">
 *   <tr><th>Name</th><th>Type</th><th>Nullable</th><th>Description</th></tr>
 *   <tr><td>{@code entity}</td><td>{@code UUID}</td><td>No</td><td>The entity that was hit.</td></tr>
 *   <tr><td>{@code shooter}</td><td>{@code UUID}</td><td>No</td><td>The entity that launched the projectile; equals {@code projectile} if launched from a dispenser.</td></tr>
 *   <tr><td>{@code projectile}</td><td>{@code UUID}</td><td>No</td><td>The projectile entity.</td></tr>
 *   <tr><td>{@code x}/{@code y}/{@code z}</td><td>{@code Double}</td><td>No</td><td>The hit entity's coordinates.</td></tr>
 *   <tr><td>{@code position}</td><td>{@code Vec3}</td><td>No</td><td>The hit entity's position.</td></tr>
 *   <tr><td>{@code dimension}</td><td>{@code ResourceLocation}</td><td>No</td><td>The dimension of the hit entity.</td></tr>
 *   <tr><td>{@code biome}</td><td>{@code ResourceLocation}</td><td>Yes</td><td>The biome of the hit entity.</td></tr>
 *   <tr><td>{@code pitch}/{@code yaw}</td><td>{@code Double}</td><td>No</td><td>The hit entity's rotation angles.</td></tr>
 *   <tr><td>{@code rotation}</td><td>{@code Vec2}</td><td>No</td><td>The hit entity's rotation vector.</td></tr>
 *   <tr><td>{@code sx}/{@code sy}/{@code sz}</td><td>{@code Double}</td><td>No</td><td>The shooter's coordinates.</td></tr>
 *   <tr><td>{@code sposition}</td><td>{@code Vec3}</td><td>No</td><td>The shooter's position.</td></tr>
 *   <tr><td>{@code sdimension}</td><td>{@code ResourceLocation}</td><td>No</td><td>The dimension of the shooter.</td></tr>
 *   <tr><td>{@code sbiome}</td><td>{@code ResourceLocation}</td><td>Yes</td><td>The biome of the shooter.</td></tr>
 *   <tr><td>{@code spitch}/{@code syaw}</td><td>{@code Double}</td><td>No</td><td>The shooter's rotation angles.</td></tr>
 *   <tr><td>{@code srotation}</td><td>{@code Vec2}</td><td>No</td><td>The shooter's rotation vector.</td></tr>
 *   <tr><td>{@code distance}</td><td>{@code Double}</td><td>No</td><td>The distance between the shooter and the hit entity.</td></tr>
 *   <tr><td>{@code health}</td><td>{@code Double}</td><td>No</td><td>The hit entity's health before being hit.</td></tr>
 *   <tr><td>{@code name}</td><td>{@code String}</td><td>No</td><td>The hit entity's display name.</td></tr>
 *   <tr><td>{@code sname}</td><td>{@code String}</td><td>No</td><td>The shooter's display name.</td></tr>
 * </table>
 *
 * <p>This class is a singleton; obtain the instance via {@link #getInstance()}.
 */
public class ProjectileHitEntityEvent implements RuleEvent {

    private static final ProjectileHitEntityEvent INSTANCE = new ProjectileHitEntityEvent();

    private final Map<String, Class<? extends Object>> variableTypes;

    private final Set<String> variablesList;

    public static final String TYPE = "ProjectileHitEntityEvent";

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
        return Util.parseTranslatableText("fmod.rule.event.projectilehitentity");
    }
}
