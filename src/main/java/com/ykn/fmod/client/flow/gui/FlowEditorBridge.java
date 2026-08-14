/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.flow.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicFlow;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.flow.tool.FlowSerializer;
import com.ykn.fmod.server.flow.tool.NodeRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Thread-safe bridge between the client-side flow editor GUI and the live {@link FlowManager}/
 * {@link ServerData} state of the local integrated server.
 *
 * <p>Editing works on a <b>detached deep copy</b>: {@link #beginEdit} hops onto the server thread
 * once to deep-copy the live {@link LogicFlow} ({@link LogicFlow#copy()}) and hands the resulting
 * orphan {@link FlowManager} back to the client. That copy is registered nowhere and touched by no
 * other thread, so the GUI is free to mutate it directly and synchronously on the client thread for
 * the rest of the editing session - no further hops needed for individual node edits, and undo/redo
 * work entirely locally via the copy's own {@link FlowManager} undo/redo stacks.
 *
 * <p>The live flow registered in {@link ServerData} is only touched again when the user explicitly
 * commits: {@link #commitEdit} hops back to the server thread to replace the registered flow with a
 * (re-copied, so still never shared across threads) snapshot of the edited copy, disabling it in the
 * process. Closing the editor without committing leaves the live flow completely untouched.</p>
 *
 * <p>No packets, no registered channels: a remote dedicated server never exposes an
 * {@link IntegratedServer} instance to any client, so there is no code path here that could ever
 * run against a remote server.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class FlowEditorBridge {

    /**
     * Whether the flow editor GUI can be used right now, i.e. the player is in a singleplayer
     * (or LAN-hosted) world with a local integrated server.
     */
    public static boolean isAvailable() {
        return Minecraft.getInstance().hasSingleplayerServer();
    }

    private static IntegratedServer requireServer() {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            throw new IllegalStateException("The flow editor is only available in singleplayer.");
        }
        return server;
    }

    /**
     * Runs {@code action} on the integrated server's own thread and completes the returned future
     * with its result once finished. {@code action} must not leak a live, still-registered
     * {@link FlowNode}/{@link LogicFlow} reference back to the caller - only detached copies or
     * plain immutable data.
     */
    private static <T> CompletableFuture<T> query(Function<MinecraftServer, T> action) {
        CompletableFuture<T> future = new CompletableFuture<>();
        IntegratedServer server;
        try {
            server = requireServer();
        } catch (IllegalStateException e) {
            future.completeExceptionally(e);
            return future;
        }
        server.execute(() -> {
            try {
                future.complete(action.apply(server));
            } catch (Throwable t) {
                Util.LOGGER.error("FMinecraftMod: Flow editor bridge action failed", t);
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /** 
     * Consumes {@code future} back on the client (render) thread once it completes. Failures are logged and ignored. 
     */
    public static <T> void onClient(CompletableFuture<T> future, Consumer<T> onSuccess) {
        future.whenCompleteAsync((result, error) -> {
            if (error == null) {
                onSuccess.accept(result);
            } else {
                Util.LOGGER.error("FMinecraftMod: Flow editor operation failed", error);
            }
        }, Minecraft.getInstance());
    }

    /** 
     * Like {@link #onClient} but also routes failures to {@code onError} (still on the client thread). 
     */
    public static <T> void onClientResult(CompletableFuture<T> future, Consumer<T> onResult, Consumer<Throwable> onError) {
        future.whenCompleteAsync((result, error) -> {
            if (error == null) {
                onResult.accept(result);
            } else {
                Util.LOGGER.error("FMinecraftMod: Flow editor operation failed", error);
                onError.accept(error);
            }
        }, Minecraft.getInstance());
    }

    // ------------------------------------------------------------------
    // Plain data / result types
    // ------------------------------------------------------------------

    /** 
     * One row of the flow-list screen. 
     */
    public static final class FlowListEntry {
        public final String name;
        public final boolean enabled;
        public final int nodeCount;
        public final String firstNodeName;
        public final boolean corrupted;

        FlowListEntry(FlowManager manager) {
            this.name = manager.getFlow().getName();
            this.enabled = manager.isEnabled();
            this.nodeCount = manager.getFlow().getNodes().size();
            FlowNode first = manager.getFlow().getFirstNode();
            this.firstNodeName = first == null ? null : first.getName();
            this.corrupted = first == null;
        }
    }

    /** 
     * Describes one registered node type for the "add node" palette. 
     */
    public static final class NodeTypeInfo {
        public final String type;
        public final boolean event;
        public final NodeMetadata metadata;

        NodeTypeInfo(String type, boolean event, NodeMetadata metadata) {
            this.type = type;
            this.event = event;
            this.metadata = metadata;
        }
    }

    /**
     * Result of a flow-list-level operation (create/copy/rename/delete/load/commit). 
     */
    public static final class OpResult {
        public final boolean success;
        public final Component message;

        private OpResult(boolean success, Component message) {
            this.success = success;
            this.message = message;
        }

        static OpResult ok(Component message) {
            return new OpResult(true, message);
        }

        static OpResult fail(Component message) {
            return new OpResult(false, message);
        }
    }

    /**
     * An in-progress edit of one flow: {@code originalName} identifies the live flow this session
     * was started from, and {@code manager} is a fully detached, client-thread-owned copy that the
     * GUI can freely mutate (including its own independent undo/redo history) until it is discarded
     * or committed back with {@link #commitEdit}.
     */
    public static final class EditSession {
        public final String originalName;
        public final FlowManager manager;

        EditSession(String originalName, FlowManager manager) {
            this.originalName = originalName;
            this.manager = manager;
        }
    }

    // ------------------------------------------------------------------
    // Reads that don't touch ServerData - safe to call directly on any thread
    // ------------------------------------------------------------------

    /** 
     * All registered node types with their metadata, for the "add node" palette. 
     */
    public static List<NodeTypeInfo> nodeTypeCatalog() {
        List<NodeTypeInfo> list = new ArrayList<>();
        for (String type : NodeRegistry.getNodeList()) {
            FlowNode temp = NodeRegistry.createNode(type, 0, type);
            if (temp != null) {
                list.add(new NodeTypeInfo(type, false, temp.getMetadata()));
            }
        }
        for (String type : NodeRegistry.getEventNodeList()) {
            FlowNode temp = NodeRegistry.createNode(type, 0, type);
            if (temp != null) {
                list.add(new NodeTypeInfo(type, true, temp.getMetadata()));
            }
        }
        list.sort(Comparator.comparing(t -> t.type));
        return list;
    }

    /** 
     * Lists `.flow` files in the mod's config directory. Pure file I/O, no ServerData involved. 
     */
    public static List<String> listFlowFiles() {
        List<String> result = new ArrayList<>();
        Path dir = Util.getConfigDir();
        try {
            if (Files.exists(dir)) {
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.filter(p -> p.toString().endsWith(".flow"))
                        .map(p -> p.getFileName().toString())
                        .forEach(result::add);
                }
            }
        } catch (IOException e) {
            Util.LOGGER.error("FMinecraftMod: Flow editor failed to list .flow files", e);
        }
        Collections.sort(result);
        return result;
    }

    // ------------------------------------------------------------------
    // Reads/writes of ServerData.getLogicFlows() - must run on the server thread
    // ------------------------------------------------------------------

    public static CompletableFuture<List<FlowListEntry>> listFlows() {
        return query(server -> {
            List<FlowListEntry> list = new ArrayList<>();
            for (FlowManager manager : Util.getServerData(server).getLogicFlows().values()) {
                list.add(new FlowListEntry(manager));
            }
            list.sort(Comparator.comparing(e -> e.name));
            return list;
        });
    }

    public static CompletableFuture<OpResult> createFlow(String name, String eventType, String eventNodeName) {
        return query(server -> {
            if (name == null || name.isEmpty()) {
                return OpResult.fail(Component.translatable("fmod.command.flow.empty").withStyle(ChatFormatting.RED));
            }
            ServerData data = Util.getServerData(server);
            if (data.getLogicFlows().containsKey(name)) {
                return OpResult.fail(Component.translatable("fmod.command.flow.exists", name).withStyle(ChatFormatting.RED));
            }
            if (!NodeRegistry.getEventNodeList().contains(eventType)) {
                return OpResult.fail(Component.translatable("fmod.command.flow.event.unknown", eventType).withStyle(ChatFormatting.RED));
            }
            FlowManager manager = new FlowManager(name, eventType, eventNodeName);
            data.getLogicFlows().put(name, manager);
            return OpResult.ok(Component.translatable("fmod.command.flow.create.success", eventType, name).withStyle(ChatFormatting.GREEN));
        });
    }

    public static CompletableFuture<OpResult> setFlowEnabled(String name, boolean enabled) {
        return query(server -> {
            FlowManager manager = Util.getServerData(server).getLogicFlows().get(name);
            if (manager == null) {
                return OpResult.fail(Component.translatable("fmod.command.flow.notexists", name).withStyle(ChatFormatting.RED));
            }
            manager.setEnabled(enabled);
            return OpResult.ok(Component.translatable(enabled ? "fmod.command.flow.enable.set.true" : "fmod.command.flow.enable.set.false", name).withStyle(ChatFormatting.YELLOW));
        });
    }

    public static CompletableFuture<OpResult> deleteFlow(String name) {
        return query(server -> {
            ServerData data = Util.getServerData(server);
            if (!data.getLogicFlows().containsKey(name)) {
                return OpResult.fail(Component.translatable("fmod.command.flow.notexists", name).withStyle(ChatFormatting.RED));
            }
            data.getLogicFlows().remove(name);
            return OpResult.ok(Component.translatable("fmod.command.flow.delete.success", name).withStyle(ChatFormatting.GREEN));
        });
    }

    public static CompletableFuture<OpResult> loadFlowFile(String fileName) {
        return query(server -> {
            Path flowFolder = Util.getConfigDir();
            Path flowPath = flowFolder.resolve(fileName).normalize();
            if (!flowPath.startsWith(flowFolder)) {
                return OpResult.fail(Component.translatable("fmod.command.flow.load.filenotfound", fileName).withStyle(ChatFormatting.RED));
            }
            LogicFlow flow = FlowSerializer.loadFile(flowPath);
            if (flow == null) {
                return OpResult.fail(Component.translatable("fmod.command.flow.load.ioexception", fileName).withStyle(ChatFormatting.RED));
            }
            ServerData data = Util.getServerData(server);
            if (data.getLogicFlows().containsKey(flow.getName())) {
                return OpResult.fail(Component.translatable("fmod.command.flow.exists", flow.getName()).withStyle(ChatFormatting.RED));
            }
            FlowManager manager = new FlowManager(flow);
            data.getLogicFlows().put(flow.getName(), manager);
            manager.setEnabled(true);
            return OpResult.ok(Component.translatable("fmod.command.flow.load.success", flow.getName()).withStyle(ChatFormatting.GREEN));
        });
    }

    /**
     * Deep-copies the live flow {@code flowName} and hands back an orphan {@link FlowManager}
     * wrapping the copy, ready for the client thread to edit freely. Fails if the flow no longer
     * exists (e.g. deleted concurrently).
     */
    public static CompletableFuture<EditSession> beginEdit(String flowName) {
        return query(server -> {
            FlowManager live = Util.getServerData(server).getLogicFlows().get(flowName);
            if (live == null) {
                throw new NoSuchElementException(flowName);
            }
            LogicFlow copy = live.getFlow().copy();
            return new EditSession(flowName, new FlowManager(copy));
        });
    }

    /**
     * Commits an edited flow copy back to {@link ServerData}, replacing whatever is currently
     * registered under {@code originalName} (or under the copy's own name, if it was renamed
     * since the session started) and disabling it. Optionally also persists it to disk.
     *
     * <p>This always wraps {@code editedFlowCopy} in a brand-new {@link FlowManager}, so any
     * undo/redo history the previously-registered flow had (whether from earlier {@code /f flow
     * edit ... undo/redo} command usage or from this same GUI session) is discarded, not merged.
     * {@link com.ykn.fmod.server.flow.tool.NodeEditPath} entries are closures over specific
     * {@link FlowNode}/{@link com.ykn.fmod.server.flow.logic.DataReference} object instances tied
     * to one particular {@link LogicFlow} object graph, so they cannot be safely replayed against a
     * different (even if structurally identical) copy. This matches the existing behavior of
     * {@code /f flow load} and {@code /f flow copy}, which already reset history the same way.</p>
     *
     * @param editedFlowCopy a copy of the edited flow, freshly taken on the client thread right
     *                       before calling this method so the caller's own working copy is never
     *                       shared with the server thread
     */
    public static CompletableFuture<OpResult> commitEdit(String originalName, LogicFlow editedFlowCopy, boolean saveToDisk) {
        return query(server -> {
            ServerData data = Util.getServerData(server);
            FlowManager newManager = new FlowManager(editedFlowCopy);
            newManager.setEnabled(false);
            data.getLogicFlows().remove(originalName);
            data.getLogicFlows().put(editedFlowCopy.getName(), newManager);
            if (!saveToDisk) {
                return OpResult.ok(Component.translatable("fmod.flowgui.graph.update.success", editedFlowCopy.getName()).withStyle(ChatFormatting.GREEN));
            }
            Path flowFolder = Util.getConfigDir();
            Path flowPath = flowFolder.resolve(editedFlowCopy.getName() + ".flow").normalize();
            if (!flowPath.startsWith(flowFolder)) {
                return OpResult.fail(Component.translatable("fmod.command.flow.save.notavailable", editedFlowCopy.getName()).withStyle(ChatFormatting.RED));
            }
            boolean ok = FlowSerializer.saveFile(editedFlowCopy, flowPath, true);
            if (!ok) {
                return OpResult.fail(Component.translatable("fmod.command.flow.save.ioexception", editedFlowCopy.getName()).withStyle(ChatFormatting.RED));
            }
            return OpResult.ok(Component.translatable("fmod.flowgui.graph.save.success", editedFlowCopy.getName()).withStyle(ChatFormatting.GREEN));
        });
    }
}
