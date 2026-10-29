/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.LogicException;

import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.data.BlockDataAccessor;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Automatic conversion between flow port values and the shapes nodes need.
 *
 * <p>A flow port is untyped: a node may receive an entity object or a UUID string, a list or a
 * single value, a {@link Tag} or a value that has to be serialized first. Everything that turns one
 * into the other lives here, so nodes do not each re-implement (and subtly diverge on) the
 * conversion.
 *
 * <p>Live game objects are only read here; the server thread requirement is the caller's.
 *
 * <p>Only nodes call this class, and only while a flow is executing - so a user mistake is reported
 * as {@link LogicException}, the engine's execution-time error type. Loading a {@code .flow} file or
 * editing one through commands is <em>not</em> execution and never goes through here.
 */
public final class FlowConvert {

    private FlowConvert() {
    }

    /**
     * Returns the UUID for a {@link UUID} or a canonical UUID string.
     *
     * @param value the candidate value
     * @return the UUID, or {@code null} when {@code value} is neither of those
     */
    public static UUID uuid(Object value) {
        if (value instanceof UUID id) {
            return id;
        }
        if (value instanceof String text) {
            try {
                UUID id = UUID.fromString(text.strip());
                if (id.toString().equalsIgnoreCase(text.strip())) {
                    return id;
                }
            } catch (IllegalArgumentException ignored) {
                // Not a canonical UUID string; the caller decides what to do with the value.
            }
        }
        return null;
    }

