/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ykn.fmod.server.base.util.GameMath;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import com.ykn.fmod.server.flow.tool.FlowConvert;
import java.util.Set;

/**
 * A flow node that gathers the entities that satisfy the given conditions.
 * Inputs:
 * 1. String - Force specify the UUID of the entity to get and ignore all other conditions.
 * 2. Enum - The identifier of the entity type to gather.
 * 3. World - The world to search for entities in.
 * 4. Vec3d - The center position to search around.
 * 5. Number - The radius to search within in meters.
 * 6. String - Optional exact, case-sensitive scoreboard tag (/tag). Null disables filtering. UUID lookup bypasses this filter.
 * Outputs:
 * 1. List[Entity] - The list of entities that match the given conditions.
 * Branches: 1 (Next node)
 */
public class GatherEntityNode extends FlowNode {

    public GatherEntityNode(long id, String name) {
        super(id, name, 6, 1, 1, "GatherEntityNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.gatherentity.title.name", "fmod.node.gatherentity.title.feat")
            .input("fmod.node.gatherentity.input.uuid.name", "fmod.node.gatherentity.input.uuid.feat", "fmod.node.gatherentity.input.uuid.type")
            .input("fmod.node.gatherentity.input.type.name", "fmod.node.gatherentity.input.type.feat", "fmod.node.gatherentity.input.type.type")
            .input("fmod.node.gatherentity.input.world.name", "fmod.node.gatherentity.input.world.feat", "fmod.node.gatherentity.input.world.type")
            .input("fmod.node.gatherentity.input.position.name", "fmod.node.gatherentity.input.position.feat", "fmod.node.gatherentity.input.position.type")
            .input("fmod.node.gatherentity.input.radius.name", "fmod.node.gatherentity.input.radius.feat", "fmod.node.gatherentity.input.radius.type")
            .input("fmod.node.gatherentity.input.tag.name", "fmod.node.gatherentity.input.tag.feat", "fmod.node.gatherentity.input.tag.type")
            .output("fmod.node.gatherentity.output.entities.name", "fmod.node.gatherentity.output.entities.feat", "fmod.node.gatherentity.output.entities.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        String uuidStr = TypeAdaptor.parse(resolvedInputs.get(0)).asString().strip();
        ResourceLocation typeFilter = parseIdentifier(resolvedInputs.get(1));
        ServerLevel worldFilter = parseWorld(resolvedInputs.get(2));
        Vec3 positionFilter = TypeAdaptor.parse(resolvedInputs.get(3)).asVec3d();
        Double radiusFilter = TypeAdaptor.parse(resolvedInputs.get(4)).asDouble();

        if (positionFilter == null && radiusFilter != null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames().get(3)), null);
        }
        if (positionFilter != null && radiusFilter == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames().get(4)), null);
        }

        if (radiusFilter != null) {
            if (Double.isNaN(radiusFilter) || radiusFilter < 0) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.gatherentity.error.math", radiusFilter), null);
            }
        }

        String tagFilter = resolvedInputs.get(5) == null ? null : TypeAdaptor.parse(resolvedInputs.get(5)).asString();

        List<Entity> resultEntities = new ArrayList<>();
        if (!uuidStr.isEmpty()) {
            // UUID search takes precedence
            try {
                UUID uuid = UUID.fromString(uuidStr);
                for (ServerLevel world : context.getServer().getAllLevels()) {
                    Entity entity = world.getEntity(uuid);
                    if (entity != null) {
                        FlowConvert.checkListSize((long) resultEntities.size() + 1);
                        resultEntities.add(entity);
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.gatherentity.error.uuid", uuidStr), null);
            }
        } else {
            // World filter
            Iterable<ServerLevel> worldsToSearch;
            if (worldFilter != null) {
                List<ServerLevel> singleWorld = new ArrayList<>();
                singleWorld.add(worldFilter);
                worldsToSearch = singleWorld;
            } else {
                worldsToSearch = context.getServer().getAllLevels();
            }

            for (ServerLevel world : worldsToSearch) {
                List<Entity> entities = Util.getAllEntities(world);
                
                for (Entity entity : entities) {
                    if (!matchesTag(entity.getTags(), tagFilter)) {
                        continue;
                    }
                    // Type filter
                    if (typeFilter != null) {
                        ResourceLocation entityType = EntityType.getKey(entity.getType());
                        if (entityType == null || !entityType.equals(typeFilter)) {
                            continue;
                        }
                    }

                    // Position and radius filter
                    if (positionFilter != null && radiusFilter != null) {
                        double distance = GameMath.getEuclideanDistance(entity.position(), positionFilter);
                        if (distance > radiusFilter) {
                            continue;   
                        }
                    }

                    FlowConvert.checkListSize((long) resultEntities.size() + 1);
                    resultEntities.add(entity);
                }
            }
        }

        status.setOutput(0, FlowConvert.unwrapOutput(resultEntities.isEmpty() ? null : resultEntities));
    }
    
    /** Null disables filtering; otherwise matches the exact, case-sensitive scoreboard tag. */
    static boolean matchesTag(Set<String> tags, String filter) {
        return filter == null || tags.contains(filter);
    }

    private ResourceLocation parseIdentifier(Object obj) throws LogicException {
        if (obj == null || obj instanceof String text && text.isBlank()) {
            return null;
        }
        ResourceLocation id = TypeAdaptor.parse(obj).asResourceLocation();
        if (id == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.id", name, String.valueOf(obj)), null);
        }
        return id;
    }

    private ServerLevel parseWorld(Object obj) throws LogicException {
        if (obj == null) {
            return null;
        } else if (obj instanceof ServerLevel) {
            return (ServerLevel) obj;
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames().get(2), this.metadata.inputDataTypes().get(2)), null);
        }
    }
}
