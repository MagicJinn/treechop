package ht.treechop.client.gui.util;

import ht.treechop.TreeChop;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public enum Sprite {
    CHOP_INDICATOR(TreeChop.resource("widget/chop_indicator"), 20, 20),
    TOGGLE_BUTTON_OFF(TreeChop.resource("widget/toggle_off"), 32, 20),
    TOGGLE_BUTTON_ON(TreeChop.resource("widget/toggle_on"), 32, 20),
    HIGHLIGHTED_TOGGLE_BUTTON_OFF(TreeChop.resource("widget/toggle_off_highlighted"), 32, 20),
    HIGHLIGHTED_TOGGLE_BUTTON_ON(TreeChop.resource("widget/toggle_on_highlighted"), 32, 20),
    LOCKED_TOGGLE_BUTTON_OFF(TreeChop.resource("widget/toggle_off_locked"), 32, 20),
    LOCKED_TOGGLE_BUTTON_ON(TreeChop.resource("widget/toggle_on_locked"), 32, 20),
    PAGE_ONE(TreeChop.resource("widget/page_one"), 32, 20),
    HIGHLIGHTED_PAGE_ONE(TreeChop.resource("widget/page_one_highlighted"), 32, 20),
    PAGE_TWO(TreeChop.resource("widget/page_two"), 32, 20),
    HIGHLIGHTED_PAGE_TWO(TreeChop.resource("widget/page_two_highlighted"), 32, 20),
    ;

    public static final int TEXTURE_WIDTH = 64;
    public static final int TEXTURE_HEIGHT = 120;

    private final Identifier texture;
    public final int width;
    public final int height;

    Sprite(Identifier texture, int width, int height) {
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    public void blit(GuiGraphicsExtractor gui, int x, int y, float alpha) {
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, this.texture, x, y, this.width, this.height, ARGB.white(alpha));
    }

    public void blitMirror(GuiGraphicsExtractor gui, int x, int y, float alpha, float scale, boolean mirror) {
        int mx = mirror ? x + (int) (this.width * scale) : x;
        int mw = (int) ((mirror ? -this.width : this.width) * scale);
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, this.texture, mx, y, mw, (int) (this.height * scale), ARGB.white(alpha));
    }

}
