package me.khajiitos.servercountryflags;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServerCountryFlags implements ClientModInitializer {
	public static final String MOD_ID = "servercountryflags";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final String API_NAME = "http://ip-api.com/json/";
	public static int API_FIELDS = 16403;

	public static HashMap<String, ServerLocationInfo> servers = new HashMap<>();
	public static HashMap<String, Float> flagAspectRatios = new HashMap<>();


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
		});
	}
}
