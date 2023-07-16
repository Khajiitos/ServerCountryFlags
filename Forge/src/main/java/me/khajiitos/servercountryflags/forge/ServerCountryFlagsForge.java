package me.khajiitos.servercountryflags.forge;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import net.minecraftforge.fml.common.Mod;

@Mod(ServerCountryFlags.MOD_ID)
public class ServerCountryFlagsForge {
    public ServerCountryFlagsForge() {
        ServerCountryFlags.init();
    }
}