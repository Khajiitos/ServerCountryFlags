package me.khajiitos.servercountryflags.forge.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.khajiitos.servercountryflags.forge.ServerCountryFlags;
import me.khajiitos.servercountryflags.forge.config.Config;
import me.khajiitos.servercountryflags.forge.util.LocationInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.ServerSelectionList;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerSelectionList.NormalEntry.class)
public class OnlineServerEntryMixin {

    @Shadow
    @Final
    private ServerData serverData;

    @Shadow
    @Final
    private MultiplayerScreen screen;

    @Unique
    private static boolean printedError = false;

    @Unique
    private static String originalName;

    @Unique
    private static void drawBorder(MatrixStack matrices, int x, int y, int width, int height, int color) {
        // In Minecraft versions older than 1.19.4 function DrawableHelper.drawBorder doesn't exist
        Widget.fill(matrices, x, y, x + width, y + 1, color);
        Widget.fill(matrices, x, y + height - 1, x + width, y + height, color);
        Widget.fill(matrices, x, y + 1, x + 1, y + height - 1, color);
        Widget.fill(matrices, x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    @Inject(at = @At("HEAD"), method = "render")
    public void renderHead(MatrixStack poseStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        if (!ServerCountryFlags.flagAspectRatiosLoaded) {
            return;
        }

        originalName = this.serverData.name;
        if (Config.flagPosition.equalsIgnoreCase("behindName")) {
            this.serverData.name = "";
        }
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(MatrixStack poseStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
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
                ServerCountryFlags.LOGGER.severe("Somehow a server has a failed ServerLocationInfo associated to it?");
                return;
            }
        } else if (!Config.displayUnknownFlag) {
            if (Config.flagPosition.equalsIgnoreCase("behindname")) {
                this.serverData.name = originalName;
                Minecraft.getInstance().font.draw(poseStack, this.serverData.name, x + 35, y + 1, 16777215);
            }
            return;
        }

        int height = 12;
        double aspect = ServerCountryFlags.flagAspectRatios.get(countryCode);
        int width = (int)(aspect * height);
        int startingX, startingY;

        String flagPosition = Config.flagPosition.toLowerCase();
        switch (flagPosition) {
            case "left":
                startingX = x - width - 6;
                startingY = y + (entryHeight / 2) - (height / 2);
                break;
            case "right":
                startingX = x + entryWidth + 10;
                startingY = y + (entryHeight / 2) - (height / 2);
                break;
            case "behindname":
                height = 8;
                width = (int) (aspect * height);
                startingX = x + 35;
                startingY = y + 1;
                this.serverData.name = originalName;
                Minecraft.getInstance().font.draw(poseStack, this.serverData.name, startingX + width + 3, y + 1, 16777215);
                break;
            default:
                startingX = x + entryWidth - width - 6;
                startingY = y + entryHeight - height - 4;
                break;
        }


        if (!ServerCountryFlags.flagAspectRatios.containsKey(countryCode)) {
            if (!printedError) {
                ServerCountryFlags.LOGGER.severe("ERROR: Unsupported country code: " + countryCode);
                printedError = true;
            }
            countryCode = "unknown";
        }

        ResourceLocation textureId = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/flags/" + countryCode + ".png");

        RenderSystem.enableBlend();

        Minecraft.getInstance().getTextureManager().bind(textureId);
        Widget.blit(poseStack, startingX, startingY, 0.0F, 0.0F, width, height, width, height);
        if (Config.flagBorder) {
            final int color = (Config.borderR << 16) | (Config.borderG << 8) | Config.borderB | (Config.borderA << 24);
            drawBorder(poseStack, startingX - 1, startingY - 1, width + 2, height + 2, color);
        }
        RenderSystem.disableBlend();
        if (mouseX >= startingX && mouseX <= startingX + width && mouseY >= startingY && mouseY <= startingY + height) {
            List<ITextComponent> toolTipList = new ArrayList<>();

            toolTipList.add(toolTip != null ? new StringTextComponent(toolTip) : new TranslationTextComponent("locationInfo.unknown"));
            if (locationInfo != null) {
                if (Config.showISP && !locationInfo.ispName.equals("")) {
                    toolTipList.add(new TranslationTextComponent("locationInfo.isp", locationInfo.ispName));
                }
                if (Config.showDistance) {
                    double distanceFromLocal = locationInfo.getDistanceFromLocal(Config.useKm);
                    if (distanceFromLocal != -1.0) {
                        toolTipList.add(new TranslationTextComponent("locationInfo.distance", (int)distanceFromLocal, new TranslationTextComponent(Config.useKm ? "locationInfo.km" : "locationInfo.mi")).withStyle(TextFormatting.ITALIC));
                    }
                }
            }
            screen.setToolTip(toolTipList);
        }
    }
}
