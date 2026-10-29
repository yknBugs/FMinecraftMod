/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.Entity;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import com.ykn.fmod.server.flow.tool.FlowConvert;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;

/**
 * A flow node that queries, adds, removes or toggles entity scoreboard tags.
 * Inputs:
 * 1. Entity/UUID/String - Required entity or UUID on this server.
 * 2. String/List<String>/StringTag - Exact case-sensitive string or string list; duplicates are processed once. Null selects no tags.
 * 3. Boolean - Default false. Add supplied tags; with remove enabled, toggle each supplied tag.
 * 4. Boolean - Default false. Remove matching tags; with update enabled, toggle each supplied tag.
 * Outputs:
 * 1. List<String> - Sorted immutable snapshot of all tags before the operation. A one-element list is returned as its sole element.
 * 2. List<String> - Distinct supplied tags that existed before the operation, sorted. A one-element list is returned as its sole element.
 * 3. List<String> - Sorted immutable snapshot after the operation; getter mode shares the old snapshot. A one-element list is returned as its sole element.
 * 4. Integer - Number of distinct tags matched before the operation.
 * Branches: 1 (Next node)
 */
public class ManageTagNode extends FlowNode {

    public ManageTagNode(long id, String name) {
        super(id, name, 4, 4, 1, "ManageTagNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        NodeMetadata.Builder builder = NodeMetadata.builder("fmod.node.managetag.title.name", "fmod.node.managetag.title.feat");
        for (String port : List.of("entity", "tags", "update", "remove")) {
            String key = "fmod.node.managetag.input." + port;
            builder.input(key + ".name", key + ".feat", key + ".type");
        }
        for (String port : List.of("old", "matched", "new", "count")) {
            String key = "fmod.node.managetag.output." + port;
            builder.output(key + ".name", key + ".feat", key + ".type");
        }
        return builder.branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat").build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        Entity entity = FlowConvert.entity(inputs.get(0), context.getServer());
        if (entity.isRemoved() || entity.level().getServer() != context.getServer()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.managetag.error.entity"), null);
        }
        Result result = plan(entity.getTags(), inputs.get(1), flag(inputs.get(2)), flag(inputs.get(3)));
        // Remove first, so toggling at the vanilla 1024-tag limit can free capacity for sadditions.
        for (String tag : result.oldTags()) {
            if (!result.newTags().contains(tag)) {
                entity.removeTag(tag);
            }
        }
        for (String tag : result.newTags()) {
            if (!entity.getTags().contains(tag)) {
                entity.addTag(tag);
            }
        }
        status.setOutput(0, FlowConvert.unwrapOutput(result.oldTags()));
        status.setOutput(1, FlowConvert.unwrapOutput(result.matchedTags()));
        status.setOutput(2, result.newTags());
        status.setOutput(3, result.matchedTags().size());
    }

    /** 
     * Detached plan, computed before changing the entity. 
     */
    record Result(List<String> oldTags, List<String> matchedTags, List<String> newTags) {
    }

    static Result plan(Set<String> existing, Object input, boolean update, boolean remove) throws LogicException {
        FlowConvert.checkListSize(existing.size());
        Set<String> requested = new TreeSet<>();
        if (input != null) {
            List<?> values = FlowConvert.listInput(input);
            FlowConvert.checkListSize(values.size());
            for (Object value : values) {
                String tag = value instanceof String string ? string : value instanceof StringTag nbt ? nbt.getAsString() : null;
                if (tag == null) {
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.managetag.error.tag"), null);
                }
                requested.add(tag);
            }
        }
        List<String> old = existing.stream().sorted().toList();
        List<String> matches = requested.stream().filter(existing::contains).toList();
        if (!update && !remove) {
            return new Result(old, matches, old);
        }
        Set<String> next = new TreeSet<>(existing);
        if (remove) {
            next.removeAll(requested);
        }
        if (update) {
            for (String tag : requested) {
                if (!remove || !existing.contains(tag)) {
                    next.add(tag);
                }
            }
        }
        FlowConvert.checkListSize(next.size());
        if (next.size() > 1024) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.managetag.error.limit"), null);
        }
        return new Result(old, matches, List.copyOf(next));
    }

    static boolean flag(Object input) throws LogicException {
        if (input == null) {
            return false;
        }
        Boolean value = TypeAdaptor.parse(input).asBoolean();
        if (value == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.boolean", input), null);
        }
        return value;
    }
}
