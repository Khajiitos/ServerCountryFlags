package me.khajiitos.servercountryflags.common.mixin.serverbrowser;

import com.epherical.serverbrowser.client.list.ServerBrowserList;
import com.epherical.serverbrowser.client.screen.ServerBrowserScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.APIResponse;
import me.khajiitos.servercountryflags.common.util.FlagPosition;
import me.khajiitos.servercountryflags.common.util.FlagRenderInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(ServerBrowserList.BrowsedEntry.class)
public class BrowsedEntryMixin {
    @Shadow(remap = false)
    private ServerData serverData;

    @Shadow(remap = false)
    @Final
    private ServerBrowserScreen screen;

    @Unique
    private static void servercountryflags$renderOutline(PoseStack poseStack, int x, int y, int width, int height, int color) {
        GuiComponent.fill(poseStack, x, y, x + width, y + 1, color);
        GuiComponent.fill(poseStack, x, y + height - 1, x + width, y + height, color);
        GuiComponent.fill(poseStack, x, y + 1, x + 1, y + height - 1, color);
        GuiComponent.fill(poseStack, x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    @Shadow(remap = false)
    @Final
    private Minecraft minecraft;

    // Suppressing all warnings - don't know what the exact name is
    // Minecraft Dev extension complains that it can't find the "render" function,
    // because the project we are working with here is obfuscated
    // When compiling on Forge/Fabric, the mod is deobfuscated there, so it works
    @SuppressWarnings("all")
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I", ordinal = 0), method = "render", index = 2)
    public float serverNameX(float oldX) {
        if (!Config.cfg.serverBrowserIntegration) {
            return oldX;
        }

        if (Config.cfg.flagPosition == FlagPosition.BEHIND_NAME) {
            APIResponse apiResponse = ServerCountryFlags.servers.get(serverData.ip);
            FlagRenderInfo renderInfo = ServerCountryFlags.getFlagRenderInfo(apiResponse);

            if (renderInfo != null) {
                return oldX + (float)renderInfo.flagAspectRatio() * 8.f + 3;
            }
        }

        return oldX;
    }

    @SuppressWarnings("all")
    @Inject(at = @At("TAIL"), method = "render")
    public void render(PoseStack poseStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        if (!Config.cfg.serverBrowserIntegration) {
            return;
        }

        APIResponse apiResponse = ServerCountryFlags.servers.get(serverData.ip);
        FlagRenderInfo flagRenderInfo = ServerCountryFlags.getFlagRenderInfo(apiResponse);

        if (flagRenderInfo == null) {
            return;
        }

        if (Config.cfg.flagPosition == FlagPosition.TOOLTIP_SERVER_NAME) {
            int serverNameStartX = x + 35;
            int serverNameStartY = y + 1;

            int serverNameWidth = this.minecraft.font.width(this.serverData.name);
            int serverNameHeight = 8;

            if (mouseX >= serverNameStartX && mouseX <= serverNameStartX + serverNameWidth && mouseY >= serverNameStartY && mouseY <= serverNameStartY + serverNameHeight) {
                screen.setToolTip(flagRenderInfo.tooltip());
            }

            // TODO: maybe render the flag in the future
            // But I'm too lazy to figure out how to do that.
            return;
        } else if (Config.cfg.flagPosition == FlagPosition.TOOLTIP_PING) {
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

        ResourceLocation textureId = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/flags/" + flagRenderInfo.countryCode() + ".png");

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, textureId);
        GuiComponent.blit(poseStack, startingX, startingY, 0.0F, 0.0F, width, height, width, height);

        if (Config.cfg.flagBorder) {
            servercountryflags$renderOutline(poseStack, startingX - 1, startingY - 1, width + 2, height + 2, Config.cfg.borderColor.toARGB());
        }

        RenderSystem.disableBlend();

        if (mouseX >= startingX && mouseX <= startingX + width && mouseY >= startingY && mouseY <= startingY + height) {
            screen.setToolTip(flagRenderInfo.tooltip());
        }
    }

    @SuppressWarnings("all")
    @Inject(at = @At(value = "INVOKE", target = "Lcom/epherical/serverbrowser/client/screen/ServerBrowserScreen;setToolTip(Ljava/util/List;)V", ordinal = 1, shift = At.Shift.AFTER), method = "render", remap = false)
    public void onSetTooltip(PoseStack poseStack, int $$1, int $$2, int $$3, int $$4, int $$5, int $$6, int $$7, boolean $$8, float $$9, CallbackInfo ci) {
        if (Config.cfg.flagPosition == FlagPosition.TOOLTIP_PING) {
            ServerBrowserScreenAccessor accessor = (ServerBrowserScreenAccessor) screen;

            APIResponse apiResponse = ServerCountryFlags.servers.get(serverData.ip);
            FlagRenderInfo flagRenderInfo = ServerCountryFlags.getFlagRenderInfo(apiResponse);

            if (flagRenderInfo == null) {
                return;
            }

            List<Component> newTooltip = new ArrayList<>(accessor.getToolTip());
            newTooltip.add(Component.literal(" "));
            newTooltip.addAll(flagRenderInfo.tooltip());
            screen.setToolTip(newTooltip);
        }
    }
}
