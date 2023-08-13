package me.khajiitos.servercountryflags.common.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.APIResponse;
import me.khajiitos.servercountryflags.common.util.LocationInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public class OnlineServerEntryMixin {

    @Shadow
    @Final
    private ServerData serverData;

    @Shadow
    @Final
    private JoinMultiplayerScreen screen;

    @Unique
    private static boolean printedError = false;

    @Unique
    private static String originalName;

    @Unique
    private static void renderOutline(PoseStack poseStack, int x, int y, int width, int height, int color) {
        GuiComponent.fill(poseStack, x, y, x + width, y + 1, color);
        GuiComponent.fill(poseStack, x, y + height - 1, x + width, y + height, color);
        GuiComponent.fill(poseStack, x, y + 1, x + 1, y + height - 1, color);
        GuiComponent.fill(poseStack, x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    @Inject(at = @At("HEAD"), method = "render")
    public void renderHead(PoseStack poseStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        if (!ServerCountryFlags.flagAspectRatiosLoaded) {
            return;
        }

        originalName = this.serverData.name;
        if (Config.cfg.flagPosition.equalsIgnoreCase("behindName")) {
            this.serverData.name = "";
        }
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(PoseStack poseStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        if (!ServerCountryFlags.flagAspectRatiosLoaded) {
            return;
        }

        Component toolTip;
        String countryCode;
        APIResponse apiResponse = ServerCountryFlags.servers.get(serverData.ip);
        LocationInfo locationInfo = null;

        if (apiResponse != null) {
            locationInfo = apiResponse.locationInfo();
            if (apiResponse.cooldown()) {
                if (!Config.cfg.displayCooldownFlag && Config.cfg.flagPosition.equalsIgnoreCase("behindname")) {
                    this.serverData.name = originalName;
                    Minecraft.getInstance().font.draw(poseStack, this.serverData.name, x + 35, y + 1, 16777215);
                    return;
                }
                toolTip = Component.translatable("locationInfo.cooldown");
                countryCode = "timeout";
            } else if (apiResponse.unknown()) {
                if (!Config.cfg.displayUnknownFlag && Config.cfg.flagPosition.equalsIgnoreCase("behindname")) {
                    this.serverData.name = originalName;
                    Minecraft.getInstance().font.draw(poseStack, this.serverData.name, x + 35, y + 1, 16777215);
                    return;
                }
                toolTip = Component.translatable("locationInfo.unknown");
                countryCode = "unknown";
            } else {
                toolTip = Component.literal((Config.cfg.showDistrict && !locationInfo.districtName.equals("") ? (locationInfo.districtName + ", ") : "") + locationInfo.cityName + ", " + locationInfo.countryName);
                countryCode = locationInfo.countryCode;
            }
        } else {
            if (!Config.cfg.displayUnknownFlag && Config.cfg.flagPosition.equalsIgnoreCase("behindname")) {
                this.serverData.name = originalName;
                Minecraft.getInstance().font.draw(poseStack, this.serverData.name, x + 35, y + 1, 16777215);                return;
            }
            toolTip = Component.translatable("locationInfo.unknown");
            countryCode = "unknown";
        }

        int height = 12;
        double aspect = ServerCountryFlags.flagAspectRatios.get(countryCode);
        int width = (int)(aspect * height);
        int startingX, startingY;

        switch (Config.cfg.flagPosition.toLowerCase()) {
            case "left" -> {
                startingX = x - width - 6;
                startingY = y + (entryHeight / 2) - (height / 2);
            }
            case "right" -> {
                startingX = x + entryWidth + 10;
                startingY = y + (entryHeight / 2) - (height / 2);
            }
            case "behindname" -> {
                height = 8;
                width = (int) (aspect * height);
                startingX = x + 35;
                startingY = y + 1;
                this.serverData.name = originalName;
                Minecraft.getInstance().font.draw(poseStack, this.serverData.name, startingX + width + 3, y + 1, 16777215);            }
            default -> {
                startingX = x + entryWidth - width - 6;
                startingY = y + entryHeight - height - 4;
            }
        }

        if (!ServerCountryFlags.flagAspectRatios.containsKey(countryCode)) {
            if (!printedError) {
                ServerCountryFlags.LOGGER.error("ERROR: Unsupported country code: " + countryCode);
                printedError = true;
            }
            toolTip = Component.translatable("locationInfo.unknown");
            countryCode = "unknown";
        }

        ResourceLocation textureId = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/flags/" + countryCode + ".png");

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, textureId);
        GuiComponent.blit(poseStack, startingX, startingY, 0.0F, 0.0F, width, height, width, height);
        if (Config.cfg.flagBorder) {
            final int color = (Config.cfg.borderR << 16) | (Config.cfg.borderG << 8) | Config.cfg.borderB | (Config.cfg.borderA << 24);
            renderOutline(poseStack, startingX - 1, startingY - 1, width + 2, height + 2, color);
        }
        RenderSystem.disableBlend();
        if (mouseX >= startingX && mouseX <= startingX + width && mouseY >= startingY && mouseY <= startingY + height) {
            List<Component> toolTipList = new ArrayList<>();
            toolTipList.add(toolTip);

            if (locationInfo != null) {
                if (Config.cfg.showISP && !locationInfo.ispName.equals("")) {
                    toolTipList.add(Component.translatable("locationInfo.isp", locationInfo.ispName));
                }
                if (Config.cfg.showDistance) {
                    double distanceFromLocal = locationInfo.getDistanceFromLocal(Config.cfg.useKm);
                    if (distanceFromLocal != -1.0) {
                        toolTipList.add(Component.translatable("locationInfo.distance", (int)distanceFromLocal, Component.translatable(Config.cfg.useKm ? "locationInfo.km" : "locationInfo.mi")).withStyle(ChatFormatting.ITALIC));
                    }
                }
            }
            screen.setToolTip(toolTipList);
        }
    }
}
