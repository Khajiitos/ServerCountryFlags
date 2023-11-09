package me.khajiitos.servercountryflags.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.APIResponse;
import me.khajiitos.servercountryflags.common.util.FlagRenderInfo;
import me.khajiitos.servercountryflags.common.util.LocationInfo;
import me.khajiitos.servercountryflags.common.util.NetworkChangeDetector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver;
import net.minecraft.client.multiplayer.resolver.ServerRedirectHandler;
import net.minecraft.network.chat.Component;
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
import java.util.*;
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
	public static Set<String> unknownCountryCodes = new HashSet<>();
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
			Collection<ResourceLocation> resourceLocations = resourceManager.listResources("textures/flags", path -> path.endsWith(".png"));

			Thread flagThread = new Thread(() -> {
				for (ResourceLocation resourceLocation : resourceLocations) {
					if (!resourceLocation.getNamespace().equals(MOD_ID)) {
						continue;
					}
					try {
						Resource resource = resourceManager.getResource(resourceLocation);

						try (InputStream inputStream = resource.getInputStream()) {
							NativeImage image = NativeImage.read(inputStream);
							String code = last(resourceLocation.getPath().split("/"));
							code = code.substring(0, code.length() - 4);
							flagAspectRatios.put(code, (float)image.getWidth() / (float)image.getHeight());
						}
					} catch (IOException e) {
						ServerCountryFlags.LOGGER.error("Failed to load resource " + resourceLocation.getPath());
					}
				}
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
			HttpURLConnection con = (HttpURLConnection) apiUrl.openConnection();
			con.setConnectTimeout(3000);

			int requestsLeft = con.getHeaderFieldInt("X-Rl", -1);
			int secondsLeft = con.getHeaderFieldInt("X-Ttl", -1);

			APITimeoutManager.decrementRequestsSent();

			if (requestsLeft != -1 && secondsLeft != -1) {
				APITimeoutManager.setRequestsLeft(requestsLeft - APITimeoutManager.getRequestsSent());
				APITimeoutManager.setSecondsLeftUntilReset(secondsLeft);
			}

			if (con.getResponseCode() == 429) {
				return new APIResponse(APIResponse.Status.COOLDOWN, null);
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
					servers.put(serverAddress, response);
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

	public static FlagRenderInfo getFlagRenderInfo(APIResponse apiResponse) {
		String countryCode;
		double aspectRatio;
		List<Component> tooltip = new ArrayList<>();

		if (apiResponse == null || apiResponse.unknown()) {
			if (!Config.cfg.displayUnknownFlag) {
				return null;
			}
			tooltip.add(Component.translatable("servercountryflags.locationInfo.unknown"));
			countryCode = "unknown";
			aspectRatio = 1.5;
		} else if (apiResponse.cooldown()) {
			if (!Config.cfg.displayCooldownFlag) {
				return null;
			}
			tooltip.add(Component.translatable("servercountryflags.locationInfo.cooldown"));
			countryCode = "timeout";
			aspectRatio = 1.5;
		} else {
			LocationInfo locationInfo = apiResponse.locationInfo();
			if (ServerCountryFlags.flagAspectRatios.containsKey(locationInfo.countryCode)) {
				countryCode = locationInfo.countryCode;
				aspectRatio = ServerCountryFlags.flagAspectRatios.get(countryCode);
			} else {
				if (!ServerCountryFlags.unknownCountryCodes.contains(locationInfo.countryCode)) {
					ServerCountryFlags.LOGGER.error("Unknown country code: " + locationInfo.countryCode);
					ServerCountryFlags.unknownCountryCodes.add(locationInfo.countryCode);
				}

				if (Config.cfg.displayUnknownFlag) {
					countryCode = "unknown";
					aspectRatio = 1.5;
				} else {
					return null;
				}
			}

			tooltip.add(Component.literal((Config.cfg.showDistrict && !locationInfo.districtName.equals("") ? (locationInfo.districtName + ", ") : "") + locationInfo.cityName + ", " + locationInfo.countryName));

			if (Config.cfg.showISP && !locationInfo.ispName.equals("")) {
				tooltip.add(Component.translatable("servercountryflags.locationInfo.isp", locationInfo.ispName));
			}

			if (Config.cfg.showDistance) {
				double distanceFromLocal = locationInfo.getDistanceFromLocal(Config.cfg.useKm);
				if (distanceFromLocal != -1.0) {
					tooltip.add(Component.translatable("servercountryflags.locationInfo.distance", (int)distanceFromLocal, Component.translatable(Config.cfg.useKm ? "servercountryflags.locationInfo.km" : "servercountryflags.locationInfo.mi")).withStyle(ChatFormatting.ITALIC));
				}
			}
		}

		return new FlagRenderInfo(countryCode, aspectRatio, tooltip);
	}
}