package me.khajiitos.servercountryflags.common.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.LocationInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

    private static boolean printedError = false;

    private static String originalName;

    @Inject(at = @At("HEAD"), method = "render")
    public void renderHead(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        if (!ServerCountryFlags.flagAspectRatiosLoaded) {
            return;
        }

        originalName = this.serverData.name;
        if (Config.flagPosition.equalsIgnoreCase("behindName")) {
            this.serverData.name = "";
        }
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        if (!ServerCountryFlags.flagAspectRatiosLoaded) {
            return;
        }

        String toolTip = null;
        String countryCode = "unknown";
        LocationInfo locationInfo = ServerCountryFlags.servers.get(serverData.ip);

        if (locationInfo != null) {
            if (locationInfo.success) {
                toolTip = (Config.showDistrict && !locationInfo.districtName.equals("") ? (locationInfo.districtName + ", ") : "") + locationInfo.cityName + ", " + locationInfo.countryName;
                countryCode = locationInfo.countryCode;
            } else {
                ServerCountryFlags.LOGGER.error("Somehow a server has a failed ServerLocationInfo associated to it?");
                return;
            }
        } else if (!Config.displayUnknownFlag) {
            if (Config.flagPosition.equalsIgnoreCase("behindname")) {
                this.serverData.name = originalName;
                context.drawString(Minecraft.getInstance().font, this.serverData.name, x + 35, y + 1, 16777215, false);
            }
            return;
        }

        int height = 12;
        double aspect = ServerCountryFlags.flagAspectRatios.get(countryCode);
        int width = (int)(aspect * height);
        int startingX, startingY;

        switch (Config.flagPosition.toLowerCase()) {
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
                context.drawString(Minecraft.getInstance().font, this.serverData.name, startingX + width + 3, y + 1, 16777215, false);
            }
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
            countryCode = "unknown";
        }

        ResourceLocation textureId = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/flags/" + countryCode + ".png");

        RenderSystem.enableBlend();
        context.blit(textureId, startingX, startingY, 0.0F, 0.0F, width, height, width, height);
        if (Config.flagBorder) {
            final int color = (Config.borderR << 16) | (Config.borderG << 8) | Config.borderB | (Config.borderA << 24);
            context.renderOutline(startingX - 1, startingY - 1, width + 2, height + 2, color);
        }
        RenderSystem.disableBlend();
        if (mouseX >= startingX && mouseX <= startingX + width && mouseY >= startingY && mouseY <= startingY + height) {
            List<Component> toolTipList = new ArrayList<>();

            toolTipList.add(toolTip != null ? Component.literal(toolTip) : Component.translatable("locationInfo.unknown"));
            if (locationInfo != null) {
                if (Config.showISP && !locationInfo.ispName.equals("")) {
                    toolTipList.add(Component.translatable("locationInfo.isp", locationInfo.ispName));
                }
                if (Config.showDistance) {
                    double distanceFromLocal = locationInfo.getDistanceFromLocal(Config.useKm);
                    if (distanceFromLocal != -1.0) {
                        toolTipList.add(Component.translatable("locationInfo.distance", (int)distanceFromLocal, Component.translatable(Config.useKm ? "locationInfo.km" : "locationInfo.mi")).withStyle(ChatFormatting.ITALIC));
                    }
                }
            }
            screen.setToolTip(toolTipList);
        }
    }
}
