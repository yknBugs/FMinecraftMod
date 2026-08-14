/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client.base.gui;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Generic reusable single-selection list screen with a live-filtering search box. Used throughout
 * the flow editor for anything that boils down to "pick one of these options" (node type palette,
 * pick a flow file to load) instead of a dedicated screen per case.
 *
 * <p>Rows are plain selectable text (like vanilla's language/resource-pack lists), not buttons.</p>
 */
@OnlyIn(Dist.CLIENT)
public class OptionPickerScreen extends Screen {

    /** One selectable row. */
    public static final class Option {
        private final Component label;
        private final Component tooltip;
        private final Runnable onSelect;
        private final String searchText;

        public Option(Component label, Component tooltip, Runnable onSelect) {
            this(label, tooltip, onSelect, label.getString());
        }

        /**
         * Constructs an option with a custom search text string.
         *
         * @param label      the display label shown in the list
         * @param tooltip    optional tooltip shown on hover, or {@code null}
         * @param onSelect   the action to run when this option is clicked
         * @param searchText the lowercased text used for live filtering
         */
        public Option(Component label, Component tooltip, Runnable onSelect, String searchText) {
            this.label = label;
            this.tooltip = tooltip;
            this.onSelect = onSelect;
            this.searchText = searchText.toLowerCase(Locale.ROOT);
        }
    }

    private final Screen parent;
    private final List<Option> options;
    private PickerList list;
    private EditBox search;
    private Button cancelButton;

    public OptionPickerScreen(Screen parent, Component title, List<Option> options) {
        super(title);
        this.parent = parent;
        this.options = options;
    }

    @Override
    protected void init() {
        this.search = new EditBox(this.font, this.width / 2 - 150, 30, 300, 20, Component.empty());
        this.search.setHint(Component.translatable("fmod.flowgui.picker.search.hint").withStyle(ChatFormatting.DARK_GRAY));
        this.search.setResponder(text -> applyFilter());
        this.addRenderableWidget(this.search);
        // this.setInitialFocus(this.search);

        this.list = new PickerList(this.minecraft, this.width, this.height, 60, this.height - 40);
        this.addWidget(this.list);
        applyFilter();

        this.cancelButton = Button.builder(CommonComponents.GUI_CANCEL, b -> this.minecraft.setScreen(this.parent))
            .pos(this.width / 2 - 100, this.height - 30).size(200, 20).build();
        this.addRenderableWidget(this.cancelButton);
    }

    private void applyFilter() {
        String query = this.search.getValue().toLowerCase(Locale.ROOT);
        this.list.clear();
        for (Option option : this.options) {
            if (query.isEmpty() || option.searchText.contains(query)) {
                this.list.addOption(option);
            }
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // this.list is added via addWidget (not addRenderableWidget) so it isn't auto-rendered by
        // super.render(); it must render first since AbstractSelectionList paints a full-width fade
        // above/below its own bounds that would otherwise mask the renderable widgets drawn by super.render().
        this.list.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private static class PickerList extends ObjectSelectionList<PickerList.Entry> {

        PickerList(Minecraft client, int width, int height, int top, int bottom) {
            super(client, width, height, top, bottom, 20);
        }

        void addOption(Option option) {
            this.addEntry(new Entry(option, this.getRowWidth()));
        }

        void clear() {
            this.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return this.width - 40;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 5;
        }

        private static class Entry extends ObjectSelectionList.Entry<Entry> {
            private final Option option;
            private final StringWidget textWidget;

            Entry(Option option, int rowWidth) {
                this.option = option;
                this.textWidget = new StringWidget(0, 0, rowWidth, 20, option.label, Minecraft.getInstance().font).alignCenter();
                if (option.tooltip != null) {
                    // Tooltip (unlike a raw renderTooltip(Font, Component, ...) call) correctly renders
                    // embedded "\n" as a line break instead of a missing-glyph box.
                    this.textWidget.setTooltip(Tooltip.create(option.tooltip));
                }
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                if (hovered) {
                    context.fill(x - 2, y - 1, x + entryWidth + 2, y + entryHeight + 1, 0x30FFFFFF);
                }
                this.textWidget.setX(x);
                this.textWidget.setY(y + (entryHeight - 18) / 2);
                this.textWidget.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                this.option.onSelect.run();
                return true;
            }

            @Override
            public Component getNarration() {
                return this.option.label;
            }
        }
    }
}
