package me.khajiitos.servercountryflags.neoforged;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.ClothConfigCheck;
import me.khajiitos.servercountryflags.common.config.ClothConfigScreenMaker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.ConfigScreenHandler;

@Mod(ServerCountryFlags.MOD_ID)
public class ServerCountryFlagsNeoforged {
    public ServerCountryFlagsNeoforged() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ServerCountryFlags.init();

            if (ClothConfigCheck.isInstalled()) {
                ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory(ClothConfigScreenMaker::create));
            }
        });
    }
}