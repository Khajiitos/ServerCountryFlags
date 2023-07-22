package me.khajiitos.servercountryflags.forge.mixin;

import me.khajiitos.servercountryflags.forge.ServerCountryFlags;
import me.khajiitos.servercountryflags.forge.config.Config;
import me.khajiitos.servercountryflags.forge.screen.ServerMapScreen;
import me.khajiitos.servercountryflags.forge.util.NetworkChangeDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class JoinMultiplayerScreenMixin extends Screen {
    private static final ResourceLocation MAP_BUTTON_TEXTURE = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/misc/map_button.png");

    @Shadow
    private ServerList servers;

    private JoinMultiplayerScreenMixin(TextComponent title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void constructor(CallbackInfo info) {
        if (Config.reloadOnRefresh) {
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
            if (!ServerCountryFlags.servers.containsKey(this.servers.get(i).ip)) {
                ServerCountryFlags.updateServerLocationInfo(this.servers.get(i).ip);
            }
        }

        if (Config.mapButton) {
            int posX = this.width / 2 + (Config.mapButtonRight ? 159 : -179);
            int posY = this.height - 28;
            this.addButton(new ImageButton(posX, posY, 20, 20, 0, 0, 20, MAP_BUTTON_TEXTURE, 20, 40, (button) -> Minecraft.getInstance().setScreen(new ServerMapScreen(this))));
        }
    }
}
