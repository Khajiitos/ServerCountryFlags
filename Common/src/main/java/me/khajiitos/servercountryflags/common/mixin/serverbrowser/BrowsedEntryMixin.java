package me.khajiitos.servercountryflags.common.mixin.serverbrowser;

import com.epherical.serverbrowser.client.list.ServerBrowserList;
import com.epherical.serverbrowser.client.screen.ServerBrowserScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.APIResponse;
import me.khajiitos.servercountryflags.common.util.FlagPosition;
import me.khajiitos.servercountryflags.common.util.FlagRenderInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(ServerBrowserList.BrowsedEntry.class)
public class BrowsedEntryMixin {
    @Shadow(remap = false)
    private ServerData serverData;

    @Shadow(remap = false) @Final private ServerBrowserScreen screen;

    // Suppressing all warnings - don't know what the exact name is
    // Minecraft Dev extension complains that it can't find the "render" function,
    // because the project we are working with here is obfuscated
    // When compiling on Forge/Fabric, the mod is deobfuscated there, so it works
    @SuppressWarnings("all")
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I", ordinal = 0), method = "render", index = 2)
    public int serverNameX(int oldX) {
        if (!Config.cfg.serverBrowserIntegration) {
            return oldX;
        }

        if (Config.cfg.flagPosition == FlagPosition.BEHIND_NAME) {
            APIResponse apiResponse = ServerCountryFlags.servers.get(serverData.ip);
            FlagRenderInfo renderInfo = ServerCountryFlags.getFlagRenderInfo(apiResponse);

            if (renderInfo != null) {
                return oldX + (int)(renderInfo.flagAspectRatio() * 8.f) + 3;
            }
        }

        return oldX;
    }

    @SuppressWarnings("all")
    @Inject(at = @At("TAIL"), method = "render")
    public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        if (!Config.cfg.serverBrowserIntegration) {
            return;
        }

        APIResponse apiResponse = ServerCountryFlags.servers.get(serverData.ip);
        FlagRenderInfo flagRenderInfo = ServerCountryFlags.getFlagRenderInfo(apiResponse);

        if (flagRenderInfo == null) {
            return;
        }

        int height = Config.cfg.flagPosition == FlagPosition.BEHIND_NAME ? 8 : 12;
        int width = (int)(flagRenderInfo.flagAspectRatio() * height);

        int startingX, startingY;
        switch (Config.cfg.flagPosition) {
            case LEFT -> {
                startingX = x - width - 6;
                startingY = y + (entryHeight / 2) - (height / 2);
            }
            case RIGHT -> {
                startingX = x + entryWidth + 10;
                startingY = y + (entryHeight / 2) - (height / 2);
            }
            case BEHIND_NAME -> {
                startingX = x + 35;
                startingY = y + 1;
            }
            default -> {
                startingX = x + entryWidth - width - 6;
                startingY = y + entryHeight - height - 4;
            }
        }

        ResourceLocation textureId = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/gui/flags/" + flagRenderInfo.countryCode() + ".png");

        RenderSystem.enableBlend();
        context.blit(textureId, startingX, startingY, 0.0F, 0.0F, width, height, width, height);

        if (Config.cfg.flagBorder) {
            context.renderOutline(startingX - 1, startingY - 1, width + 2, height + 2, Config.cfg.borderColor.toARGB());
        }

        RenderSystem.disableBlend();
        if (mouseX >= startingX && mouseX <= startingX + width && mouseY >= startingY && mouseY <= startingY + height) {
            screen.setToolTip(flagRenderInfo.tooltip());
        }
    }
}