    /**
     * Returns an existing entity, or resolves a UUID in the server's loaded levels.
     *
     * @param value  the entity or UUID(-string)
     * @param server the server whose levels are searched
     * @return the resolved entity
     * @throws LogicException if a non-null value cannot be resolved
     */
    public static Entity entity(Object value, MinecraftServer server) throws LogicException {
        if (value instanceof Entity entity) {
            return entity;
        }
        UUID id = uuid(value);
        if (id != null && server != null) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(id);
                if (entity != null) {
                    return entity;
                }
            }
        }
        throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.entitylookup", String.valueOf(value)), null);
    }

    /**
     * Resolves UUID references for NBT sources; values that are not UUIDs pass through unchanged.
     *
     * @param value  the candidate NBT source
     * @param server the server used to resolve entity UUIDs
     * @return the value, or the entity a UUID pointed at
     * @throws LogicException if a UUID does not resolve to a loaded entity
     */
    public static Object source(Object value, MinecraftServer server) throws LogicException {
        return uuid(value) == null ? value : entity(value, server);
    }

    /**
     * Checks a prospective list size before anything is allocated or mutated.
     *
     * @param size the size the list would reach
     * @throws LogicException if the size is negative or above the configured limit
     */
    public static void checkListSize(long size) throws LogicException {
        int limit = Util.getServerConfig().getMaxFlowListSize();
        if (size < 0 || size > limit) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.listsize", size, limit), null);
        }
    }

    /**
     * Splits {@code text} with the semantics of {@link String#split(String)}, but bounded.
     *
     * @param text      the text to split
     * @param delimiter the literal delimiter
     * @return the split parts, without trailing empty parts
     * @throws LogicException if the result would exceed the list size limit
     */
    public static List<String> split(String text, String delimiter) throws LogicException {
        Matcher matcher = Pattern.compile(Pattern.quote(delimiter)).matcher(text);
        List<String> result = new ArrayList<>();
        int start = 0;
        int pendingEmpty = 0;
        while (matcher.find()) {
            if (start == 0 && matcher.start() == 0 && matcher.end() == 0) {
                continue;
            }
            String part = text.substring(start, matcher.start());
            if (part.isEmpty()) {
                pendingEmpty++;
            } else {
                checkListSize((long) result.size() + pendingEmpty + 1);
                while (pendingEmpty > 0) {
                    result.add("");
                    pendingEmpty--;
                }
                result.add(part);
            }
            start = matcher.end();
        }
        if (start == 0) {
            checkListSize(1);
            return new ArrayList<>(List.of(text));
        }
        String tail = text.substring(start);
        if (!tail.isEmpty()) {
            checkListSize((long) result.size() + pendingEmpty + 1);
            while (pendingEmpty > 0) {
                result.add("");
                pendingEmpty--;
            }
            result.add(tail);
        }
        return result;
    }

    /**
     * Adapts a value for a list port: {@code null} is the empty list, a list is copied, and any
     * other value becomes a single-element list (strings are not parsed).
     *
     * @param value the port value
     * @return a mutable list holding the value(s)
     * @throws LogicException if the result would exceed the list size limit
     */
    public static List<Object> listInput(Object value) throws LogicException {
        long size = value == null ? 0 : value instanceof List<?> list ? list.size() : 1;
        checkListSize(size);
        List<Object> result = new ArrayList<>((int) size);
        if (value instanceof List<?> list) {
            result.addAll(list);
        } else if (value != null) {
            result.add(value);
        }
        return result;
    }

    /**
     * Reads a list port tolerantly: iterables are copied, and bracketed text is parsed element by
     * element through {@link TypeAdaptor}.
     *
     * @param input the port value
     * @return a mutable list, or {@code null} when the text is not a bracketed list
     * @throws LogicException if the result would exceed the list size limit
     */
    public static List<Object> listCopy(Object input) throws LogicException {
        if (input instanceof List<?> list) {
            checkListSize(list.size());
        }
        if (input instanceof Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            for (Object value : iterable) {
                checkListSize((long) result.size() + 1);
                result.add(value);
            }
            return result;
        }
        String text = TypeAdaptor.parse(input).asString().strip();
        if (!text.startsWith("[") || !text.endsWith("]")) {
            return null;
        }
        text = text.substring(1, text.length() - 1).strip();
        List<Object> result = new ArrayList<>();
        // Match String.split(",") trailing-empty behaviour without allocating an unbounded array.
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == ',') {
            end--;
        }
        if (end == 0 && !text.isEmpty()) {
            return result;
        }
        int start = 0;
        do {
            int comma = text.indexOf(',', start);
            int next = comma < 0 || comma >= end ? end : comma;
            checkListSize((long) result.size() + 1);
            String part = text.substring(start, next).strip();
            result.add(part.startsWith("[") && part.endsWith("]") ? part : TypeAdaptor.parse(part).autoCast());
            start = next + 1;
        } while (start <= end);
        return result;
    }

    /**
     * Unwraps a one-element list produced as a node output, so a port declared as "one value or a
     * list of them" hands a scalar to the next node.
     *
     * <p>Empty lists, lists with several elements and NBT values are returned unchanged: a
     * {@link ListTag} is a list value, not a wrapper.
     *
     * @param value the value about to be written to an output port
     * @return the single element, or {@code value} itself
     */
    public static Object unwrapOutput(Object value) {
        if (value instanceof Tag) {
            return value;
        }
        return value instanceof List<?> list && list.size() == 1 ? list.get(0) : value;
    }

    /**
     * A detached NBT edit result.
     *
     * @param nbt      the edited tree
     * @param oldValue the previous value(s): {@code null}, a {@link Tag}, or an immutable list of copied tags
     * @param existed  whether the path matched anything before the edit
     * @param changed  how many values the edit wrote or removed
     */
    public record NbtEdit(Tag nbt, Object oldValue, boolean existed, int changed) {
    }

    /**
     * Edits a detached NBT snapshot without mutating either input.
     *
     * <p>A {@code null} source starts an empty compound, a {@code null} path addresses the root, and
     * a {@code null} value deletes. Missing deletions are successful no-ops. Vanilla paths can match
     * several values; incompatible writes fail instead of reporting success. Item stacks serialize
     * to their full stack NBT, including id, Count and tag.
     *
     * @param source   the value to edit: NBT, an entity, a block entity, an item stack, or a supported scalar
     * @param pathText the NBT path to edit, or {@code null} to replace the root
     * @param value    the value to write, or {@code null} to delete
     * @return the edit result
     * @throws LogicException for an invalid path, an unsupported value, excessive depth, or a path
     *                        that cannot accept the value
     */
    public static NbtEdit editNbt(Object source, String pathText, Object value) throws LogicException {
        Tag original = source == null ? null : adapt(source, 0);
        Tag replacement = value == null ? null : adapt(value, 0);
        if (original != null) {
            validateDepth(original);
        }
        if (replacement != null) {
            validateDepth(replacement);
        }
        if (pathText == null) {
            return new NbtEdit(replacement, original, original != null,
                Objects.equals(original, replacement) ? 0 : 1);
        }
        NbtPathArgument.NbtPath path = parsePath(pathText);
        Tag tree = original == null ? new CompoundTag() : original.copy();
        List<Tag> old;
        try {
            old = path.countMatching(tree) == 0 ? List.of() : path.get(tree).stream().map(Tag::copy).toList();
        } catch (CommandSyntaxException exception) {
            throw new LogicException(exception, Util.parseTranslatableText("fmod.node.error.nbtpathread", pathText), null);
        }
        int changed;
        try {
            if (replacement == null) {
                changed = path.remove(tree);
            } else {
                changed = path.set(tree, replacement);
            }
        } catch (CommandSyntaxException exception) {
            throw new LogicException(exception, Util.parseTranslatableText("fmod.node.error.nbtpathwrite", pathText), null);
        }
        if (replacement != null) {
            long expectedChanges = old.stream().filter(tag -> !tag.equals(replacement)).count();
            if ((old.isEmpty() && changed == 0) || changed < expectedChanges) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.nbtpathwrite", pathText), null);
            }
        }
        validateDepth(tree);
        return new NbtEdit(tree, old.isEmpty() ? null : old.size() == 1 ? old.get(0) : old, !old.isEmpty(), changed);
    }

    private static NbtPathArgument.NbtPath parsePath(String pathText) throws LogicException {
        StringReader reader = new StringReader(pathText);
        try {
            NbtPathArgument.NbtPath path = NbtPathArgument.nbtPath().parse(reader);
            reader.skipWhitespace();
            if (pathText.isBlank() || reader.canRead()) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.nbtpath", pathText), null);
            }
            return path;
        } catch (CommandSyntaxException exception) {
            throw new LogicException(exception, Util.parseTranslatableText("fmod.node.error.nbtpath", pathText), null);
        }
    }

    /**
     * Parses a complete SNBT source.
     *
     * @param text the SNBT text
     * @return the parsed, depth-checked tag
     * @throws LogicException if the text is not valid SNBT
     */
    public static Tag parseNbt(String text) throws LogicException {
        return adapt(text, 0);
    }

    /**
     * Serializes a supported value to a detached NBT tree.
     *
     * @param value the value to serialize
     * @return the detached, depth-checked tag
     * @throws LogicException if the value cannot be represented as NBT
     */
    public static Tag snapshotNbt(Object value) throws LogicException {
        Tag tag = adapt(value, 0);
        validateDepth(tag);
        return tag;
    }

    private static void validateDepth(Tag tag) throws LogicException {
        if (NbtPathArgument.NbtPath.isTooDeep(tag, 0)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.nbtdeep"), null);
        }
    }

    private static Tag adapt(Object value, int depth) throws LogicException {
        if (depth >= 512) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.nbtdeep"), null);
        }
        if (value instanceof Tag tag) {
            validateDepth(tag);
            return tag.copy();
        }
        if (value instanceof Entity entity) {
            return new EntityDataAccessor(entity).getData().copy();
        }
        if (value instanceof BlockEntity block) {
            return new BlockDataAccessor(block, block.getBlockPos()).getData().copy();
        }
        if (value instanceof ItemStack stack) {
            return stack.save(new CompoundTag()).copy();
        }
        if (value instanceof Boolean b) {
            return ByteTag.valueOf(b);
        }
        if (value instanceof Byte n) {
            return ByteTag.valueOf(n);
        }
        if (value instanceof Short n) {
            return ShortTag.valueOf(n);
        }
        if (value instanceof Integer n) {
            return IntTag.valueOf(n);
        }
        if (value instanceof Long n) {
            return LongTag.valueOf(n);
        }
        if (value instanceof Float n) {
            return FloatTag.valueOf(n);
        }
        if (value instanceof Number n) {
            return DoubleTag.valueOf(n.doubleValue());
        }
        if (value instanceof byte[] array) {
            return new ByteArrayTag(array.clone());
        }
        if (value instanceof int[] array) {
            return new IntArrayTag(array.clone());
        }
        if (value instanceof long[] array) {
            return new LongArrayTag(array.clone());
        }
        if (value instanceof String text) {
            Tag tag;
            StringReader reader = new StringReader(text);
            try {
                tag = new TagParser(reader).readValue();
            } catch (CommandSyntaxException exception) {
                throw new LogicException(exception, Util.parseTranslatableText("fmod.node.error.snbt", text), null);
            }
            reader.skipWhitespace();
            if (reader.canRead()) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.snbttrailing"), null);
            }
            validateDepth(tag);
            return tag;
        }
        if (value instanceof Map<?, ?> map) {
            CompoundTag result = new CompoundTag();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.nbtkeys"), null);
                }
                result.put(key, adapt(entry.getValue(), depth + 1));
            }
            return result;
        }
        if (value instanceof List<?> list) {
            ListTag result = new ListTag();
            for (Object entry : list) {
                if (!result.addTag(result.size(), adapt(entry, depth + 1))) {
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.nbtmixed"), null);
                }
            }
            return result;
        }
        throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.nbtvalue",
            value == null ? "null" : value.getClass().getSimpleName()), null);
    }
}
