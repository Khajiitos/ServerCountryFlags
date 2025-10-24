package me.khajiitos.servercountryflags.common.mixin;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.screen.ServerMapScreen;
import me.khajiitos.servercountryflags.common.util.APIResponse;
import me.khajiitos.servercountryflags.common.util.NetworkChangeDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
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
            ResourceLocation.fromNamespaceAndPath(ServerCountryFlags.MOD_ID, "widget/map_button"),
            ResourceLocation.fromNamespaceAndPath(ServerCountryFlags.MOD_ID, "widget/map_button_focused")
    );

    @Unique
    private static final WidgetSprites MAP_BUTTON_SPRITES_HIGH_CONTRAST = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath(ServerCountryFlags.MOD_ID, "widget/map_button_high_contrast"),
            ResourceLocation.fromNamespaceAndPath(ServerCountryFlags.MOD_ID, "widget/map_button_focused_high_contrast")
    );

    @Unique
    private ImageButton servercountryflags$serverMapButton = null;

    @Shadow
    private ServerList servers;

    @Shadow
    @Final
    private HeaderAndFooterLayout layout;

    @Shadow
    private Button editButton;

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

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/multiplayer/JoinMultiplayerScreen;repositionElements()V"), method = "init")
    public void init(CallbackInfo info) {
        ServerCountryFlags.serverList = this.servers;

        for (int i = 0; i < this.servers.size(); i++) {
            if (!ServerCountryFlags.servers.containsKey(this.servers.get(i).ip) || ServerCountryFlags.servers.get(this.servers.get(i).ip).status() != APIResponse.Status.SUCCESS) {
                ServerCountryFlags.updateServerLocationInfo(this.servers.get(i).ip);
            }
        }

        if (Config.cfg.mapButton) {
            // Setting x and y to 0, they will be set instantly in repositionElements
            servercountryflags$serverMapButton = new ImageButton(0, 0, 20, 20, Minecraft.getInstance().options.highContrast().get() ? MAP_BUTTON_SPRITES_HIGH_CONTRAST : MAP_BUTTON_SPRITES, (button) -> Minecraft.getInstance().setScreen(new ServerMapScreen(this)));
            this.addRenderableWidget(servercountryflags$serverMapButton);
        }
    }

    @Inject(at = @At("TAIL"), method = "repositionElements")
    protected void repositionElements(CallbackInfo info) {
        if (servercountryflags$serverMapButton != null) {
            int posX = this.width / 2 + (Config.cfg.mapButtonRight ? 159 : -179);
            int posY = this.height - 28;
            servercountryflags$serverMapButton.setPosition(posX, posY);
        }
    }
}
