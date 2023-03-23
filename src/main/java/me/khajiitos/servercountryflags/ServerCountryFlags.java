package me.khajiitos.servercountryflags;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

public class ServerCountryFlags implements ClientModInitializer {

	public static final Logger LOGGER = Logger.getLogger("servercountryflags");
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
		MinecraftClient.getInstance().execute(() -> {
			ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
			Collection<Identifier> resourceLocations = manager.findResources("textures/flags", path -> true);

			System.out.println("Initializing flags");
			for (Identifier identifier : resourceLocations) {
				if (!identifier.getNamespace().equals("servercountryflags"))
					continue;
				try {
					NativeImage image = NativeImage.read(manager.getResource(identifier).getInputStream());
					String code = last(identifier.getPath().split("/"));
					code = code.substring(0, code.length() - 4);
					flagAspectRatios.put(code, (float)image.getWidth() / (float)image.getHeight());
					LOGGER.info(code);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
