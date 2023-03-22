package me.khajiitos.betterclocks.mixin;

import me.khajiitos.betterclocks.BetterClocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Shadow
	int scaledWidth;

	@Shadow
	int scaledHeight;

	@Shadow
	MinecraftClient client;

	@Inject(at = @At("TAIL"), method = "render")
	public void render(MatrixStack matrices, float tickDelta, CallbackInfo info) {
		if (client.player == null || client.options.hudHidden)
			return;
		ItemStack mainHandStack = client.player.getMainHandStack();
		ItemStack offHandStack = client.player.getOffHandStack();

		if	((mainHandStack == null || mainHandStack.getItem() != Items.CLOCK) &&
			(offHandStack == null || offHandStack.getItem() != Items.CLOCK))
			return;

		Text text = BetterClocks.getTimeText();
		int width = client.textRenderer.getWidth(text);
		int x = (scaledWidth - width) / 2;
		int y = scaledHeight - 83;
		if (client.player.isCreative())
			y += 28;
		client.textRenderer.drawWithShadow(matrices, text, x, y, 0xFFFFFFFF);
	}
}
