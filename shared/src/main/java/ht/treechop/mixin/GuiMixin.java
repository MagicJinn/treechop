package ht.treechop.mixin;

import ht.treechop.client.gui.screen.ChopIndicator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void injectChopIndicator(GuiGraphicsExtractor gui, DeltaTracker delta, CallbackInfo info) {
        ChopIndicator.render(gui, gui.guiWidth(), gui.guiHeight());
    }
}
