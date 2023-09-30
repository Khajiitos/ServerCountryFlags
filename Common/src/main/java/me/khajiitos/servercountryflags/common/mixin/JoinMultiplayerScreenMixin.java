package me.khajiitos.servercountryflags.common.mixin;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.screen.ServerMapScreen;
import me.khajiitos.servercountryflags.common.util.APIResponse;
import me.khajiitos.servercountryflags.common.util.NetworkChangeDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class JoinMultiplayerScreenMixin extends Screen {
    @Unique
    private static final WidgetSprites MAP_BUTTON_SPRITES = new WidgetSprites(
            new ResourceLocation(ServerCountryFlags.MOD_ID, "widget/map_button"),
            new ResourceLocation(ServerCountryFlags.MOD_ID, "widget/map_button_focused")
    );

    @Unique
    private static final WidgetSprites MAP_BUTTON_SPRITES_HIGH_CONTRAST = new WidgetSprites(
            new ResourceLocation(ServerCountryFlags.MOD_ID, "widget/map_button_high_contrast"),
            new ResourceLocation(ServerCountryFlags.MOD_ID, "widget/map_button_focused_high_contrast")
    );

    @Shadow
    private ServerList servers;

    private JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void constructor(CallbackInfo info) {
        if (Config.cfg.reloadOnRefresh) {
            ServerCountryFlags.servers.clear();
            ServerCountryFlags.localLocation = null;
        }

        if (ServerCountryFlags.localLocation == null || NetworkChangeDetector.check()) {
            ServerCountryFlags.updateLocalLocationInfo();
        }
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo info) {
        ServerCountryFlags.serverList = this.servers;

        for (int i = 0; i < this.servers.size(); i++) {
            if (!ServerCountryFlags.servers.containsKey(this.servers.get(i).ip) || ServerCountryFlags.servers.get(this.servers.get(i).ip).status() != APIResponse.Status.SUCCESS) {
                ServerCountryFlags.updateServerLocationInfo(this.servers.get(i).ip);
            }
        }

        if (Config.cfg.mapButton) {
            int posX = this.width / 2 + (Config.cfg.mapButtonRight ? 159 : -179);
            int posY = this.height - 30;
            this.addRenderableWidget(new ImageButton(posX, posY, 20, 20, Minecraft.getInstance().options.highContrast().get() ? MAP_BUTTON_SPRITES_HIGH_CONTRAST : MAP_BUTTON_SPRITES, (button) -> Minecraft.getInstance().setScreen(new ServerMapScreen(this))));
        }
    }
}
