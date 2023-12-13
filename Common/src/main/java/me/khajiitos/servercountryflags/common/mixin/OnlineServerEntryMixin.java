package me.khajiitos.servercountryflags.common.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.APIResponse;
import me.khajiitos.servercountryflags.common.util.FlagPosition;
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
    private static String originalName;

    @Inject(at = @At("HEAD"), method = "render")
    public void renderHead(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        if (!ServerCountryFlags.flagAspectRatiosLoaded) {
            return;
        }

        originalName = this.serverData.name;
        if (Config.cfg.flagPosition == FlagPosition.BEHIND_NAME) {
            this.serverData.name = "";
        }
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
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
                if (!Config.cfg.displayCooldownFlag && Config.cfg.flagPosition == FlagPosition.BEHIND_NAME) {
                    this.serverData.name = originalName;
                    context.drawString(Minecraft.getInstance().font, this.serverData.name, x + 35, y + 1, 16777215, false);
                    return;
                }
                toolTip = Component.translatable("servercountryflags.locationInfo.cooldown");
                countryCode = "timeout";
            } else if (apiResponse.unknown()) {
                if (!Config.cfg.displayUnknownFlag && Config.cfg.flagPosition == FlagPosition.BEHIND_NAME) {
                    this.serverData.name = originalName;
                    context.drawString(Minecraft.getInstance().font, this.serverData.name, x + 35, y + 1, 16777215, false);
                    return;
                }
                toolTip = Component.translatable("servercountryflags.locationInfo.unknown");
                countryCode = "unknown";
            } else {
                toolTip = Component.literal((Config.cfg.showDistrict && !locationInfo.districtName.equals("") ? (locationInfo.districtName + ", ") : "") + locationInfo.cityName + ", " + locationInfo.countryName);
                countryCode = locationInfo.countryCode;
            }
        } else {
            if (!Config.cfg.displayUnknownFlag && Config.cfg.flagPosition == FlagPosition.BEHIND_NAME) {
                this.serverData.name = originalName;
                context.drawString(Minecraft.getInstance().font, this.serverData.name, x + 35, y + 1, 16777215, false);
                return;
            }
            toolTip = Component.translatable("servercountryflags.locationInfo.unknown");
            countryCode = "unknown";
        }

        int height = 12;

        double aspect;
        if (ServerCountryFlags.flagAspectRatios.containsKey(countryCode)) {
            aspect = ServerCountryFlags.flagAspectRatios.get(countryCode);
        } else {
            if (!ServerCountryFlags.unknownCountryCodes.contains(countryCode)) {
                ServerCountryFlags.LOGGER.error("Unknown country code: " + countryCode);
                ServerCountryFlags.unknownCountryCodes.add(countryCode);
            }

            countryCode = "unknown";
            aspect = 1.5;
        }

        int width = (int)(aspect * height);
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

        ResourceLocation textureId = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/gui/flags/" + countryCode + ".png");

        RenderSystem.enableBlend();
        context.blit(textureId, startingX, startingY, 0.0F, 0.0F, width, height, width, height);
        if (Config.cfg.flagBorder) {
            context.renderOutline(startingX - 1, startingY - 1, width + 2, height + 2, Config.cfg.borderColor.toARGB());
        }
        RenderSystem.disableBlend();
        if (mouseX >= startingX && mouseX <= startingX + width && mouseY >= startingY && mouseY <= startingY + height) {
            List<Component> toolTipList = new ArrayList<>();
            toolTipList.add(toolTip);

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
