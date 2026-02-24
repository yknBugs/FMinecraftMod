/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;

/**
 * Metadata about a node type.
 * Users are not allowed to modify the metadata.
 * The data like {@code name} and {@code inputs} can be modified by users, so they should be in the {@link FlowNode} class, not here.
 */
public class NodeMetadata {

    /**
     * The number of inputs this node has.
     */
    public final int inputNumber;

    /**
     * The number of outputs this node has.
     */
    public final int outputNumber;

    /**
     * The branch number of this node.
     * Usually 1 for most nodes. 2 for simple if nodes (true/false).
     */
    public final int branchNumber;

    /**
     * The official name of this node type.
     */
    public final Component displayName;

    /**
     * The description about the functionality of this node type.
     */
    public final Component description;

    /**
     * The official name of the inputs
     */
    public final List<Component> inputNames;

    /**
     * The description about the inputs
     */
    public final List<Component> inputDescriptions;

    /**
     * The data types of the inputs
     */
    public final List<Component> inputDataTypes;

    /**
     * The official name of the outputs
     */
    public final List<Component> outputNames;

    /**
     * The description about the outputs
     */
    public final List<Component> outputDescriptions;

    /**
     * The data types of the outputs
     */
    public final List<Component> outputDataTypes;

    /**
     * The official name of the branches
     */
    public final List<Component> branchNames;
    
    /**
     * The description about the branches
     */
    public final List<Component> branchDescriptions;

    public NodeMetadata(int inputNumber, int outputNumber, int branchNumber,
                        Component displayName, Component description,
                        List<Component> inputNames, List<Component> inputDescriptions, List<Component> inputDataTypes,
                        List<Component> outputNames, List<Component> outputDescriptions, List<Component> outputDataTypes,
                        List<Component> branchNames, List<Component> branchDescriptions) {
        this.inputNumber = inputNumber;
        this.outputNumber = outputNumber;
        this.branchNumber = branchNumber;
        this.displayName = displayName;
        this.description = description;
        this.inputNames = Collections.unmodifiableList(inputNames);
        this.inputDescriptions = Collections.unmodifiableList(inputDescriptions);
        this.inputDataTypes = Collections.unmodifiableList(inputDataTypes);
        this.outputNames = Collections.unmodifiableList(outputNames);
        this.outputDescriptions = Collections.unmodifiableList(outputDescriptions);
        this.outputDataTypes = Collections.unmodifiableList(outputDataTypes);
        this.branchNames = Collections.unmodifiableList(branchNames);
        this.branchDescriptions = Collections.unmodifiableList(branchDescriptions);
        verifyMetadataIntegrity();
    }

    public boolean verifyMetadataIntegrity() {
        boolean valid = true;
        if (this.inputNames.size() != this.inputNumber) {
            Util.LOGGER.warn("FMinecraftMod: NodeMetadata integrity check failed: inputNames size = " + this.inputNames.size() + ", expected " + this.inputNumber);
            valid = false;
        }
        if (this.inputDescriptions.size() != this.inputNumber) {
            Util.LOGGER.warn("FMinecraftMod: NodeMetadata integrity check failed: inputDescriptions size = " + this.inputDescriptions.size() + ", expected " + this.inputNumber);
            valid = false;
        }
        if (this.inputDataTypes.size() != this.inputNumber) {
            Util.LOGGER.warn("FMinecraftMod: NodeMetadata integrity check failed: inputDataTypes size = " + this.inputDataTypes.size() + ", expected " + this.inputNumber);
            valid = false;
        }
        if (this.outputNames.size() != this.outputNumber) {
            Util.LOGGER.warn("FMinecraftMod: NodeMetadata integrity check failed: outputNames size = " + this.outputNames.size() + ", expected " + this.outputNumber);
            valid = false;
        }
        if (this.outputDescriptions.size() != this.outputNumber) {
            Util.LOGGER.warn("FMinecraftMod: NodeMetadata integrity check failed: outputDescriptions size = " + this.outputDescriptions.size() + ", expected " + this.outputNumber);
            valid = false;
        }
        if (this.outputDataTypes.size() != this.outputNumber) {
            Util.LOGGER.warn("FMinecraftMod: NodeMetadata integrity check failed: outputDataTypes size = " + this.outputDataTypes.size() + ", expected " + this.outputNumber);
            valid = false;
        }
        if (this.branchNames.size() != this.branchNumber) {
            Util.LOGGER.warn("FMinecraftMod: NodeMetadata integrity check failed: branchNames size = " + this.branchNames.size() + ", expected " + this.branchNumber);
            valid = false;
        }
        if (this.branchDescriptions.size() != this.branchNumber) {
            Util.LOGGER.warn("FMinecraftMod: NodeMetadata integrity check failed: branchDescriptions size = " + this.branchDescriptions.size() + ", expected " + this.branchNumber);
            valid = false;
        }
        return valid;
    }

