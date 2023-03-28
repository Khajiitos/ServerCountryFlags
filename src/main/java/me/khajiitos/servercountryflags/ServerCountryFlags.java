package me.khajiitos.servercountryflags;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServerCountryFlags implements ClientModInitializer {
	public static final String MOD_ID = "servercountryflags";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final String API_NAME = "http://ip-api.com/json/";
	public static final int API_FIELDS = 541395;
	public static String apiLanguage = null;

	public static HashMap<String, LocationInfo> servers = new HashMap<>();
	public static HashMap<String, Float> flagAspectRatios = new HashMap<>();
	public static boolean flagAspectRatiosLoaded = false;

	public static LocationInfo localLocation = null;

	<T>
	T last(T[] arr) {
		return arr[arr.length - 1];
	}

	@Override
	public void onInitializeClient() {
		Config.init();
		MinecraftClient.getInstance().execute(() -> {
			ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
			Map<Identifier, Resource> resourceLocations = manager.findResources("textures/flags", path -> true);

			Thread flagThread = new Thread(() -> {
				for (Map.Entry<Identifier, Resource> entry : resourceLocations.entrySet()) {
					if (!entry.getKey().getNamespace().equals(MOD_ID))
						continue;
					try {
						NativeImage image = NativeImage.read(entry.getValue().getInputStream());
						String code = last(entry.getKey().getPath().split("/"));
						code = code.substring(0, code.length() - 4);
						flagAspectRatios.put(code, (float)image.getWidth() / (float)image.getHeight());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				flagAspectRatiosLoaded = true;
			});
			flagThread.setName("Flag load thread");
			flagThread.start();
		});
		LocationInfo local = getServerLocationInfo("");
		if (local != null && local.success) {
			ServerCountryFlags.localLocation = local;
		}
	}

	public static void updateAPILanguage(String language) {
		final String oldApiLanguage = apiLanguage;

		if (Config.forceEnglish) {
			apiLanguage = null;
		} else if (language != null) {
			if (language.startsWith("en")) apiLanguage = null;
			else if (language.startsWith("de")) apiLanguage = "de";
			else if (language.startsWith("es")) apiLanguage = "es";
			else if (language.startsWith("pt")) apiLanguage = "pt-BR";
			else if (language.startsWith("fr")) apiLanguage = "fr";
			else if (language.startsWith("ja")) apiLanguage = "ja";
			else if (language.startsWith("zn")) apiLanguage = "zn-CN";
			else if (language.startsWith("ru")) apiLanguage = "ru";
			else apiLanguage = null;
		}

		if (!Objects.equals(apiLanguage, oldApiLanguage)) {
			servers.clear();
		}
	}

	public static LocationInfo getServerLocationInfo(String ip) {
		// If the IP is local, make the API give us our location
		if (ip.equals("127.0.0.1") || ip.startsWith("192.168")) {
			ip = "";
		}

		String apiUrlStr = API_NAME + ip + "?fields=" + API_FIELDS;
		if (apiLanguage != null) {
			apiUrlStr += "&lang=" + apiLanguage;
		}
		try {
			URL apiUrl = new URL(apiUrlStr);
			URLConnection con = apiUrl.openConnection();
			con.setConnectTimeout(3000);
			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			JsonElement jsonElement = JsonParser.parseReader(reader);
			if (jsonElement.isJsonObject()) {
				return new LocationInfo((JsonObject) jsonElement);
			} else {
				ServerCountryFlags.LOGGER.error("Received JSON element, but it's not an object: " + jsonElement);
			}
		} catch (MalformedURLException e) {
			ServerCountryFlags.LOGGER.error("Malformed API Url: " + apiUrlStr);
		}
		catch (IOException e) {
			ServerCountryFlags.LOGGER.error("Some other exception: " + apiUrlStr);
			ServerCountryFlags.LOGGER.error(e.getMessage());
		}
		return null;
	}
}
