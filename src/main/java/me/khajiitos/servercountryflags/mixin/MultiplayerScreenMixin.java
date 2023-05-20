package me.khajiitos.servercountryflags.mixin;

import me.khajiitos.servercountryflags.ServerCountryFlags;
import me.khajiitos.servercountryflags.config.Config;
import me.khajiitos.servercountryflags.screen.ServerMapScreen;
import me.khajiitos.servercountryflags.util.Compatibility;
import me.khajiitos.servercountryflags.util.NetworkChangeDetector;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen {
    private static final Identifier MAP_BUTTON_TEXTURE = new Identifier(ServerCountryFlags.MOD_ID, "textures/misc/map_button.png");

    @Shadow
    private ServerList serverList;

    private MultiplayerScreenMixin(Text title) {
        super(title);
    }

    private void updateServers() {
        for (int i = 0; i < this.serverList.size(); i++) {
            if (!ServerCountryFlags.servers.containsKey(this.serverList.get(i).address)) {
                ServerCountryFlags.updateServerLocationInfo(this.serverList.get(i).address);
            }
        }
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
        ServerCountryFlags.serverList = serverList;
        updateServers();

        if (Config.mapButton) {
            int posX = this.width / 2 + 159;
            int posY = this.height - 28;
            if (ServerCountryFlags.isNewerThan1_19_3) {
                // Minecraft 1.19.4 slightly changed the positions of buttons in the Multiplayer Screen
                posY = this.height - 30;
            }
            this.addDrawableChild(new TexturedButtonWidget(posX, posY, 20, 20, 0, 0, 20, MAP_BUTTON_TEXTURE, 20, 40, (button) -> Compatibility.setScreen(new ServerMapScreen(this))));
        }
    }
}
