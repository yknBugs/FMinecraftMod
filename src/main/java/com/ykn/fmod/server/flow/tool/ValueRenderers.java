/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.tool;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;

/**
 * Renders a flow value as text for node status display.
 *
 * <p>The rendering of a type is registered here instead of being hardcoded, so a mod that
 * introduces a value type can register how it should appear:
 *
 * <pre>{@code
 * ValueRenderers.register(MyValue.class, value -> "MyValue(" + value.id() + ")");
 * }</pre>
 *
 * <p>The registry is consulted before the structural fallbacks: registered types first, then
 * {@link Map}, {@link Iterable} and arrays (rendered recursively), then {@link Object#toString()}.
 * Rendering is bounded - six levels deep, at most {@value #MAX_ITEMS} entries per container and
 * {@value #MAX_CHARS} characters per value - and cycle-safe through identity tracking.
 *
 * <p>{@link #registerDefaults()} covers every built-in type and is called from
 * {@code NodeRegistry.registerDefaultNodes()}; later registrations of the same type replace it.
 */
public final class ValueRenderers {

    /**
     * Renders one value of {@code T} as text. Implementations must not recurse into
     * {@link ValueRenderers#render(Object)} unless they start a fresh rendering.
     */
    @FunctionalInterface
    public interface Renderer<T> {
        String render(T value);
    }

    private static final Map<Class<?>, Renderer<?>> RENDERERS = new LinkedHashMap<>();

    private static final int MAX_CHARS = 4096;

    private static final int MAX_ITEMS = 32;

    private static final int MAX_DEPTH = 6;

    private ValueRenderers() {
    }

    /**
     * Registers the renderer for {@code type}, replacing any earlier one.
     *
     * @param type     the type to render
     * @param renderer the renderer
     * @param <T>      the rendered type
     */
    public static <T> void register(Class<T> type, Renderer<? super T> renderer) {
        RENDERERS.put(type, renderer);
    }

    /**
     * Registers the renderer for every built-in flow value type.
     */
    public static void registerDefaults() {
        register(Component.class, Component::getString);
        register(Vec2.class, value -> "(" + value.x + ", " + value.y + ")");
        register(Vec3i.class, value -> "(" + value.getX() + ", " + value.getY() + ", " + value.getZ() + ")");
        register(Entity.class, value -> value.getName().getString()
                + " [" + BuiltInRegistries.ENTITY_TYPE.getKey(value.getType()) + ", " + value.getUUID() + "]");
        register(ItemStack.class, value -> {
            String base = BuiltInRegistries.ITEM.getKey(value.getItem()) + " × " + value.getCount();
            return value.getComponentsPatch().isEmpty() ? base : base + " " + value.getComponentsPatch();
        });
        register(Level.class, value -> value.dimension().location().toString());
        register(BlockEntity.class, value -> BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(value.getType())
                + " @ " + value.getBlockPos().toShortString());
        register(LevelChunk.class, value -> "Chunk " + value.getPos() + " @ " + value.getLevel().dimension().location());
        register(EntityHitResult.class, value -> ValueRenderers.render(value.getEntity()) + " @ " + value.getLocation());
        register(BlockHitResult.class, value -> value.getType() + " @ " + value.getBlockPos().toShortString()
                + " face = " + value.getDirection() + " hit = " + value.getLocation());
        register(HitResult.class, value -> value.getType() + " @ " + value.getLocation());
        register(AdvancementHolder.class, value -> value.id().toString());
        register(AbstractContainerMenu.class, value -> value.getClass().getSimpleName()
                + " #" + value.containerId + " slots = " + value.slots.size());
        register(MerchantOffer.class, value -> ValueRenderers.render(value.getResult()) + " uses = " + value.getUses());
        register(DataComponentPatch.class, Object::toString);
        register(DamageSource.class, value -> value.getMsgId()
                + " source = " + ValueRenderers.render(value.getEntity()));
        register(Explosion.class, value -> "Explosion blocks = " + value.getToBlow().size());
        register(MinecraftServer.class, value -> "Server " + value.getMotd());
        register(Tag.class, Tag::toString);
    }

    /**
     * Renders a value, bounded and cycle-safe.
     *
     * @param value the value to render, may be {@code null}
     * @return the display text
     */
    public static String render(Object value) {
        StringBuilder out = new StringBuilder();
        append(out, value, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
        return out.length() > MAX_CHARS ? out.substring(0, MAX_CHARS) + "…" : out.toString();
    }

    private static void append(StringBuilder out, Object value, Set<Object> seen, int depth) {
        if (out.length() >= MAX_CHARS) {
            return;
        }
        if (value == null) {
            out.append("null");
            return;
        }
        if (depth >= MAX_DEPTH) {
            out.append("…");
            return;
        }
        if (!seen.add(value)) {
            out.append("<cycle>");
            return;
        }
        try {
            Renderer<?> renderer = find(value);
            if (renderer != null) {
                out.append(render(renderer, value));
            } else if (value instanceof Map<?, ?> map) {
                appendMap(out, map, seen, depth);
            } else if (value instanceof Iterable<?> iterable) {
                appendIterable(out, iterable, seen, depth);
            } else if (value.getClass().isArray()) {
                appendArray(out, value, seen, depth);
            } else {
                appendFallback(out, value);
            }
        } catch (RuntimeException exception) {
            out.append('<').append(value.getClass().getSimpleName()).append(" unavailable>");
        } finally {
            seen.remove(value);
        }
    }

    private static Renderer<?> find(Object value) {
        Renderer<?> exact = RENDERERS.get(value.getClass());
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<Class<?>, Renderer<?>> entry : RENDERERS.entrySet()) {
            if (entry.getKey().isInstance(value)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String render(Renderer<?> renderer, Object value) {
        return ((Renderer<Object>) renderer).render(value);
    }

    private static void appendMap(StringBuilder out, Map<?, ?> map, Set<Object> seen, int depth) {
        out.append('{');
        int index = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (index > 0) {
                out.append(", ");
            }
            if (index++ >= MAX_ITEMS || out.length() >= MAX_CHARS) {
                out.append('…');
                break;
            }
            append(out, entry.getKey(), seen, depth + 1);
            out.append('=');
            append(out, entry.getValue(), seen, depth + 1);
        }
        out.append('}');
    }

    private static void appendIterable(StringBuilder out, Iterable<?> iterable, Set<Object> seen, int depth) {
        out.append('[');
        int index = 0;
        for (Object item : iterable) {
            if (index > 0) {
                out.append(", ");
            }
            if (index++ >= MAX_ITEMS || out.length() >= MAX_CHARS) {
                out.append('…');
                break;
            }
            append(out, item, seen, depth + 1);
        }
        out.append(']');
    }

    private static void appendArray(StringBuilder out, Object array, Set<Object> seen, int depth) {
        out.append('[');
        int length = Array.getLength(array);
        for (int index = 0; index < Math.min(length, MAX_ITEMS) && out.length() < MAX_CHARS; index++) {
            if (index > 0) {
                out.append(", ");
            }
            append(out, Array.get(array, index), seen, depth + 1);
        }
        if (length > MAX_ITEMS) {
            out.append(", …");
        }
        out.append(']');
    }

    private static void appendFallback(StringBuilder out, Object value) {
        String text = String.valueOf(value);
        // Default Object.toString is an address-like identity, not useful node data.
        if (text.startsWith(value.getClass().getName() + "@")) {
            text = value.getClass().getSimpleName();
        }
        out.append(text, 0, Math.min(text.length(), MAX_CHARS));
    }
}
