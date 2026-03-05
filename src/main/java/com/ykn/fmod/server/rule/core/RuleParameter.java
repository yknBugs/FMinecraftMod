/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.tool.ThrowingSupplier;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * A typed parameter that can be resolved either from a named event variable
 * or from a fixed constant value.
 *
 * <p>Each configurable value on a {@link SourceCondition} or {@link RuleAction}
 * is wrapped in a {@code RuleParameter}. When the rule fires, the parameter
 * is resolved via {@link #resolve(RuleContext, Class)}:
 * <ol>
 *   <li>If a {@code variableName} is set, the value is looked up in the context's variable map.
 *       If the lookup succeeds and the value is assignment-compatible with {@code T}, it is used.</li>
 *   <li>Otherwise, the {@code constantValue} is returned as a fallback.</li>
 * </ol>
 *
 * <p>Parameters are constructed through the static factory methods:
 * <ul>
 *   <li>{@link #ofVariable(String)}  – variable reference only (no constant fallback)</li>
 *   <li>{@link #ofConstant(Object)}  – constant only (no variable read)</li>
 *   <li>{@link #ofBoth(String, Object)} – variable with constant fallback</li>
 *   <li>{@link #ofNull()}            – both null (parameter is effectively absent)</li>
 * </ul>
 *
 * <p>JSON representation:
 * <pre>{@code
 * {"variable": "playerId"}                           // variable only
 * {"constant": "minecraft:overworld"}               // constant only
 * {"variable": "targetPos", "constant": {"x":0,"y":64,"z":0}}  // both
 * }</pre>
 *
 * @param <T> the value type (e.g. {@link java.util.UUID}, {@link net.minecraft.util.math.Vec3d})
 * @see SourceCondition
 * @see RuleAction
 */
public class RuleParameter<T> {

    /**
     * The variable key to look up in {@link RuleContext#getVariables()}.
     * {@code null} if this parameter has no variable binding.
     */
    @Nullable
    private final String variableName;

    /**
     * The constant fallback value returned when the variable is absent or type-incompatible.
     * {@code null} if this parameter has no constant.
     */
    @Nullable
    private final T constantValue;

    private RuleParameter(@Nullable String variableName, @Nullable T constantValue) {
        this.variableName = variableName;
        this.constantValue = constantValue;
    }

    /**
     * Creates a parameter backed only by a named event variable.
     *
     * @param variableName the variable key in the context's map
     * @param <T>          the value type
     * @return a new {@code RuleParameter} with no constant fallback
     */
    public static <T> RuleParameter<T> ofVariable(String variableName) {
        return new RuleParameter<>(variableName, null);
    }

    /**
     * Creates a parameter backed only by a fixed constant value.
     *
     * @param constantValue the constant value
     * @param <T>           the value type
     * @return a new {@code RuleParameter} that always returns the constant
     */
    public static <T> RuleParameter<T> ofConstant(T constantValue) {
        return new RuleParameter<>(null, constantValue);
    }

    /**
     * Creates a parameter with both a variable binding and a constant fallback.
     *
     * @param variableName  the variable key in the context's map
     * @param constantValue the fallback value if the variable is absent or type-incompatible
     * @param <T>           the value type
     * @return a new {@code RuleParameter}
     */
    public static <T> RuleParameter<T> ofBoth(String variableName, T constantValue) {
        return new RuleParameter<>(variableName, constantValue);
    }

    /**
     * Creates a parameter with neither a variable binding nor a constant (always resolves to {@code null}).
     *
     * @param <T> the value type
     * @return a new {@code RuleParameter} that always resolves to {@code null}
     */
    public static <T> RuleParameter<T> ofNull() {
        return new RuleParameter<>(null, null);
    }

    /**
     * Constructs a {@code RuleParameter} from a Brigadier command context.
     *
     * <p>Examines the {@code arguments} set to determine which of the three modes was
     * specified by the player: constant-only, variable-only, or both.
     *
     * @param constArgumentName      the Brigadier argument name for the constant value
     * @param variableArgumentName   the Brigadier argument name for the variable reference
     * @param constValueSupplier     called when the constant argument is present to obtain the value
     * @param arguments              the set of argument names present in the command invocation
     * @param context                the Brigadier command context
     * @param <T>                    the value type
     * @return a new {@code RuleParameter} built from the given command arguments
     * @throws com.mojang.brigadier.exceptions.CommandSyntaxException if the supplier throws
     */
    public static <T> RuleParameter<T> fromCommandContext(
        String constArgumentName, String variableArgumentName, 
        ThrowingSupplier<T, CommandSyntaxException> constValueSupplier, 
        Set<String> arguments, CommandContext<ServerCommandSource> context
    ) throws CommandSyntaxException {
        T constValue = null;
        String variableValue = null;
        if (arguments.contains(constArgumentName)) {
            constValue = constValueSupplier.get();
        }
        if (arguments.contains(variableArgumentName)) {
            variableValue = StringArgumentType.getString(context, variableArgumentName);
        }
        return new RuleParameter<>(variableValue, constValue);
    }

    /**
     * Resolves this parameter to a concrete value in the given context.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>If {@code variableName} is set, the variable is looked up in
     *       {@link RuleContext#getVariables()}.</li>
     *   <li>If the variable is present and assignable to {@code clazz}, it is returned.</li>
     *   <li>Otherwise, the {@code constantValue} is returned (which may also be {@code null}).</li>
     * </ol>
     *
     * @param context the rule execution context
     * @param clazz   the expected runtime type of the value
     * @return the resolved value, or {@code null} if neither source is available
     */
    @Nullable
    public T resolve(RuleContext context, Class<T> clazz) {
        if (variableName != null) {
            Object value = context.getVariable(variableName);
            if (value == null) {
                return constantValue;
            }
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            } else {
                return constantValue;
            }
        }
        return constantValue;
    }

    public Text render() {
        // (variable: variableName, constant: constantValue)
        MutableText text = Text.literal("(");
        boolean hasValidValue = false;
        if (variableName != null) {
            text = text.append(Util.parseTranslatableText("fmod.misc.var")).append(": " + variableName);
            hasValidValue = true;
        }
        if (constantValue != null) {
            if (hasValidValue) {
                text = text.append(", ");
            }
            text = text.append(Util.parseTranslatableText("fmod.misc.const")).append(": " + TypeAdaptor.parse(constantValue).asString());
        }
        text = text.append(")");
        return text;
    }

    /**
     * Returns the variable name if this parameter is variable-backed, or {@code null} if it is constant-only.
     * 
     * @return the variable name, or {@code null} if none
     */
    @Nullable
    public String getVariableName() {
        return variableName;
    }

    /**
     * Returns the constant value if this parameter is constant-backed, or {@code null} if it is variable-only.
     * 
     * @return the constant value, or {@code null} if none
     */
    @Nullable
    public T getConstantValue() {
        return constantValue;
    }

    /**
     * Serializes this parameter to a {@link JsonObject}.
     *
     * <p>The resulting JSON may contain a {@code "variable"} string and/or a {@code "constant"}
     * element, depending on which fields are non-null.
     *
     * @param constantSerializer converts the constant value to a {@link com.google.gson.JsonElement}
     * @return a JSON representation of this parameter
     */
    public JsonObject toJson(Function<T, JsonElement> constantSerializer) {
        JsonObject json = new JsonObject();
        if (variableName != null) {
            json.addProperty("variable", variableName);
        }
        if (constantValue != null) {
            json.add("constant", constantSerializer.apply(constantValue));
        }
        return json;
    }

    /**
     * Static convenience overload for {@link #toJson(java.util.function.Function)}.
     *
     * @param parameter           the parameter to serialise
     * @param constantSerializer  converter from the value type to a JSON element
     * @param <T>                 the value type
     * @return the serialised JSON object
     */
    public static <T> JsonObject toJson(RuleParameter<T> parameter, Function<T, JsonElement> constantSerializer) {
        return parameter.toJson(constantSerializer);
    }
    
    /**
     * Deserializes a {@code RuleParameter} from the {@code "value"} object inside a component JSON.
     *
     * <p>Navigates to {@code json["value"][memberName]} and reads optional {@code "variable"}
     * and {@code "constant"} fields.
     *
     * @param json                  the containing component JSON object
     * @param memberName            the key within {@code "value"} for this parameter
     * @param constantDeserializer  converts the raw JSON element to the value type
     * @param <T>                   the value type
     * @return a new {@code RuleParameter}
     */
    public static <T> RuleParameter<T> fromJson(JsonObject json, String memberName, Function<JsonElement, T> constantDeserializer) {
        if (!json.has("value") || !json.get("value").isJsonObject()) {
            Util.LOGGER.warn("FMinecraftMod: Missing 'value' object for parameter '" + memberName + "'. Defaulting to null.");
            return new RuleParameter<>(null, null);
        }
        JsonObject valueObj = json.getAsJsonObject("value");
        if (!valueObj.has(memberName) || !valueObj.get(memberName).isJsonObject()) {
            Util.LOGGER.warn("FMinecraftMod: Missing '" + memberName + "' object for parameter. Defaulting to null.");
            return new RuleParameter<>(null, null);
        }
        JsonObject valueJson = valueObj.getAsJsonObject(memberName);
        String variableName = valueJson.has("variable") ? valueJson.get("variable").getAsString() : null;
        T constantValue = null;
        try {
            constantValue = valueJson.has("constant") ? constantDeserializer.apply(valueJson.get("constant")) : null;
        } catch (Exception e) {
            Util.LOGGER.warn("FMinecraftMod: Failed to deserialize constant for parameter '" + memberName + "'. Defaulting to null.", e);
        }
        return new RuleParameter<>(variableName, constantValue);
    }
}
