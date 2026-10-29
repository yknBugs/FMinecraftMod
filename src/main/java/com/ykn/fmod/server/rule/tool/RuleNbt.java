/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import java.util.List;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

/**
 * Strict SNBT and NBT-path handling with copy-on-write operations.
 *
 * <p>No method mutates an input tag: {@link #write}, {@link #delete} and {@link #get} all work on
 * copies, so a rule can never alter world data through a shared tag reference.
 *
 * <p>Malformed paths and over-deep trees throw {@link IllegalArgumentException}. Callers in the
 * component layer catch it at the call site, report a warning and fail that component; the
 * exception is never swallowed here, and always marks a real input problem rather than a bug.
 */
public final class RuleNbt {

    private RuleNbt() {
    }

    /**
     * Parses one complete SNBT value.
     *
     * @param text the SNBT text
     * @return the parsed tag
     * @throws IllegalArgumentException when the text is malformed, has trailing data, or is too deep
     */
    public static Tag parse(String text) {
        StringReader reader = new StringReader(text);
        try {
            Tag result = new TagParser(reader).readValue();
            reader.skipWhitespace();
            if (reader.canRead()) {
                throw new IllegalArgumentException("Trailing SNBT data");
            }
            validateDepth(result);
            return result;
        } catch (CommandSyntaxException | IndexOutOfBoundsException exception) {
            throw new IllegalArgumentException("Invalid SNBT", exception);
        }
    }

    /**
     * Parses a compound root, rejecting primitive and list roots.
     *
     * @param text the SNBT text
     * @return the parsed compound
     * @throws IllegalArgumentException when the text is not a compound
     */
    public static CompoundTag compound(String text) {
        Tag result = parse(text);
        if (!(result instanceof CompoundTag compound)) {
            throw new IllegalArgumentException("Expected compound NBT");
        }
        return compound;
    }

    /**
     * Parses one complete non-empty vanilla NBT path.
     *
     * @param text the path text
     * @return the parsed path
     * @throws IllegalArgumentException when the path is malformed or empty
     */
    public static NbtPathArgument.NbtPath path(String text) {
        StringReader reader = new StringReader(text);
        try {
            NbtPathArgument.NbtPath result = NbtPathArgument.nbtPath().parse(reader);
            reader.skipWhitespace();
            if (text.isBlank() || reader.canRead()) {
                throw new IllegalArgumentException("Invalid NBT path");
            }
            return result;
        } catch (CommandSyntaxException | IndexOutOfBoundsException exception) {
            throw new IllegalArgumentException("Invalid NBT path", exception);
        }
    }

    /**
     * Validates a path while retaining its text for variable binding and serialization.
     *
     * @param text the path text
     * @return the canonical path text
     * @throws IllegalArgumentException when the path is malformed
     */
    public static String pathText(String text) {
        return path(text).toString();
    }

    /**
     * Rejects trees exceeding vanilla's NBT path depth limit.
     *
     * @param tag the tree to check
     * @throws IllegalArgumentException when the tree is too deep
     */
    public static void validateDepth(Tag tag) {
        if (NbtPathArgument.NbtPath.isTooDeep(tag, 0)) {
            throw new IllegalArgumentException("NBT too deep");
        }
    }

    /**
     * Returns a detached tag for exactly one match.
     *
     * @param source the tree to read, or {@code null} for no tree
     * @param path   the NBT path
     * @return a copy of the single match, or {@code null} for a null source, no match, or several matches
     * @throws IllegalArgumentException for a malformed path or an over-deep tree
     */
    public static Tag get(Tag source, String path) {
        if (source == null) {
            return null;
        }
        validateDepth(source);
        try {
            List<Tag> matches = path(path).get(source);
            return matches.size() == 1 ? matches.get(0).copy() : null;
        } catch (CommandSyntaxException exception) {
            return null;
        }
    }

    /**
     * Compares one NBT value with a tag, including tag types.
     *
     * @param source   the tree to read
     * @param path     the NBT path
     * @param expected the expected tag
     * @return {@code true} only for exactly one match that equals {@code expected}
     * @throws IllegalArgumentException for a malformed path or an over-deep tree
     */
    public static boolean check(Tag source, String path, Tag expected) {
        Tag actual = get(source, path);
        return actual != null && actual.equals(expected);
    }

    /**
     * Removes all matched entries from a copy of the source.
     *
     * @param source the tree to modify
     * @param text   the NBT path
     * @return the modified copy, or {@code null} when nothing matched
     * @throws IllegalArgumentException for a malformed path or an over-deep tree
     */
    public static Tag delete(Tag source, String text) {
        validateDepth(source);
        Tag copy = source.copy();
        return path(text).remove(copy) > 0 ? copy : null;
    }

    /**
     * Writes a copied value into a copied tree.
     *
     * <p>{@code add} requires zero matches; an update requires exactly one. Replacing a value with
     * an equal one still counts as a successful update.
     *
     * @param source the tree to modify
     * @param text   the NBT path
     * @param value  the value to write
     * @param add    {@code true} to add a new entry, {@code false} to update an existing one
     * @return the modified copy, or {@code null} for an incompatible write or an invalid match count
     * @throws IllegalArgumentException for a malformed path or an over-deep tree
     */
    public static Tag write(Tag source, String text, Tag value, boolean add) {
        validateDepth(source);
        validateDepth(value);
        NbtPathArgument.NbtPath path = path(text);
        int count = path.countMatching(source);
        if (add ? count != 0 : count != 1) {
            return null;
        }
        Tag copy = source.copy();
        try {
            int changed = path.set(copy, value.copy());
            validateDepth(copy);
            // A filter may no longer match after replacement; use the mutation count, not a re-query.
            if (changed == 1) {
                return copy;
            }
            // set() may silently decline incompatible list/array types. Only a genuine no-op succeeds.
            return !add && changed == 0 && path.get(source).get(0).equals(value) ? copy : null;
        } catch (CommandSyntaxException exception) {
            return null;
        }
    }
}
