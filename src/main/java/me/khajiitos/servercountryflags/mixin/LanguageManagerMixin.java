package me.khajiitos.servercountryflags.mixin;

import me.khajiitos.servercountryflags.Compatibility;
import me.khajiitos.servercountryflags.ServerCountryFlags;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LanguageManager.class)
public class LanguageManagerMixin {
    @Inject(at = @At("TAIL"), method = "reload")
    public void reload(ResourceManager manager, CallbackInfo info) {
        ServerCountryFlags.updateAPILanguage(Compatibility.getLanguageCode());
    }
}
