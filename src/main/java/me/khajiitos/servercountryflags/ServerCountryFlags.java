package me.khajiitos.servercountryflags;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.Address;
import net.minecraft.client.network.AddressResolver;
import net.minecraft.client.network.RedirectResolver;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ServerCountryFlags implements ClientModInitializer {
	public static final String MOD_ID = "servercountryflags";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final String API_NAME = "http://ip-api.com/json/";
	public static final int API_FIELDS = 541395;
	public static final boolean isNewerThan1_19_3 = Compatibility.isNewerThan1_19_3();
	public static String apiLanguage = null;
	public static ServerList serverList; // Servers from the server list

	public static HashMap<String, LocationInfo> servers = new HashMap<>(); // Servers' flags
	public static HashMap<String, Float> flagAspectRatios = new HashMap<>();
	public static boolean flagAspectRatiosLoaded = false;
	public static RedirectResolver redirectResolver = RedirectResolver.createSrv();

	public static LocationInfo localLocation = null;

	<T>
	T last(T[] arr) {
		return arr[arr.length - 1];
	}

	@Override
	public void onInitializeClient() {
		Config.init();
		NetworkChangeDetector.check();
		MinecraftClient.getInstance().execute(() -> {
			Collection<Identifier> resourceLocations = Compatibility.findResources("textures/flags");

			Thread flagThread = new Thread(() -> {
				for (Identifier identifier : resourceLocations) {
					if (!identifier.getNamespace().equals(MOD_ID))
						continue;
					try {
						Object resource = Compatibility.getResource(identifier);
						if (resource == null) {
							ServerCountryFlags.LOGGER.error("Failed to load resource " + identifier.getPath());
							continue;
						}
						NativeImage image = NativeImage.read(Compatibility.getResourceInputStream(resource));
						String code = last(identifier.getPath().split("/"));
						code = code.substring(0, code.length() - 4);
						flagAspectRatios.put(code, (float)image.getWidth() / (float)image.getHeight());
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
			return (divided[0] == 192 && divided[1] == 168) || (divided[0] == 10) || (divided[0] == 172 && divided[1] >= 16 && divided[1] <= 31);
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
			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			JsonElement jsonElement = Compatibility.parseReaderToJson(reader);
			if (jsonElement.isJsonObject()) {
				return new LocationInfo((JsonObject) jsonElement);
			} else {
				ServerCountryFlags.LOGGER.error("Received JSON element, but it's not an object: " + jsonElement);
			}
		} catch (MalformedURLException e) {
			ServerCountryFlags.LOGGER.error("Malformed API Url: " + apiUrlStr);
		} catch (UnknownHostException e) {
			ServerCountryFlags.LOGGER.error("Unknown host - no internet?");
		} catch (IOException e) {
			ServerCountryFlags.LOGGER.error(e.getMessage());
		}
		return null;
	}

	public static void updateServerLocationInfo(String serverAddress) {
		CompletableFuture.runAsync(() -> {
			ServerAddress parsedAddress = ServerAddress.parse(serverAddress);
			Optional<Address> optional = AddressResolver.DEFAULT.resolve(parsedAddress);
			if (optional.isPresent()) {
				InetSocketAddress address = optional.get().getInetSocketAddress();
				Optional<ServerAddress> redirect = redirectResolver.lookupRedirect(parsedAddress);
				if (redirect.isPresent()) {
					Optional<Address> resolved = AddressResolver.DEFAULT.resolve(redirect.get());
					if (resolved.isPresent()) {
						address = resolved.get().getInetSocketAddress();
					}
				}
				LocationInfo locationInfo = getServerLocationInfo(address.getAddress().getHostAddress());
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
