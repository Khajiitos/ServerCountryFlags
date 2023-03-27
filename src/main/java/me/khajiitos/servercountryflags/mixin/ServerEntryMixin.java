package me.khajiitos.servercountryflags.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.khajiitos.servercountryflags.Config;
import me.khajiitos.servercountryflags.ServerCountryFlags;
import me.khajiitos.servercountryflags.LocationInfo;
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

    @Inject(at = @At("TAIL"), method = "render")
    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        String toolTip = null;
        String countryCode = "unknown";
        LocationInfo locationInfo = ServerCountryFlags.servers.get(server.address);

        if (locationInfo != null) {
            if (locationInfo.success) {
                toolTip = locationInfo.cityName + ", " + locationInfo.countryName;
                countryCode = locationInfo.countryCode;
            } else {
                ServerCountryFlags.LOGGER.error("Somehow a server has a failed ServerLocationInfo associated to it?");
                return;
            }
        }

        if (!ServerCountryFlags.flagAspectRatios.containsKey(countryCode)) {
            if (!printedError) {
                ServerCountryFlags.LOGGER.error("ERROR: Unsupported country code: " + countryCode);
                printedError = true;
            }
            countryCode = "unknown";
        }

        Identifier textureId = new Identifier("servercountryflags", "textures/flags/" + countryCode + ".png");

        int height = 12;
        int width = (int)(ServerCountryFlags.flagAspectRatios.get(countryCode) * height);

        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.enableBlend();
        DrawableHelper.drawTexture(matrices, x + entryWidth - width - 6, y + entryHeight - height - 4, 0.0F, 0.0F, width, height, width, height);
        if (Config.flagBorder) {
            final int color = (Config.borderR << 16) | (Config.borderG << 8) | Config.borderB | (Config.borderA << 24);
            DrawableHelper.drawBorder(matrices, x + entryWidth - width - 7, y + entryHeight - height - 5, width + 2, height + 2, color);
        }
        RenderSystem.disableBlend();

        if (mouseX >= x + entryWidth - width - 6 && mouseX <= x + entryWidth - 6 && mouseY >= y + entryHeight - height - 4 && mouseY <= y + entryHeight - 4) {
            List<Text> toolTipList = new ArrayList<>();
            toolTipList.add(toolTip != null ? Text.literal(toolTip) : Text.translatable("locationInfo.unknown"));
            if (Config.showDistance && locationInfo != null && locationInfo.hasLonlat()) {
                double distanceFromLocal = locationInfo.getDistanceFromLocal(Config.useKm);
                if (distanceFromLocal != -1.0) {
                    toolTipList.add(Text.translatable("locationInfo.distance", (int)distanceFromLocal, Text.translatable(Config.useKm ? "locationInfo.km" : "locationInfo.mi")).formatted(Formatting.ITALIC));
                }
            }
            screen.setMultiplayerScreenTooltip(toolTipList);
        }
    }
}
