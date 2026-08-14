/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.base.gui;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Generic reusable single text-field prompt, used by the flow editor for anything that needs a
 * free-form name (new flow/node name, rename, "save as", etc.) without a dedicated screen class.
 * The confirm callback is responsible for any follow-up screen transition.
 */
public class TextPromptScreen extends Screen {

    private final Screen parent;
    private final Component prompt;
    private final String initialValue;
    private final Consumer<String> onConfirm;
    private EditBox input;

    /**
     * Constructs a text-input prompt screen.
     *
     * @param parent       the screen to return to on cancel
     * @param title        the screen title shown at the top
     * @param prompt       the hint text displayed below the title
     * @param initialValue the initial text in the input field, or {@code null} for empty
     * @param onConfirm    called with the final text when Done is pressed
     */
    public TextPromptScreen(Screen parent, Component title, Component prompt, String initialValue, Consumer<String> onConfirm) {
        super(title);
        this.parent = parent;
        this.prompt = prompt;
        this.initialValue = initialValue == null ? "" : initialValue;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        this.input = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 10, 200, 20, Component.empty());
        this.input.setHint(this.prompt.copy().withStyle(ChatFormatting.DARK_GRAY));
        this.input.setValue(this.initialValue);
        this.input.setMaxLength(256);
        this.addRenderableWidget(this.input);
        this.setInitialFocus(this.input);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> confirm())
            .pos(this.width / 2 - 100, this.height / 2 + 20).size(95, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.minecraft.setScreen(this.parent))
            .pos(this.width / 2 + 5, this.height / 2 + 20).size(95, 20).build());
    }

    private void confirm() {
        this.onConfirm.accept(this.input.getValue());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        context.drawCenteredString(this.font, this.prompt, this.width / 2, this.height / 2 - 26, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