    /**
     * Creates a fluent builder for {@link NodeMetadata}.
     * <p>Usage inside {@code createMetadata}:</p>
     * <pre>
     * return NodeMetadata.builder("fmod.node.foo.title.name", "fmod.node.foo.title.feat")
     *     .input("fmod.node.foo.input.bar.name",  "...feat", "...type")
     *     .output("fmod.node.foo.output.baz.name", "...feat", "...type")
     *     .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
     *     .build(inputNumber, outputNumber, branchNumber);
     * </pre>
     */
    public static Builder builder(String displayNameKey, String descriptionKey) {
        return new Builder(displayNameKey, descriptionKey);
    }

    /**
     * Fluent builder that accumulates translation-key triples for inputs, outputs, and branches,
     * then constructs a validated {@link NodeMetadata} via {@link #build}.
     */
    public static final class Builder {

        private final Component displayName;
        private final Component description;
        private final List<Component> inputNames         = new ArrayList<>();
        private final List<Component> inputDescriptions  = new ArrayList<>();
        private final List<Component> inputDataTypes     = new ArrayList<>();
        private final List<Component> outputNames        = new ArrayList<>();
        private final List<Component> outputDescriptions = new ArrayList<>();
        private final List<Component> outputDataTypes    = new ArrayList<>();
        private final List<Component> branchNames        = new ArrayList<>();
        private final List<Component> branchDescriptions = new ArrayList<>();

        private Builder(String displayNameKey, String descriptionKey) {
            this.displayName = Util.parseTranslatableText(displayNameKey);
            this.description = Util.parseTranslatableText(descriptionKey);
        }

        /**
         * Adds one input slot using its three translation keys.
         *
         * @param nameKey human-readable name key
         * @param descKey description key
         * @param typeKey data-type label key
         * @return {@code this} for chaining
         */
        public Builder input(String nameKey, String descKey, String typeKey) {
            inputNames.add(Util.parseTranslatableText(nameKey));
            inputDescriptions.add(Util.parseTranslatableText(descKey));
            inputDataTypes.add(Util.parseTranslatableText(typeKey));
            return this;
        }

        /**
         * Adds one output slot using its three translation keys.
         *
         * @param nameKey human-readable name key
         * @param descKey description key
         * @param typeKey data-type label key
         * @return {@code this} for chaining
         */
        public Builder output(String nameKey, String descKey, String typeKey) {
            outputNames.add(Util.parseTranslatableText(nameKey));
            outputDescriptions.add(Util.parseTranslatableText(descKey));
            outputDataTypes.add(Util.parseTranslatableText(typeKey));
            return this;
        }

        /**
         * Adds one execution branch using its two translation keys.
         *
         * @param nameKey human-readable name key
         * @param descKey description key
         * @return {@code this} for chaining
         */
        public Builder branch(String nameKey, String descKey) {
            branchNames.add(Util.parseTranslatableText(nameKey));
            branchDescriptions.add(Util.parseTranslatableText(descKey));
            return this;
        }

        /**
         * Finalises the builder and returns a new {@link NodeMetadata}.
         * The counts supplied here should match the number of {@link #input}, {@link #output},
         * and {@link #branch} calls made on this builder — the metadata integrity check will
         * log a warning if they do not.
         *
         * @param inputNumber  expected number of inputs
         * @param outputNumber expected number of outputs
         * @param branchNumber expected number of branches
         * @return a fully constructed, immutable {@link NodeMetadata}
         */
        public NodeMetadata build(int inputNumber, int outputNumber, int branchNumber) {
            return new NodeMetadata(inputNumber, outputNumber, branchNumber,
                    displayName, description,
                    inputNames, inputDescriptions, inputDataTypes,
                    outputNames, outputDescriptions, outputDataTypes,
                    branchNames, branchDescriptions);
        }
    }
}
