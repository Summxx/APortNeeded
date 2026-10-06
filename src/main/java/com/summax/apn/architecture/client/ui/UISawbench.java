package com.summax.apn.architecture.client.ui;

import com.summax.apn.architecture.common.ArchitectureMod;
import com.summax.apn.architecture.common.block.container.ContainerSawbench;
import com.summax.apn.architecture.common.shape.ShapePage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Sawbench screen, layout taken from the 1.12 version of the mod.
 */
public class UISawbench extends AbstractContainerScreen<ContainerSawbench> {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(ArchitectureMod.MOD_ID, "textures/gui/gui_sawbench.png");
    private static final ResourceLocation SHAPE_MENU_BG = new ResourceLocation(ArchitectureMod.MOD_ID, "textures/gui/shapemenu_bg.png");
    // 110 icons per sheet, double resolution.
    private static final ResourceLocation[] SHAPE_MENU_ITEMS = {
            new ResourceLocation(ArchitectureMod.MOD_ID, "textures/gui/shapemenu_items_0.png"),
            new ResourceLocation(ArchitectureMod.MOD_ID, "textures/gui/shapemenu_items_1.png")
    };
    private static final int SHAPE_ICONS_PER_SHEET = 110;
    private static final int SHAPE_ICON_SHEET_SIZE = 1024;
    private static final int SHAPE_ICON_SHEET_SCALE = 2;

    private static final int PAGE_MENU_LEFT = 176;
    private static final int PAGE_MENU_TOP = 19;
    private static final int PAGE_MENU_WIDTH = 58;
    private static final int PAGE_MENU_ROW_HEIGHT = 10;

    private static final int SHAPE_MENU_LEFT = 44;
    private static final int SHAPE_MENU_TOP = 23;
    private static final int SHAPE_MENU_MARGIN = 4;
    private static final int SHAPE_MENU_CELL_SIZE = 24;
    private static final int SHAPE_MENU_ROWS = 4;
    private static final int SHAPE_MENU_COLS = 5;
    private static final int SHAPE_MENU_WIDTH = SHAPE_MENU_COLS * SHAPE_MENU_CELL_SIZE;
    private static final int SHAPE_MENU_HEIGHT = SHAPE_MENU_ROWS * SHAPE_MENU_CELL_SIZE;
    private static final int SHAPE_ICON_U_SIZE = 40;
    private static final int SHAPE_ICON_V_SIZE = 45;
    private static final int SHAPE_ICON_WIDTH = 20;
    private static final int SHAPE_ICON_HEIGHT = 22;
    private static final int SHAPE_ICONS_PER_ROW = 10;

    private static final int SELECTED_SHAPE_TITLE_LEFT = 40;
    private static final int SELECTED_SHAPE_TITLE_TOP = 128;
    private static final int SELECTED_SHAPE_TITLE_RIGHT = 168;
    private static final int MATERIAL_USAGE_LEFT = 7;
    private static final int MATERIAL_USAGE_TOP = 82;

    private static final int TEXT_COLOUR = 0x404040;
    private static final int PAGE_HIGHLIGHT_COLOUR = 0xFF66CCFF;

    public UISawbench(ContainerSawbench container, Player player) {
        this(container, player.getInventory(), Component.translatable("apn.gui.sawbench.title"));
    }

    public UISawbench(ContainerSawbench container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.imageWidth = 242;
        this.imageHeight = 224;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderShapeTooltip(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);
        this.renderShapeMenu(graphics, x, y);
        this.renderShapeSelection(graphics, x, y);
        this.renderPageMenu(graphics, x, y);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 7, 7, TEXT_COLOUR, false);

