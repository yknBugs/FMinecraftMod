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
import com.ykn.fmod.server.flow.logic.NodeStatus;

import com.ykn.fmod.server.flow.logic.NodeMetadata;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Get entities matching certain criteria, such as nearby entities, entities in a certain area, etc.
 * Inputs:
 * 1. String - UUID, if provided, ignore all other filters and directly get the entity with this UUID.
 * 2. Identifier - The type of the entity (e.g. "minecraft:player") to filter by (optional).
 * 3. World - The world to search for entities in (optional, defaults from all loaded worlds).
 * 4. Vec3d - The center position to search around (optional, if provided, 5 must not be null).
 * 5. Double - The radius to search within (optional, if provided, 4 must not be null).
 * Outputs:
 * 1. List - The list of entities matching the specified criteria.
 * Branches: 1 (Next node)
 */
public class GatherEntityNode extends FlowNode {

    public GatherEntityNode(long id, String name) {
        super(id, name, 5, 1, 1);
        this.type = "GatherEntityNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Text displayName = Util.parseTranslatableText("fmod.node.gatherentity.title.name");
        Text description = Util.parseTranslatableText("fmod.node.gatherentity.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.gatherentity.input.uuid.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.gatherentity.input.uuid.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.gatherentity.input.uuid.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.gatherentity.input.type.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.gatherentity.input.type.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.gatherentity.input.type.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.gatherentity.input.world.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.gatherentity.input.world.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.gatherentity.input.world.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.gatherentity.input.position.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.gatherentity.input.position.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.gatherentity.input.position.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.gatherentity.input.radius.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.gatherentity.input.radius.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.gatherentity.input.radius.type"));
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.gatherentity.output.entities.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.gatherentity.output.entities.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.gatherentity.output.entities.type"));
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        String uuidStr = TypeAdaptor.parse(resolvedInputs.get(0)).asString().strip();
        Identifier typeFilter = parseIdentifier(resolvedInputs.get(1));
        ServerWorld worldFilter = parseWorld(resolvedInputs.get(2));
        Vec3d positionFilter = TypeAdaptor.parse(resolvedInputs.get(3)).asVec3d();
        Double radiusFilter = TypeAdaptor.parse(resolvedInputs.get(4)).asDouble();

        if (positionFilter == null && radiusFilter != null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(3)), null);
        }
        if (positionFilter != null && radiusFilter == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(4)), null);
        }

        if (radiusFilter != null) {
            if (Double.isNaN(radiusFilter) || radiusFilter < 0) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.gatherentity.error.math", radiusFilter), null);
            }
        }

        List<Entity> resultEntities = new ArrayList<>();
        if (!uuidStr.isEmpty()) {
            // UUID search takes precedence
            try {
                UUID uuid = UUID.fromString(uuidStr);
                for (ServerWorld world : context.getServer().getWorlds()) {
                    Entity entity = world.getEntity(uuid);
                    if (entity != null) {
                        resultEntities.add(entity);
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.gatherentity.error.uuid", uuidStr), null);
            }
        } else {
            // World filter
            Iterable<ServerWorld> worldsToSearch;
            if (worldFilter != null) {
                List<ServerWorld> singleWorld = new ArrayList<>();
                singleWorld.add(worldFilter);
                worldsToSearch = singleWorld;
            } else {
                worldsToSearch = context.getServer().getWorlds();
            }

            for (ServerWorld world : worldsToSearch) {
                List<Entity> entities = Util.getAllEntities(world);
                
                for (Entity entity : entities) {
                    // Type filter
                    if (typeFilter != null) {
                        Identifier entityType = EntityType.getId(entity.getType());
                        if (entityType == null || !entityType.equals(typeFilter)) {
                            continue;
                        }
                    }

                    // Position and radius filter
                    if (positionFilter != null && radiusFilter != null) {
                        double distance = GameMath.getEuclideanDistance(entity.getPos(), positionFilter);
                        if (distance > radiusFilter) {
                            continue;   
                        }
                    }

                    resultEntities.add(entity);
                }
            }
        }

        if (resultEntities.isEmpty()) {
            status.setOutput(0, null);
        } else if (resultEntities.size() == 1) {
            status.setOutput(0, resultEntities.get(0));
        } else {
            status.setOutput(0, resultEntities);
        }
    }
    
    private Identifier parseIdentifier(Object obj) throws LogicException {
        if (obj == null) {
            return null;
        } else if (obj instanceof Identifier) {
            return (Identifier) obj;
        } else {
            String str = TypeAdaptor.parse(obj).asString().strip();
            if (str.isEmpty()) {
                return null;
            }
            try {
                return new Identifier(str);
            } catch (Exception e) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(1), this.metadata.inputDataTypes.get(1)), null);
            }
        }
    }

    private ServerWorld parseWorld(Object obj) throws LogicException {
        if (obj == null) {
            return null;
        } else if (obj instanceof ServerWorld) {
            return (ServerWorld) obj;
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(2), this.metadata.inputDataTypes.get(2)), null);
        }
    }
}
