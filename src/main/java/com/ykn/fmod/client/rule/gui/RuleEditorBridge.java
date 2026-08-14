/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.rule.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.CustomRule;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.core.RuleEvent;
import com.ykn.fmod.server.rule.tool.RuleManager;
import com.ykn.fmod.server.rule.tool.RuleRegistry;
import com.ykn.fmod.server.rule.tool.RuleSerializer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Thread-safe bridge between the client-side rule editor GUI and the live {@link RuleManager}/
 * {@link ServerData} state of the local integrated server.
 *
 * <p>Mirrors {@link com.ykn.fmod.client.flow.gui.FlowEditorBridge}'s detached-deep-copy editing
 * pattern: {@link #beginEdit} hops onto the server thread once to deep-copy the live
 * {@link CustomRule} ({@link CustomRule#copy()}) and hands the resulting orphan {@link RuleManager}
 * back to the client. That copy is registered nowhere and touched by no other thread, so the GUI
 * is free to mutate it directly and synchronously on the client thread for the rest of the editing
 * session - no further hops needed for individual edits.
 *
 * <p>The live rule registered in {@link ServerData} is only touched again when the user explicitly
 * commits: {@link #commitEdit} hops back to the server thread to replace the registered rule with a
 * (re-copied, so still never shared across threads) snapshot of the edited copy, disabling it in the
 * process. Closing the editor without committing leaves the live rule completely untouched.</p>
 *
 * <p>No packets, no registered channels - see {@link com.ykn.fmod.client.flow.gui.FlowEditorBridge}
 * for the full rationale, which applies identically here.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class RuleEditorBridge {

    /**
     * Whether the rule editor GUI can be used right now, i.e. the player is in a singleplayer
     * (or LAN-hosted) world with a local integrated server.
     */
    public static boolean isAvailable() {
        return Minecraft.getInstance().hasSingleplayerServer();
    }

    private static IntegratedServer requireServer() {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            throw new IllegalStateException("The rule editor is only available in singleplayer.");
        }
        return server;
    }

    /**
     * Runs {@code action} on the integrated server's own thread and completes the returned future
     * with its result once finished. {@code action} must not leak a live, still-registered
     * {@link RuleManager}/{@link CustomRule} reference back to the caller - only detached copies or
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
                Util.LOGGER.error("FMinecraftMod: Rule editor bridge action failed", t);
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
                Util.LOGGER.error("FMinecraftMod: Rule editor operation failed", error);
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
                Util.LOGGER.error("FMinecraftMod: Rule editor operation failed", error);
                onError.accept(error);
            }
        }, Minecraft.getInstance());
    }

    // ------------------------------------------------------------------
    // Plain data / result types
    // ------------------------------------------------------------------

    /**
     * One row of the rule-list screen.
     */
    public static final class RuleListEntry {
        public final String name;
        public final boolean enabled;
        public final String eventType;
        public final int extraCount;
        public final int satisfiedCount;
        public final int violatedCount;

        RuleListEntry(RuleManager manager) {
            CustomRule rule = manager.getRule();
            this.name = rule.getName();
            this.enabled = manager.isEnabled();
            this.eventType = rule.getEvent().getType();
            this.extraCount = rule.getExtra().size();
            this.satisfiedCount = rule.getActionIfSatisfied().size();
            this.violatedCount = rule.getActionIfViolated().size();
        }
    }

    /**
     * Describes one registered condition or action type for the "add" palette.
     */
    public static final class ComponentTypeInfo {
        public final String type;
        public final RequiredParamMetadata metadata;

        ComponentTypeInfo(String type, RequiredParamMetadata metadata) {
            this.type = type;
            this.metadata = metadata;
        }
    }

    /**
     * Describes one registered event type for the event picker.
     */
    public static final class EventTypeInfo {
        public final String type;
        public final Map<String, Class<?>> variablesType;
        public final Set<String> requiredVariables;
        public final Component render;

        EventTypeInfo(RuleEvent event) {
            this.type = event.getType();
            this.variablesType = new HashMap<>(event.variablesType());
            this.requiredVariables = new HashSet<>(event.variablesList());
            this.render = event.render();
        }
    }

    /**
     * Result of a rule-list-level operation (create/rename/delete/load/commit).
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
     * An in-progress edit of one rule: {@code originalName} identifies the live rule this session
     * was started from, and {@code manager} is a fully detached, client-thread-owned copy that the
     * GUI can freely mutate until it is discarded or committed back with {@link #commitEdit}.
     */
    public static final class EditSession {
        public final String originalName;
        public final RuleManager manager;

        EditSession(String originalName, RuleManager manager) {
            this.originalName = originalName;
            this.manager = manager;
        }
    }

    /**
     * A single online player, for {@code ENTITY}/{@code PLAYER}/{@code PLAYERS} widget pickers.
     */
    public static final class PlayerInfo {
        public final String name;
        public final UUID uuid;

        PlayerInfo(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }

    // ------------------------------------------------------------------
    // Reads that don't touch ServerData - safe to call directly on any thread
    // ------------------------------------------------------------------

    /**
     * All registered event types with their variable schema, for the event picker.
     */
    public static List<EventTypeInfo> eventTypeCatalog() {
        List<EventTypeInfo> list = new ArrayList<>();
        for (String type : RuleRegistry.getRegisteredEventNames()) {
            RuleEvent event = RuleRegistry.createRuleEvent(type);
            if (event != null) {
                list.add(new EventTypeInfo(event));
            }
        }
        list.sort(Comparator.comparing(t -> t.type));
        return list;
    }

    /**
     * All registered condition types (excluding composite/reference/const core types, which are
     * not directly addable through the "add condition" palette) with their parameter metadata.
     */
    public static List<ComponentTypeInfo> conditionTypeCatalog() {
        List<ComponentTypeInfo> list = new ArrayList<>();
        for (String type : RuleRegistry.getRegisteredConditionNames()) {
            RequiredParamMetadata metadata = RuleRegistry.getRequiredParamMetadata(type);
            if (metadata != null) {
                list.add(new ComponentTypeInfo(type, metadata));
            }
        }
        list.sort(Comparator.comparing(t -> t.type));
        return list;
    }

    /**
     * All registered action types with their parameter metadata, for the "add action" palette.
     */
    public static List<ComponentTypeInfo> actionTypeCatalog() {
        List<ComponentTypeInfo> list = new ArrayList<>();
        for (String type : RuleRegistry.getRegisteredActionNames()) {
            RequiredParamMetadata metadata = RuleRegistry.getRequiredParamMetadata(type);
            if (metadata != null) {
                list.add(new ComponentTypeInfo(type, metadata));
            }
        }
        list.sort(Comparator.comparing(t -> t.type));
        return list;
    }

    /**
     * Lists {@code .rule} files in the mod's config directory. Pure file I/O, no ServerData involved.
     */
    public static List<String> listRuleFiles() {
        List<String> result = new ArrayList<>();
        Path dir = Util.getConfigDir();
        try {
            if (Files.exists(dir)) {
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.filter(p -> p.toString().endsWith(".rule"))
                        .map(p -> p.getFileName().toString())
                        .forEach(result::add);
                }
            }
        } catch (IOException e) {
            Util.LOGGER.error("FMinecraftMod: Rule editor failed to list .rule files", e);
        }
        Collections.sort(result);
        return result;
    }

    // ------------------------------------------------------------------
    // Reads/writes of ServerData.getCustomRules() - must run on the server thread
    // ------------------------------------------------------------------

    public static CompletableFuture<List<RuleListEntry>> listRules() {
        return query(server -> {
            List<RuleListEntry> list = new ArrayList<>();
            for (RuleManager manager : Util.getServerData(server).getCustomRules().values()) {
                list.add(new RuleListEntry(manager));
            }
            list.sort(Comparator.comparing(e -> e.name));
            return list;
        });
    }

    public static CompletableFuture<OpResult> createRule(String name, String eventType) {
        return query(server -> {
            if (name == null || name.isEmpty()) {
                return OpResult.fail(Component.translatable("fmod.command.rule.empty").withStyle(ChatFormatting.RED));
            }
            ServerData data = Util.getServerData(server);
            if (data.getCustomRules().containsKey(name)) {
                return OpResult.fail(Component.translatable("fmod.command.rule.exists", name).withStyle(ChatFormatting.RED));
            }
            if (RuleRegistry.createRuleEvent(eventType) == null) {
                return OpResult.fail(Component.translatable("fmod.command.rule.event.unknown", eventType).withStyle(ChatFormatting.RED));
            }
            RuleManager manager = new RuleManager(name, eventType);
            data.getCustomRules().put(name, manager);
            return OpResult.ok(Component.translatable("fmod.command.rule.create.success", eventType, name).withStyle(ChatFormatting.GREEN));
        });
    }

    public static CompletableFuture<OpResult> setRuleEnabled(String name, boolean enabled) {
        return query(server -> {
            RuleManager manager = Util.getServerData(server).getCustomRules().get(name);
            if (manager == null) {
                return OpResult.fail(Component.translatable("fmod.command.rule.notexists", name).withStyle(ChatFormatting.RED));
            }
            manager.setEnabled(enabled);
            return OpResult.ok(Component.translatable(enabled ? "fmod.command.rule.enable.set.true" : "fmod.command.rule.enable.set.false", name).withStyle(ChatFormatting.YELLOW));
        });
    }

    public static CompletableFuture<OpResult> deleteRule(String name) {
        return query(server -> {
            ServerData data = Util.getServerData(server);
            if (!data.getCustomRules().containsKey(name)) {
                return OpResult.fail(Component.translatable("fmod.command.rule.notexists", name).withStyle(ChatFormatting.RED));
            }
            data.getCustomRules().remove(name);
            return OpResult.ok(Component.translatable("fmod.command.rule.delete.success", name).withStyle(ChatFormatting.GREEN));
        });
    }

    public static CompletableFuture<OpResult> loadRuleFile(String fileName) {
        return query(server -> {
            Path ruleFolder = Util.getConfigDir();
            Path rulePath = ruleFolder.resolve(fileName).normalize();
            if (!rulePath.startsWith(ruleFolder)) {
                return OpResult.fail(Component.translatable("fmod.command.rule.load.filenotfound", fileName).withStyle(ChatFormatting.RED));
            }
            CustomRule rule = RuleSerializer.loadFile(rulePath);
            if (rule == null) {
                return OpResult.fail(Component.translatable("fmod.command.rule.load.ioexception", fileName).withStyle(ChatFormatting.RED));
            }
            ServerData data = Util.getServerData(server);
            if (data.getCustomRules().containsKey(rule.getName())) {
                return OpResult.fail(Component.translatable("fmod.command.rule.exists", rule.getName()).withStyle(ChatFormatting.RED));
            }
            RuleManager manager = new RuleManager(rule);
            data.getCustomRules().put(rule.getName(), manager);
            manager.setEnabled(true);
            return OpResult.ok(Component.translatable("fmod.command.rule.load.success", rule.getName()).withStyle(ChatFormatting.GREEN));
        });
    }

    /**
     * Deep-copies the live rule {@code ruleName} and hands back an orphan {@link RuleManager}
     * wrapping the copy, ready for the client thread to edit freely. Fails if the rule no longer
     * exists (e.g. deleted concurrently).
     */
    public static CompletableFuture<EditSession> beginEdit(String ruleName) {
        return query(server -> {
            RuleManager live = Util.getServerData(server).getCustomRules().get(ruleName);
            if (live == null) {
                throw new NoSuchElementException(ruleName);
            }
            CustomRule copy = live.getRule().copy();
            return new EditSession(ruleName, new RuleManager(copy));
        });
    }

    /**
     * Commits an edited rule copy back to {@link ServerData}, replacing whatever is currently
     * registered under {@code originalName} (or under the copy's own name, if it was renamed
     * since the session started) and disabling it. Optionally also persists it to disk.
     *
     * @param editedRuleCopy a copy of the edited rule, freshly taken on the client thread right
     *                       before calling this method so the caller's own working copy is never
     *                       shared with the server thread
     */
    public static CompletableFuture<OpResult> commitEdit(String originalName, CustomRule editedRuleCopy, boolean saveToDisk) {
        return query(server -> {
            ServerData data = Util.getServerData(server);
            RuleManager newManager = new RuleManager(editedRuleCopy);
            newManager.setEnabled(false);
            data.getCustomRules().remove(originalName);
            data.getCustomRules().put(editedRuleCopy.getName(), newManager);
            if (!saveToDisk) {
                return OpResult.ok(Component.translatable("fmod.rulegui.editor.update.success", editedRuleCopy.getName()).withStyle(ChatFormatting.GREEN));
            }
            Path ruleFolder = Util.getConfigDir();
            Path rulePath = ruleFolder.resolve(editedRuleCopy.getName() + ".rule").normalize();
            if (!rulePath.startsWith(ruleFolder)) {
                return OpResult.fail(Component.translatable("fmod.command.rule.save.notavailable", editedRuleCopy.getName()).withStyle(ChatFormatting.RED));
            }
            boolean ok = RuleSerializer.saveFile(editedRuleCopy, rulePath, true);
            if (!ok) {
                return OpResult.fail(Component.translatable("fmod.command.rule.save.ioexception", editedRuleCopy.getName()).withStyle(ChatFormatting.RED));
            }
            return OpResult.ok(Component.translatable("fmod.rulegui.editor.save.success", editedRuleCopy.getName()).withStyle(ChatFormatting.GREEN));
        });
    }

    /**
     * Currently-online players, for {@code ENTITY}/{@code PLAYER}/{@code PLAYERS} widget pickers.
     */
    public static CompletableFuture<List<PlayerInfo>> onlinePlayers() {
        return query(server -> {
            List<PlayerInfo> list = new ArrayList<>();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                list.add(new PlayerInfo(player.getGameProfile().getName(), player.getUUID()));
            }
            list.sort(Comparator.comparing(p -> p.name));
            return list;
        });
    }

    /**
     * All currently-loaded dimension identifiers, for the {@code DIMENSION} widget picker.
     */
    public static CompletableFuture<List<ResourceLocation>> dimensionCatalog() {
        return query(server -> {
            List<ResourceLocation> list = new ArrayList<>();
            for (ServerLevel level : server.getAllLevels()) {
                list.add(level.dimension().location());
            }
            list.sort(Comparator.comparing(ResourceLocation::toString));
            return list;
        });
    }
}
