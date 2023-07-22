package me.khajiitos.servercountryflags.forge.mixin;

import me.khajiitos.servercountryflags.forge.ServerCountryFlags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LanguageManager.class)
public class LanguageManagerMixin {
    @Inject(at = @At("TAIL"), method = "onResourceManagerReload")
    public void onResourceManagerReload(IResourceManager manager, CallbackInfo info) {
        ServerCountryFlags.updateAPILanguage(Minecraft.getInstance().getLanguageManager().getSelected().getName());
    }
}
