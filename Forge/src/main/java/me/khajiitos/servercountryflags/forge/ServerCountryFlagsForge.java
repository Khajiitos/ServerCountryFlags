package me.khajiitos.servercountryflags.forge;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(ServerCountryFlags.MOD_ID)
public class ServerCountryFlagsForge {
    public ServerCountryFlagsForge() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ServerCountryFlags::init);
    }
}