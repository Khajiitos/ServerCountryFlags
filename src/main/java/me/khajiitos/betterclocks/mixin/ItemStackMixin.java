package me.khajiitos.betterclocks.mixin;

import me.khajiitos.betterclocks.BetterClocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Shadow @Final
    Item item;

    @Inject(at = @At("RETURN"), method = "getTooltip")
    public void getTooltip(@Nullable PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> callbackInfo) {
        if (item != Items.CLOCK)
            return;
        List<Text> toolTip = callbackInfo.getReturnValue();
        toolTip.add(BetterClocks.getTimeText().formatted(Formatting.GRAY));
    }
}
