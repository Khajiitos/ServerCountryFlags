package me.khajiitos.servercountryflags.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.khajiitos.servercountryflags.ServerCountryFlags;
import me.khajiitos.servercountryflags.ServerLocationInfo;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

@Mixin(MultiplayerServerListPinger.class)
public class MultiplayerServerListPingerMixin {
    @Inject(method = "add", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void afterAdd(final ServerInfo entry, final Runnable saver, final CallbackInfo info, final ServerAddress sa, Optional o, InetSocketAddress inetSocketAddress) {
        String ip = inetSocketAddress.getHostName();
        String apiUrlStr = ServerCountryFlags.API_NAME + ip + "?fields=" + ServerCountryFlags.API_FIELDS;
        try {
            URL apiUrl = new URL(apiUrlStr);
            URLConnection con = apiUrl.openConnection();
            con.setConnectTimeout(3000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            JsonElement jsonElement = JsonParser.parseReader(reader);
            if (!jsonElement.isJsonObject()) {
                ServerCountryFlags.LOGGER.error("Received JSON element, but it's not an object: " + jsonElement);
                return;
            }
            ServerLocationInfo serverLocationInfo = new ServerLocationInfo((JsonObject) jsonElement);

            if (serverLocationInfo.success) {
                ServerCountryFlags.servers.put(entry.address, serverLocationInfo);
            } else {
                ServerCountryFlags.servers.put(entry.address, null);
            }
        } catch (MalformedURLException e) {
            ServerCountryFlags.LOGGER.error("Malformed API Url: " + apiUrlStr);
        } catch (IOException e) {
            ServerCountryFlags.LOGGER.error("Some other exception: " + apiUrlStr);
        }
    }
}
