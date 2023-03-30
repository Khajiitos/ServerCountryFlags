package me.khajiitos.servercountryflags.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.khajiitos.servercountryflags.Config;
import me.khajiitos.servercountryflags.LocationInfo;
import me.khajiitos.servercountryflags.ServerCountryFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public class ServerEntryMixin {

    @Shadow
    @Final
    private ServerInfo server;

    @Shadow
    @Final
    private MultiplayerScreen screen;

    private static boolean printedError = false;

    private static String originalName;

    @Inject(at = @At("HEAD"), method = "render")
    public void renderHead(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        if (!ServerCountryFlags.flagAspectRatiosLoaded) {
            ServerCountryFlags.LOGGER.error("In the server list before the flags were loaded?");
            return;
        }

        originalName = this.server.name;
        if (Config.flagPosition.equalsIgnoreCase("behindName")) {
            this.server.name = "";
        }
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        if (!ServerCountryFlags.flagAspectRatiosLoaded) {
            return;
        }

        String toolTip = null;
        String countryCode = "unknown";
        LocationInfo locationInfo = ServerCountryFlags.servers.get(server.address);

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
                this.server.name = originalName;
                MinecraftClient.getInstance().textRenderer.draw(matrices, this.server.name, (float)(x + 35), (float)(y + 1), 16777215);
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
                this.server.name = originalName;
                MinecraftClient.getInstance().textRenderer.draw(matrices, this.server.name, (float)(startingX + width + 3), (float)(y + 1), 16777215);
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

        Identifier textureId = new Identifier(ServerCountryFlags.MOD_ID, "textures/flags/" + countryCode + ".png");

        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.enableBlend();
        DrawableHelper.drawTexture(matrices, startingX, startingY, 0.0F, 0.0F, width, height, width, height);
        if (Config.flagBorder) {
            final int color = (Config.borderR << 16) | (Config.borderG << 8) | Config.borderB | (Config.borderA << 24);
            DrawableHelper.drawBorder(matrices, startingX - 1, startingY - 1, width + 2, height + 2, color);
        }
        RenderSystem.disableBlend();
        if (mouseX >= startingX && mouseX <= startingX + width && mouseY >= startingY && mouseY <= startingY + height) {
            List<Text> toolTipList = new ArrayList<>();
            toolTipList.add(toolTip != null ? Text.literal(toolTip) : Text.translatable("locationInfo.unknown"));
            if (locationInfo != null) {
                if (Config.showISP && !locationInfo.ispName.equals("")) {
                    toolTipList.add(Text.translatable("locationInfo.isp", locationInfo.ispName));
                }
                if (Config.showDistance) {
                    double distanceFromLocal = locationInfo.getDistanceFromLocal(Config.useKm);
                    if (distanceFromLocal != -1.0) {
                        toolTipList.add(Text.translatable("locationInfo.distance", (int)distanceFromLocal, Text.translatable(Config.useKm ? "locationInfo.km" : "locationInfo.mi")).formatted(Formatting.ITALIC));
                    }
                }
            }
            screen.setMultiplayerScreenTooltip(toolTipList);
        }
    }
}
