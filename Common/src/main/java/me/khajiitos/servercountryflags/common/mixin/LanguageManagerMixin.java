package me.khajiitos.servercountryflags.common.mixin;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LanguageManager.class)
public class LanguageManagerMixin {
    @Inject(at = @At("TAIL"), method = "onResourceManagerReload")
    public void onResourceManagerReload(ResourceManager manager, CallbackInfo info) {
        ServerCountryFlags.updateAPILanguage(Minecraft.getInstance().getLanguageManager().getSelected());
    }
}
