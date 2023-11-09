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
import me.khajiitos.servercountryflags.common.util.LocationInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
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

    @Shadow(remap = false) @Final private ServerBrowserScreen screen;

    // Suppressing all warnings - don't know what the exact name is
    // Minecraft Dev extension complains that it can't find the "render" function,
    // because the project we are working with here is obfuscated
    // When compiling on Forge/Fabric, the mod is deobfuscated there, so it works
    @SuppressWarnings("all")
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I", ordinal = 0), method = "render", index = 2)
    public float serverNameX(float oldX) {
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

        ResourceLocation textureId = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/flags/" + flagRenderInfo.countryCode() + ".png");

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, textureId);
        GuiComponent.blit(poseStack, startingX, startingY, 0.0F, 0.0F, width, height, width, height);

        if (Config.cfg.flagBorder) {
            GuiComponent.renderOutline(poseStack, startingX - 1, startingY - 1, width + 2, height + 2, Config.cfg.borderColor.toARGB());
        }

        RenderSystem.disableBlend();
        if (mouseX >= startingX && mouseX <= startingX + width && mouseY >= startingY && mouseY <= startingY + height) {
            List<Component> toolTipList = new ArrayList<>();
            toolTipList.add(flagRenderInfo.tooltip());

            LocationInfo locationInfo = apiResponse.locationInfo();
            if (locationInfo != null) {
                if (Config.cfg.showISP && !locationInfo.ispName.equals("")) {
                    toolTipList.add(Component.translatable("servercountryflags.locationInfo.isp", locationInfo.ispName));
                }
                if (Config.cfg.showDistance) {
                    double distanceFromLocal = locationInfo.getDistanceFromLocal(Config.cfg.useKm);
                    if (distanceFromLocal != -1.0) {
                        toolTipList.add(Component.translatable("servercountryflags.locationInfo.distance", (int)distanceFromLocal, Component.translatable(Config.cfg.useKm ? "servercountryflags.locationInfo.km" : "servercountryflags.locationInfo.mi")).withStyle(ChatFormatting.ITALIC));
                    }
                }
            }
            screen.setToolTip(toolTipList);
        }
    }
}
