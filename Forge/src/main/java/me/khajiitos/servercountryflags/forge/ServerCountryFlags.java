package me.khajiitos.servercountryflags.forge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.khajiitos.servercountryflags.forge.config.Config;
import me.khajiitos.servercountryflags.forge.util.LocationInfo;
import me.khajiitos.servercountryflags.forge.util.NetworkChangeDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class ServerCountryFlags {
	public static final String MOD_ID = "servercountryflags";
	public static final Logger LOGGER = Logger.getLogger(MOD_ID);
	public static final String API_NAME = "http://ip-api.com/json/";
	public static final int API_FIELDS = 541395;
	public static String apiLanguage = null;
	public static ServerList serverList; // Servers from the server list

	public static HashMap<String, LocationInfo> servers = new HashMap<>(); // Servers' flags
	public static HashMap<String, Float> flagAspectRatios = new HashMap<>();
	public static boolean flagAspectRatiosLoaded = false;
	public static LocationInfo localLocation = null;

	static <T>
	T last(T[] arr) {
		return arr[arr.length - 1];
	}

	public static void init() {
		Config.init();
		NetworkChangeDetector.check();
		Minecraft.getInstance().execute(() -> {
			IResourceManager resourceManager =  Minecraft.getInstance().getResourceManager();
			Collection<ResourceLocation> resourceLocations = resourceManager.listResources("textures/flags", path -> path.endsWith(".png"));

			Thread flagThread = new Thread(() -> {
				for (ResourceLocation resourceLocation : resourceLocations) {
					if (!resourceLocation.getNamespace().equals(MOD_ID))
						continue;

					try {
						IResource resource = resourceManager.getResource(resourceLocation);
						try (NativeImage image = NativeImage.read(resource.getInputStream())) {
							String code = last(resourceLocation.getPath().split("/"));
							code = code.substring(0, code.length() - 4);
							flagAspectRatios.put(code, (float)image.getWidth() / (float)image.getHeight());
						}
					} catch (IOException e) {
						LOGGER.severe("Failed to load resource");
						e.printStackTrace();
					}
				}
				flagAspectRatiosLoaded = true;
			});
			flagThread.setName("Flag load thread");
			flagThread.start();
		});

		updateLocalLocationInfo();
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

	public static boolean isIpLocal(String ip) {
		if (ip.isEmpty()) {
			return true;
		}
		try {
			int[] divided = Arrays.stream(ip.split("\\.")).mapToInt(Integer::parseInt).toArray();
			if (divided.length != 4) {
				return false;
			}
			return (divided[0] == 127 && divided[1] == 0 && divided[2] == 0 && divided[3] == 1) || (divided[0] == 192 && divided[1] == 168) || (divided[0] == 10) || (divided[0] == 172 && divided[1] >= 16 && divided[1] <= 31);
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static LocationInfo getServerLocationInfo(String ip) {
		// If the IP is local, make the API give us our location
		if (isIpLocal(ip)) {
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

			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
			JsonElement jsonElement = new JsonParser().parse(reader);

			if (jsonElement == null) {
				ServerCountryFlags.LOGGER.severe("Received something that's not JSON");
				return null;
			}

			if (jsonElement.isJsonObject()) {
				return new LocationInfo((JsonObject) jsonElement);
			} else {
				ServerCountryFlags.LOGGER.severe("Received JSON element, but it's not an object: " + jsonElement);
			}
		} catch (MalformedURLException e) {
			ServerCountryFlags.LOGGER.severe("Malformed API Url: " + apiUrlStr);
		} catch (UnknownHostException e) {
			ServerCountryFlags.LOGGER.severe("Unknown host - no internet?");
		} catch (IOException e) {
			ServerCountryFlags.LOGGER.severe(e.getMessage());
		}
		return null;
	}

	public static void updateServerLocationInfo(String serverAddress) {
		CompletableFuture.runAsync(() -> {
			ServerAddress address = ServerAddress.parseString(serverAddress);
			if (address != null) {
				LocationInfo locationInfo = getServerLocationInfo(address.getHost());
				if (locationInfo != null && locationInfo.success) {
					servers.put(serverAddress, locationInfo);
				}
			}
		});
	}

	public static void updateLocalLocationInfo() {
		CompletableFuture.runAsync(() -> {
			LocationInfo info = getServerLocationInfo("");
			if (info != null && info.success) {
				localLocation = info;

				for (LocationInfo locationInfo : servers.values()) {
					locationInfo.updateDistanceFromLocal();
				}
			}
		});
	}
}
