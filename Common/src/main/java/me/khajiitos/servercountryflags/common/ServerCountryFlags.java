package me.khajiitos.servercountryflags.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.APIResponse;
import me.khajiitos.servercountryflags.common.util.LocationInfo;
import me.khajiitos.servercountryflags.common.util.NetworkChangeDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver;
import net.minecraft.client.multiplayer.resolver.ServerRedirectHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ServerCountryFlags {
	public static final String MOD_ID = "servercountryflags";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final String API_NAME = "http://ip-api.com/json/";
	public static final int API_FIELDS = 541395;
	public static String apiLanguage = null;
	public static ServerList serverList; // Servers from the server list

	public static HashMap<String, APIResponse> servers = new HashMap<>(); // Servers' flags
	public static HashMap<String, Float> flagAspectRatios = new HashMap<>();
	public static boolean flagAspectRatiosLoaded = false;
	public static ServerRedirectHandler redirectResolver = ServerRedirectHandler.createDnsSrvRedirectHandler();

	public static LocationInfo localLocation = null;

	static <T>
	T last(T[] arr) {
		return arr[arr.length - 1];
	}

	public static void init() {
		Config.init();
		NetworkChangeDetector.check();
		Minecraft.getInstance().execute(() -> {
			ResourceManager resourceManager =  Minecraft.getInstance().getResourceManager();
			Map<ResourceLocation, Resource> resourceLocations = resourceManager.listResources("textures/flags", path -> true);

			Thread flagThread = new Thread(() -> {
				for (Map.Entry<ResourceLocation, Resource> entry : resourceLocations.entrySet()) {
					if (!entry.getKey().getNamespace().equals(MOD_ID)) {
						continue;
					}
					try {
						if (entry.getValue() == null) {
							ServerCountryFlags.LOGGER.error("Failed to load resource " + entry.getKey().getPath());
							continue;
						}

						try (InputStream inputStream = entry.getValue().open()) {
							NativeImage image = NativeImage.read(inputStream);
							String code = last(entry.getKey().getPath().split("/"));
							code = code.substring(0, code.length() - 4);
							flagAspectRatios.put(code, (float)image.getWidth() / (float)image.getHeight());
						}
					} catch (IOException e) {
						LOGGER.error(e.getMessage());
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

		if (Config.cfg.forceEnglish) {
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

	public static @NotNull APIResponse getAPIResponse(String ip) {
		// If the IP is empty, the API will give us our location

		if (APITimeoutManager.isOnCooldown()) {
			return new APIResponse(APIResponse.Status.COOLDOWN, null);
		}

		String apiUrlStr = API_NAME + ip + "?fields=" + API_FIELDS;
		if (apiLanguage != null) {
			apiUrlStr += "&lang=" + apiLanguage;
		}
		try {
			APITimeoutManager.incrementRequestsSent();
			URL apiUrl = new URL(apiUrlStr);
			URLConnection con = apiUrl.openConnection();
			con.setConnectTimeout(3000);

			int requestsLeft = con.getHeaderFieldInt("X-Rl", -1);
			int secondsLeft = con.getHeaderFieldInt("X-Ttl", -1);

			APITimeoutManager.decrementRequestsSent();

			if (requestsLeft != -1 && secondsLeft != -1) {
				APITimeoutManager.setRequestsLeft(requestsLeft - APITimeoutManager.getRequestsSent());
				APITimeoutManager.setSecondsLeftUntilReset(secondsLeft);
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
			JsonElement jsonElement = JsonParser.parseReader(reader);

			if (jsonElement == null) {
				ServerCountryFlags.LOGGER.error("Received something that's not JSON");
				return new APIResponse(APIResponse.Status.UNKNOWN, null);
			}

			if (jsonElement.isJsonObject()) {
				return new APIResponse(APIResponse.Status.SUCCESS, new LocationInfo((JsonObject) jsonElement));
			} else {
				ServerCountryFlags.LOGGER.error("Received JSON element, but it's not an object: " + jsonElement);
				return new APIResponse(APIResponse.Status.UNKNOWN, null);
			}
		} catch (MalformedURLException e) {
			ServerCountryFlags.LOGGER.error("Malformed API Url: " + apiUrlStr);
		} catch (UnknownHostException e) {
			ServerCountryFlags.LOGGER.error("Unknown host - no internet?");
		} catch (IOException e) {
			ServerCountryFlags.LOGGER.error(e.getMessage());
		}
		APITimeoutManager.decrementRequestsSent();
		return new APIResponse(APIResponse.Status.UNKNOWN, null);
	}

	public static boolean isIpLocal(InetAddress address) {
		return address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress();
	}

	public static void updateServerLocationInfo(String serverAddress) {
		CompletableFuture.runAsync(() -> {
			ServerAddress address = ServerAddress.parseString(serverAddress);
			if (Config.cfg.resolveRedirects) {
				Optional<ServerAddress> redirect = redirectResolver.lookupRedirect(address);

				if (redirect.isPresent()) {
					address = redirect.get();
				}
			}

			Optional<ResolvedServerAddress> resolvedAddress = ServerAddressResolver.SYSTEM.resolve(address);
			if (resolvedAddress.isPresent()) {
				InetSocketAddress socketAddress = resolvedAddress.get().asInetSocketAddress();
				String stringHostAddress = isIpLocal(socketAddress.getAddress()) ? "" : socketAddress.getAddress().getHostAddress();

				APIResponse response = getAPIResponse(stringHostAddress);
				APIResponse oldResponse = servers.get(serverAddress);
				if (oldResponse == null || (oldResponse.unknown() || !response.unknown()) || (oldResponse.cooldown() && response.locationInfo() != null)) {
					servers.putIfAbsent(serverAddress, response);
				}
			}
		});
	}

	public static void updateLocalLocationInfo() {
		CompletableFuture.runAsync(() -> {
			APIResponse response = getAPIResponse("");
			if (response.locationInfo() != null) {
				localLocation = response.locationInfo();

				for (APIResponse serverResponse : servers.values()) {
					if (!serverResponse.cooldown()) {
						serverResponse.locationInfo().updateDistanceFromLocal();
					}
				}
			}
		});
	}
}