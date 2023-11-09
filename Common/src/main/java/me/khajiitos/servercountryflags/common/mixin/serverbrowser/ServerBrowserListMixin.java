package me.khajiitos.servercountryflags.common.mixin.serverbrowser;


import com.epherical.serverbrowser.client.list.ServerBrowserList;
import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.APIResponse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(ServerBrowserList.class)
public class ServerBrowserListMixin {
    @Shadow(remap = false) @Final private List<ServerBrowserList.BrowsedEntry> entries;

    @Unique
    private static void servercountryflags$updateLocationInfos(List<ServerBrowserList.BrowsedEntry> entries) {
        entries.forEach(entry -> {
            String ip = entry.getServerData().ip;
            if (!ServerCountryFlags.servers.containsKey(ip) || ServerCountryFlags.servers.get(ip).status() != APIResponse.Status.SUCCESS) {
                ServerCountryFlags.updateServerLocationInfo(ip);
            }
        });
    }

    @Inject(at = @At("TAIL"), method = "refreshServers", remap = false)
    public void refreshServers(CallbackInfo ci) {
        if (!Config.cfg.serverBrowserIntegration) {
            return;
        }

        if (Config.cfg.reloadOnRefresh) {
            ServerCountryFlags.servers.entrySet().removeIf(entry -> entries.stream().anyMatch(browsedEntry -> browsedEntry.getServerData().ip.equals(entry.getKey())));
        }

        servercountryflags$updateLocationInfos(entries);
    }

    @Inject(at = @At("TAIL"), method = "queryServers", remap = false)
    public void queryServers(CallbackInfo ci) {
        if (!Config.cfg.serverBrowserIntegration) {
            return;
        }

        servercountryflags$updateLocationInfos(entries);
    }
}