        var shape = this.menu.getSelectedShape();
        if (shape != null) {
            var name = Component.translatable(shape.getLocalizationKey());
            int x = SELECTED_SHAPE_TITLE_LEFT;
            int w = this.font.width(name);
            if (x + w > SELECTED_SHAPE_TITLE_RIGHT)
                x = SELECTED_SHAPE_TITLE_RIGHT - w;
            graphics.drawString(this.font, name, x, SELECTED_SHAPE_TITLE_TOP, TEXT_COLOUR, false);

            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(MATERIAL_USAGE_LEFT, MATERIAL_USAGE_TOP, 0);
            pose.scale(0.5F, 0.5F, 1F);
            graphics.drawString(this.font,
                    Component.translatable("apn.gui.sawbench.ratio", this.menu.getMaterialMultiple(), this.menu.getResultMultiple()),
                    0, 0, TEXT_COLOUR, false);
            pose.popPose();
        }
    }

    private void renderPageMenu(GuiGraphics graphics, int x, int y) {
        int left = x + PAGE_MENU_LEFT;
        int top = y + PAGE_MENU_TOP;
        int selected = this.menu.getSelectedPage();
        graphics.fill(left, top + selected * PAGE_MENU_ROW_HEIGHT, left + PAGE_MENU_WIDTH, top + (selected + 1) * PAGE_MENU_ROW_HEIGHT, PAGE_HIGHLIGHT_COLOUR);
        for (int i = 0; i < ShapePage.PAGES.size(); i++) {
            graphics.drawString(this.font, Component.translatable(ShapePage.PAGES.get(i).translationKey()),
                    left + 1, top + 1 + i * PAGE_MENU_ROW_HEIGHT, TEXT_COLOUR, false);
        }
    }

    private void renderShapeMenu(GuiGraphics graphics, int x, int y) {
        int left = x + SHAPE_MENU_LEFT;
        int top = y + SHAPE_MENU_TOP;
        int w = SHAPE_MENU_WIDTH + 2 * SHAPE_MENU_MARGIN;
        int h = SHAPE_MENU_HEIGHT + 2 * SHAPE_MENU_MARGIN;
        // The menu background texture is drawn at half scale.
        graphics.blit(SHAPE_MENU_BG, left - SHAPE_MENU_MARGIN, top - SHAPE_MENU_MARGIN, w, h, 0, 0, w * 2, h * 2, 256, 256);

        int page = this.menu.getSelectedPage();
        if (page < 0 || page >= ShapePage.PAGES.size())
            return;
        var shapes = ShapePage.PAGES.get(page).shapes();
        for (int i = 0; i < shapes.size() && i < SHAPE_MENU_ROWS * SHAPE_MENU_COLS; i++) {
            int row = i / SHAPE_MENU_COLS;
            int col = i % SHAPE_MENU_COLS;
            int icon = ShapePage.getIconIndex(shapes.get(i));
            int sheet = Math.min(icon / SHAPE_ICONS_PER_SHEET, SHAPE_MENU_ITEMS.length - 1);
            int iconInSheet = icon % SHAPE_ICONS_PER_SHEET;
            int u = (iconInSheet % SHAPE_ICONS_PER_ROW) * SHAPE_ICON_U_SIZE * SHAPE_ICON_SHEET_SCALE;
            int v = (iconInSheet / SHAPE_ICONS_PER_ROW) * SHAPE_ICON_V_SIZE * SHAPE_ICON_SHEET_SCALE;
            int ix = left + col * SHAPE_MENU_CELL_SIZE + (SHAPE_MENU_CELL_SIZE - SHAPE_ICON_WIDTH) / 2;
            int iy = top + row * SHAPE_MENU_CELL_SIZE + (SHAPE_MENU_CELL_SIZE - SHAPE_ICON_HEIGHT) / 2;
            graphics.blit(SHAPE_MENU_ITEMS[sheet], ix, iy, SHAPE_ICON_WIDTH, SHAPE_ICON_HEIGHT, u, v,
                    SHAPE_ICON_U_SIZE * SHAPE_ICON_SHEET_SCALE, SHAPE_ICON_V_SIZE * SHAPE_ICON_SHEET_SCALE,
                    SHAPE_ICON_SHEET_SIZE, SHAPE_ICON_SHEET_SIZE);
        }
    }

    private void renderShapeSelection(GuiGraphics graphics, int x, int y) {
        int i = this.menu.getSelectedSlot();
        int row = i / SHAPE_MENU_COLS;
        int col = i % SHAPE_MENU_COLS;
        int sx = x + SHAPE_MENU_LEFT + SHAPE_MENU_CELL_SIZE * col;
        int sy = y + SHAPE_MENU_TOP + SHAPE_MENU_CELL_SIZE * row;
        // The selection frame is the dashed square drawn in the background texture, shrunk to cell size.
        graphics.blit(BACKGROUND, sx, sy, 25, 25, 44, 23, 49, 49, 256, 256);
    }

    private void renderShapeTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int index = this.getShapeIndexAt(mouseX, mouseY);
        if (index < 0)
            return;
        var shapes = ShapePage.PAGES.get(this.menu.getSelectedPage()).shapes();
        if (index < shapes.size())
            graphics.renderTooltip(this.font, Component.translatable(shapes.get(index).getLocalizationKey()), mouseX, mouseY);
    }

    private int getShapeIndexAt(double mouseX, double mouseY) {
        double mx = mouseX - this.leftPos - SHAPE_MENU_LEFT;
        double my = mouseY - this.topPos - SHAPE_MENU_TOP;
        if (mx < 0 || my < 0 || mx >= SHAPE_MENU_WIDTH || my >= SHAPE_MENU_HEIGHT)
            return -1;
        return (int) (my / SHAPE_MENU_CELL_SIZE) * SHAPE_MENU_COLS + (int) (mx / SHAPE_MENU_CELL_SIZE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double px = mouseX - this.leftPos - PAGE_MENU_LEFT;
        double py = mouseY - this.topPos - PAGE_MENU_TOP;
        if (px >= 0 && py >= 0 && px < PAGE_MENU_WIDTH) {
            int page = (int) (py / PAGE_MENU_ROW_HEIGHT);
            if (page < ShapePage.PAGES.size()) {
                this.menu.selectPage(page);
                this.sendButton(ContainerSawbench.PAGE_BUTTON_START + page);
                return true;
            }
        }

        int index = this.getShapeIndexAt(mouseX, mouseY);
        if (index >= 0 && index < ShapePage.PAGES.get(this.menu.getSelectedPage()).size()) {
            this.menu.selectShape(this.menu.getSelectedPage(), index);
            this.sendButton(index);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
