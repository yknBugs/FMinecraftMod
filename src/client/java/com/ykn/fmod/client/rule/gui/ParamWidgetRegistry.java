/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.rule.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.ykn.fmod.client.base.gui.OptionPickerScreen;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleParameter;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side registry of {@link ParamKind} GUI editors, the direct counterpart of
 * {@link com.ykn.fmod.server.rule.tool.RuleRegistry} for the one thing the server can't drive
 * itself: building a widget for a parameter, since server code can't invoke client code.
 *
 * <p>Every built-in {@link ParamKind} is registered by {@link #registerDefault()}, called once
 * from {@link com.ykn.fmod.client.FMod#onInitializeClient()}. Third-party code that declares a
 * custom {@link ParamKind} on the server should register a matching widget builder here (in its
 * own client entrypoint) via {@link #registerByKind}/{@link #registerByValueType} - otherwise
 * {@link #isSupported} reports the kind as unsupported and the rule editor GUI refuses to open an
 * editor for any condition/action that uses it (falling back to command-line/JSON editing
 * instead), rather than guessing with a widget that doesn't actually understand the value.
 *
 * <p>Lookup has two tiers, mirroring the dispatch {@link ParamWidgetFactory} used to do inline:
 * <ul>
 *   <li>{@link #registerByKind} - for kinds that need a distinct editor despite sharing a
 *       {@link ParamKind#valueType()} with another kind (e.g. {@code DIMENSION} vs {@code BLOCK_ID},
 *       both {@link ResourceLocation}-valued).</li>
 *   <li>{@link #registerByValueType} - the fallback used by every kind that doesn't need its own
 *       entry, keyed by {@link ParamKind#valueType()}. This is also what makes
 *       {@link ParamKind#intAtLeast(int)}/{@link ParamKind#doubleAtLeast(double)} work without
 *       per-call registration: each call mints a distinct {@code ParamKind} instance, so an
 *       identity-keyed map alone would never match them.</li>
 * </ul>
 */
public final class ParamWidgetRegistry {

    /**
     * Builds the constant-value widget(s) for one parameter row, returning a supplier that reads
     * the current constant value back out. Same shape {@link ParamWidgetFactory} used to switch
     * on inline before this became a registry.
     */
    @FunctionalInterface
    public interface WidgetBuilder {
        Supplier<Object> build(ParamEditorScreen<?> owner, int index, RequiredParamMetadata.Entry entry,
                Object initialConstant, int x, int y, int width, Consumer<AbstractWidget> sink);
    }

    private static final Map<ParamKind<?>, WidgetBuilder> byKind = new IdentityHashMap<>();

    private static final Map<Class<?>, WidgetBuilder> byValueType = new HashMap<>();

    /**
     * Registers a widget builder for one specific {@link ParamKind} instance (by identity),
     * overriding whatever {@link #registerByValueType} would otherwise supply for its
     * {@link ParamKind#valueType()}.
     *
     * @param kind    the exact kind instance to register a builder for
     * @param builder the widget builder
     */
    public static void registerByKind(ParamKind<?> kind, WidgetBuilder builder) {
        byKind.put(kind, builder);
    }

    /**
     * Registers a widget builder for every {@link ParamKind} whose {@link ParamKind#valueType()}
     * equals {@code valueType} and has no more specific {@link #registerByKind} entry.
     *
     * @param valueType the value type to register a builder for
     * @param builder   the widget builder
     */
    public static void registerByValueType(Class<?> valueType, WidgetBuilder builder) {
        byValueType.put(valueType, builder);
    }

    /**
     * Whether a GUI widget can be built for {@code kind}: either a specific {@link #registerByKind}
     * entry, or a {@link #registerByValueType} entry for its {@link ParamKind#valueType()}.
     *
     * @param kind the kind to check
     * @return {@code true} if {@link #build} can be called for a parameter of this kind
     */
    public static boolean isSupported(ParamKind<?> kind) {
        return byKind.containsKey(kind) || byValueType.containsKey(kind.valueType());
    }

    /**
     * Whether every parameter declared by {@code metadata} is {@link #isSupported(ParamKind)}.
     *
     * @param metadata the component type's declared parameters
     * @return {@code true} if the rule editor GUI can build an editor for every parameter
     */
    public static boolean isSupported(RequiredParamMetadata metadata) {
        for (RequiredParamMetadata.Entry entry : metadata.getArgumentList()) {
            if (!isSupported(entry.kind)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds the constant-value widget(s) for one parameter row. Callers must have already
     * confirmed {@link #isSupported(ParamKind)} for {@code entry.kind}.
     *
     * @throws IllegalStateException if no builder is registered for {@code entry.kind}
     */
    public static Supplier<Object> build(ParamEditorScreen<?> owner, int index, RequiredParamMetadata.Entry entry,
            Object initialConstant, int x, int y, int width, Consumer<AbstractWidget> sink) {
        WidgetBuilder builder = byKind.get(entry.kind);
        if (builder == null) {
            builder = byValueType.get(entry.kind.valueType());
        }
        if (builder == null) {
            throw new IllegalStateException("No GUI widget registered for ParamKind with value type "
                + entry.kind.valueType() + " - callers must check isSupported() first.");
        }
        return builder.build(owner, index, entry, initialConstant, x, y, width, sink);
    }

    private static final int PICK_WIDTH = 50;
    private static final int GAP = 4;

    /** 
     * Registers every built-in {@link ParamKind}'s widget builder. 
     */
    public static void registerDefault() {
        registerByKind(ParamKind.DIMENSION, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width - PICK_WIDTH - GAP, initialConstant, "fmod.rulegui.param.hint.dimension");
            sink.accept(box);
            sink.accept(Button.builder(Component.translatable("fmod.rulegui.param.pick"), b -> {
                owner.captureWorkingValues();
                RuleEditorBridge.onClient(RuleEditorBridge.dimensionCatalog(), locations -> openResourceLocationPicker(owner, index, locations));
            }).pos(x + width - PICK_WIDTH, y).size(PICK_WIDTH, 18).build());
            return () -> ResourceLocation.tryParse(box.getValue().trim());
        });

        registerByKind(ParamKind.BLOCK_ID, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width - PICK_WIDTH - GAP, initialConstant, "fmod.rulegui.param.hint.block");
            sink.accept(box);
            sink.accept(Button.builder(Component.translatable("fmod.rulegui.param.pick"), b -> {
                owner.captureWorkingValues();
                openResourceLocationPicker(owner, index, BuiltInRegistries.BLOCK.keySet());
            }).pos(x + width - PICK_WIDTH, y).size(PICK_WIDTH, 18).build());
            return () -> ResourceLocation.tryParse(box.getValue().trim());
        });

        registerByKind(ParamKind.ENTITY_TYPE_ID, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width - PICK_WIDTH - GAP, initialConstant, "fmod.rulegui.param.hint.entitytype");
            sink.accept(box);
            sink.accept(Button.builder(Component.translatable("fmod.rulegui.param.pick"), b -> {
                owner.captureWorkingValues();
                openResourceLocationPicker(owner, index, BuiltInRegistries.ENTITY_TYPE.keySet());
            }).pos(x + width - PICK_WIDTH, y).size(PICK_WIDTH, 18).build());
            return () -> ResourceLocation.tryParse(box.getValue().trim());
        });

        registerByKind(ParamKind.VARIABLE_NAME, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, initialConstant, "fmod.rulegui.param.hint.varname");
            sink.accept(box);
            return () -> {
                String value = box.getValue().trim();
                return RuleCondition.NAME_PATTERN.matcher(value).matches() ? value : null;
            };
        });

        registerByKind(ParamKind.AUTO, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, initialConstant, "fmod.rulegui.param.hint.constant");
            sink.accept(box);
            return () -> box.getValue().isEmpty() ? null : TypeAdaptor.parse(box.getValue()).autoCast();
        });

        registerByValueType(UUID.class, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width - PICK_WIDTH - GAP, initialConstant, "fmod.rulegui.param.hint.player");
            sink.accept(box);
            sink.accept(Button.builder(Component.translatable("fmod.rulegui.param.pick"), b -> {
                owner.captureWorkingValues();
                RuleEditorBridge.onClient(RuleEditorBridge.onlinePlayers(), players -> openPlayerPicker(owner, index, players, false));
            }).pos(x + width - PICK_WIDTH, y).size(PICK_WIDTH, 18).build());
            return () -> {
                try {
                    return UUID.fromString(box.getValue().trim());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            };
        });

        registerByValueType(List.class, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width - PICK_WIDTH - GAP, initialConstant, "fmod.rulegui.param.hint.players");
            sink.accept(box);
            sink.accept(Button.builder(Component.translatable("fmod.rulegui.param.pick"), b -> {
                owner.captureWorkingValues();
                RuleEditorBridge.onClient(RuleEditorBridge.onlinePlayers(), players -> openPlayerPicker(owner, index, players, true));
            }).pos(x + width - PICK_WIDTH, y).size(PICK_WIDTH, 18).build());
            return () -> {
                if (box.getValue().isBlank()) {
                    return null;
                }
                List<UUID> uuids = new ArrayList<>();
                for (String token : box.getValue().split(",")) {
                    try {
                        uuids.add(UUID.fromString(token.trim()));
                    } catch (IllegalArgumentException ignored) {
                        // Skip malformed tokens; the row is still usable with the remaining valid UUIDs.
                    }
                }
                return uuids;
            };
        });

        registerByValueType(Vec3.class, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            int fieldWidth = (width - 8) / 3;
            EditBox xBox = numberBox(x, y, fieldWidth, initialConstant instanceof Vec3 v ? String.valueOf(v.x) : "", "fmod.rulegui.param.hint.x");
            EditBox yBox = numberBox(x + fieldWidth + 4, y, fieldWidth, initialConstant instanceof Vec3 v ? String.valueOf(v.y) : "", "fmod.rulegui.param.hint.y");
            EditBox zBox = numberBox(x + 2 * (fieldWidth + 4), y, fieldWidth, initialConstant instanceof Vec3 v ? String.valueOf(v.z) : "", "fmod.rulegui.param.hint.z");
            sink.accept(xBox);
            sink.accept(yBox);
            sink.accept(zBox);
            return () -> {
                try {
                    return new Vec3(Double.parseDouble(xBox.getValue().trim()), Double.parseDouble(yBox.getValue().trim()), Double.parseDouble(zBox.getValue().trim()));
                } catch (NumberFormatException e) {
                    return null;
                }
            };
        });

        registerByValueType(Boolean.class, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            CycleButton<Boolean> box = CycleButton.onOffBuilder(initialConstant instanceof Boolean b && b)
                .create(x, y, width, 18, Component.empty());
            sink.accept(box);
            return box::getValue;
        });

        registerByValueType(Integer.class, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, initialConstant, "fmod.rulegui.param.hint.constant");
            sink.accept(box);
            return () -> {
                try {
                    return Integer.parseInt(box.getValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            };
        });

        registerByValueType(Double.class, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, initialConstant, "fmod.rulegui.param.hint.constant");
            sink.accept(box);
            return () -> {
                try {
                    return Double.parseDouble(box.getValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            };
        });

        // STRING / GREEDY_STRING - a plain textbox is a correct editor for any string-valued kind.
        registerByValueType(String.class, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, initialConstant, "fmod.rulegui.param.hint.constant");
            sink.accept(box);
            return box::getValue;
        });
    }

    private static EditBox textBox(int x, int y, int width, Object initialValue, String hintI18nKey) {
        EditBox box = new EditBox(Minecraft.getInstance().font, x, y, width, 18, Component.empty());
        box.setMaxLength(512);
        box.setHint(Component.translatable(hintI18nKey).withStyle(ChatFormatting.DARK_GRAY));
        if (initialValue != null) {
            box.setValue(TypeAdaptor.parse(initialValue).asString());
        }
        return box;
    }

    private static EditBox numberBox(int x, int y, int width, String initialValue, String hintI18nKey) {
        EditBox box = new EditBox(Minecraft.getInstance().font, x, y, width, 18, Component.empty());
        box.setMaxLength(32);
        box.setHint(Component.translatable(hintI18nKey).withStyle(ChatFormatting.DARK_GRAY));
        box.setValue(initialValue);
        return box;
    }

    private static void openResourceLocationPicker(ParamEditorScreen<?> owner, int index, Iterable<ResourceLocation> locations) {
        List<OptionPickerScreen.Option> options = new ArrayList<>();
        for (ResourceLocation location : locations) {
            options.add(new OptionPickerScreen.Option(Component.literal(location.toString()), null,
                () -> applyPickedConstant(owner, index, location.toString())));
        }
        Minecraft.getInstance().setScreen(new OptionPickerScreen(owner, Component.translatable("fmod.rulegui.param.pick"), options));
    }

    /**
     * Opens a player picker sub-screen, which hands back the picked UUID(s) through
     * {@link ParamEditorScreen#setWorkingValue} rather than writing into a textbox directly.
     *
     * @param owner   the parent screen to return to after picking
     * @param index   the parameter row index to update with the picked UUID(s)
     * @param players the list of online players to pick from
     * @param append if {@code true} (the {@code List<UUID>} / "players" kind), the picked UUID is
     *               comma-appended to whatever constant text is already present; otherwise (the
     *               single {@code UUID} / "player" kind) it replaces the constant outright.
     * @see ParamWidgetFactory
     */ 
    private static void openPlayerPicker(ParamEditorScreen<?> owner, int index, List<RuleEditorBridge.PlayerInfo> players, boolean append) {
        List<OptionPickerScreen.Option> options = new ArrayList<>();
        for (RuleEditorBridge.PlayerInfo player : players) {
            options.add(new OptionPickerScreen.Option(Component.literal(player.name), Component.literal(player.uuid.toString()), () -> {
                if (append) {
                    Object existingConstant = owner.getWorkingValue(index).getConstantValue();
                    String existing = existingConstant == null ? "" : TypeAdaptor.parse(existingConstant).asString();
                    applyPickedConstant(owner, index, existing.isBlank() ? player.uuid.toString() : existing + "," + player.uuid.toString());
                } else {
                    applyPickedConstant(owner, index, player.uuid.toString());
                }
            }));
        }
        Minecraft.getInstance().setScreen(new OptionPickerScreen(owner, Component.translatable("fmod.rulegui.param.pickplayer"), options));
    }

    /** 
     * Replaces the row's constant value, preserving its variable half (if bound) unchanged. 
     * 
     * @param owner   the parent screen to return to after picking
     * @param index   the parameter row index to update with the picked constant
     * @param constant the new constant value to set
     */
    private static void applyPickedConstant(ParamEditorScreen<?> owner, int index, Object constant) {
        RuleParameter<?> current = owner.getWorkingValue(index);
        String variableName = current.getVariableName();
        owner.setWorkingValue(index, variableName != null
            ? RuleParameter.ofBoth(variableName, constant)
            : RuleParameter.ofConstant(constant));
        Minecraft.getInstance().setScreen(owner);
    }
}
