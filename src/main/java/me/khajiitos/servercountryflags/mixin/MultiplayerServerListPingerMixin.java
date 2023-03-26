package me.khajiitos.servercountryflags.mixin;

import me.khajiitos.servercountryflags.Config;
import me.khajiitos.servercountryflags.ServerCountryFlags;
import me.khajiitos.servercountryflags.LocationInfo;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.net.InetSocketAddress;
import java.util.Optional;

@Mixin(MultiplayerServerListPinger.class)
public class MultiplayerServerListPingerMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    public void constructor(CallbackInfo info) {
        if (Config.reloadOnRefresh) {
            ServerCountryFlags.servers.clear();
        }
    }

    @Inject(method = "add", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void afterAdd(final ServerInfo entry, final Runnable saver, final CallbackInfo info, final ServerAddress sa, Optional o, InetSocketAddress inetSocketAddress) {
        String ip = inetSocketAddress.getAddress().getHostAddress();

        if (ServerCountryFlags.servers.containsKey(ip))
            return;

        LocationInfo locationInfo = ServerCountryFlags.getServerLocationInfo(ip, Config.showDistance);
        if (locationInfo != null && locationInfo.success) {
            ServerCountryFlags.servers.put(entry.address, locationInfo);
        } else {
            ServerCountryFlags.servers.put(entry.address, null);
        }
    }
}
