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

import com.google.gson.JsonPrimitive;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side registry of {@link ParamKind} GUI editors, the direct counterpart of
 * {@link com.ykn.fmod.server.rule.tool.RuleRegistry} for the one thing the server can't drive
 * itself: building a widget for a parameter, since server code can't invoke client code.
 *
 * <p>Every built-in kind is registered twice:
 * <ul>
 *   <li>{@link #registerByKind} - the editor that kind deserves. Kinds sharing a value type can
 *       differ here (e.g. {@link ParamKind#DIMENSION} gets a dimension picker while
 *       {@link ParamKind#RESOURCE_ID} is a plain id box, although both are
 *       {@link ResourceLocation}-valued).</li>
 *   <li>{@link #registerByValueType} - the fallback used by any kind that has no own entry, keyed
 *       by {@link ParamKind#valueType()}. Built-in kinds use it for kinds that share their value
 *       type, and third-party kinds get an editor for free as soon as they reuse a built-in value
 *       type. It is also what makes {@link ParamKind#intAtLeast(int)} /
 *       {@link ParamKind#doubleAtLeast(double)} work without per-call registration: each call
 *       mints a distinct {@code ParamKind} instance, so an identity-keyed map alone would never
 *       match them.</li>
 * </ul>
 *
 * <p>Only value types that can be edited unambiguously have a fallback: every built-in value type
 * except {@link Object} is registered, and {@code Object} (the value type of
 * {@link ParamKind#AUTO}) is deliberately left out - as a fallback it would claim every kind and
 * turn "no editor registered" into a silently wrong editor.
 *
 * <p>Third-party code that declares a custom {@link ParamKind} on the server should register a
 * matching widget builder here (in its own client entrypoint); without one,
 * {@link #isSupported} reports the kind as unsupported and the rule editor GUI refuses to open an
 * editor for any condition/action that uses it (falling back to command-line/JSON editing instead),
 * rather than guessing with a widget that doesn't actually understand the value.
 */
@OnlyIn(Dist.CLIENT)
public final class ParamWidgetRegistry {

    /**
     * Builds the constant-value widget(s) for one parameter row, returning a supplier that reads
     * the current constant value back out.
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
     * Registers the fallback builder for every {@link ParamKind} whose {@link ParamKind#valueType()}
     * equals {@code valueType} and that has no {@link #registerByKind} entry.
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
     * Registers every built-in {@link ParamKind}'s widget builder, by kind and by value type.
     */
    public static void registerDefault() {
        // Rotation is a pitch/yaw pair; the same editor is the fallback for any Vec2 parameter.
        WidgetBuilder vec2 = (owner, index, entry, initialConstant, x, y, width, sink) -> {
            int fieldWidth = (width - GAP) / 2;
            Vec2 value = initialConstant instanceof Vec2 constant ? constant : null;
            EditBox pitch = numberBox(x, y, fieldWidth, value == null ? "" : String.valueOf(value.x), "fmod.rulegui.param.hint.pitch");
            EditBox yaw = numberBox(x + fieldWidth + GAP, y, fieldWidth, value == null ? "" : String.valueOf(value.y), "fmod.rulegui.param.hint.yaw");
            sink.accept(pitch);
            sink.accept(yaw);
            return () -> {
                try {
                    float pitchValue = Float.parseFloat(pitch.getValue().trim());
                    float yawValue = Float.parseFloat(yaw.getValue().trim());
                    return Float.isFinite(pitchValue) && Float.isFinite(yawValue)
                        ? new Vec2(pitchValue, yawValue) : null;
                } catch (NumberFormatException exception) {
                    return null;
                }
            };
        };

        // NBT and NBT paths both parse through the kind's own fromJson, so one SNBT box serves
        // all three kinds and the two NBT value types.
        WidgetBuilder snbt = (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, null, "fmod.rulegui.param.hint.constant");
            box.setMaxLength(32767);
            box.setValue(initialConstant == null ? "" : initialConstant.toString());
            sink.accept(box);
            return () -> {
                try {
                    return entry.kind.fromJson(new JsonPrimitive(box.getValue()));
                } catch (IllegalArgumentException exception) {
                    return null;
                }
            };
        };

        // A plain id box: usable for any ResourceLocation-valued kind that has no picker of its own.
        WidgetBuilder resourceId = (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, initialConstant, "fmod.rulegui.param.hint.constant");
            sink.accept(box);
            return () -> ResourceLocation.tryParse(box.getValue().trim());
        };

        WidgetBuilder uuid = (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width - PICK_WIDTH - GAP, initialConstant, "fmod.rulegui.param.hint.player");
            sink.accept(box);
            sink.accept(Button.builder(Component.translatable("fmod.rulegui.param.pick"), b -> {
                owner.captureWorkingValues();
                RuleEditorBridge.onClient(RuleEditorBridge.onlinePlayers(), players -> openPlayerPicker(owner, index, players, false));
            }).pos(x + width - PICK_WIDTH, y).size(PICK_WIDTH, 18).build());
            return () -> {
                try {
                    return UUID.fromString(box.getValue().trim());
                } catch (IllegalArgumentException exception) {
                    return null;
                }
            };
        };

        WidgetBuilder uuidList = (owner, index, entry, initialConstant, x, y, width, sink) -> {
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
        };

        WidgetBuilder vec3 = (owner, index, entry, initialConstant, x, y, width, sink) -> {
            int fieldWidth = (width - 2 * GAP) / 3;
            EditBox xBox = numberBox(x, y, fieldWidth, initialConstant instanceof Vec3 v ? String.valueOf(v.x) : "", "fmod.rulegui.param.hint.x");
            EditBox yBox = numberBox(x + fieldWidth + GAP, y, fieldWidth, initialConstant instanceof Vec3 v ? String.valueOf(v.y) : "", "fmod.rulegui.param.hint.y");
            EditBox zBox = numberBox(x + 2 * (fieldWidth + GAP), y, fieldWidth, initialConstant instanceof Vec3 v ? String.valueOf(v.z) : "", "fmod.rulegui.param.hint.z");
            sink.accept(xBox);
            sink.accept(yBox);
            sink.accept(zBox);
            return () -> {
                try {
                    return new Vec3(Double.parseDouble(xBox.getValue().trim()), Double.parseDouble(yBox.getValue().trim()), Double.parseDouble(zBox.getValue().trim()));
                } catch (NumberFormatException exception) {
                    return null;
                }
            };
        };

        WidgetBuilder bool = (owner, index, entry, initialConstant, x, y, width, sink) -> {
            CycleButton<Boolean> box = CycleButton.onOffBuilder(initialConstant instanceof Boolean b && b)
                .create(x, y, width, 18, Component.empty());
            sink.accept(box);
            return box::getValue;
        };

        WidgetBuilder integer = (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, initialConstant, "fmod.rulegui.param.hint.constant");
            sink.accept(box);
            return () -> {
                try {
                    return Integer.parseInt(box.getValue().trim());
                } catch (NumberFormatException exception) {
                    return null;
                }
            };
        };

        WidgetBuilder decimal = (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, initialConstant, "fmod.rulegui.param.hint.constant");
            sink.accept(box);
            return () -> {
                try {
                    return Double.parseDouble(box.getValue().trim());
                } catch (NumberFormatException exception) {
                    return null;
                }
            };
        };

        WidgetBuilder text = (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, initialConstant, "fmod.rulegui.param.hint.constant");
            sink.accept(box);
            return box::getValue;
        };

        // Kinds that need an editor their value type can't express.
        registerByKind(ParamKind.ROTATION, vec2);
        registerByKind(ParamKind.NBT, snbt);
        registerByKind(ParamKind.NBT_COMPOUND, snbt);
        registerByKind(ParamKind.NBT_PATH, snbt);
        registerByKind(ParamKind.RESOURCE_ID, resourceId);
        registerByKind(ParamKind.VARIABLE_NAME, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, initialConstant, "fmod.rulegui.param.hint.varname");
            sink.accept(box);
            return () -> {
                String value = box.getValue().trim();
                return RuleCondition.NAME_PATTERN.matcher(value).matches() ? value : null;
            };
        });
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
        registerByKind(ParamKind.AUTO, (owner, index, entry, initialConstant, x, y, width, sink) -> {
            EditBox box = textBox(x, y, width, initialConstant, "fmod.rulegui.param.hint.constant");
            sink.accept(box);
            return () -> box.getValue().isEmpty() ? null : TypeAdaptor.parse(box.getValue()).autoCast();
        });

        // One fallback per built-in value type. Object is skipped on purpose: it would claim
        // every unregistered kind instead of letting isSupported report it.
        registerByValueType(Vec2.class, vec2);
        registerByValueType(Tag.class, snbt);
        registerByValueType(CompoundTag.class, snbt);
        registerByValueType(ResourceLocation.class, resourceId);
        registerByValueType(UUID.class, uuid);
        registerByValueType(List.class, uuidList);
        registerByValueType(Vec3.class, vec3);
        registerByValueType(Boolean.class, bool);
        registerByValueType(Integer.class, integer);
        registerByValueType(Double.class, decimal);
        registerByValueType(String.class, text);
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
     * @param owner    the parent screen to return to after picking
     * @param index    the parameter row index to update with the picked constant
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
