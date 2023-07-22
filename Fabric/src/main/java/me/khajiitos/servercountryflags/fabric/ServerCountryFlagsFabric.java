package me.khajiitos.servercountryflags.fabric;

import net.fabricmc.api.ClientModInitializer;

public class ServerCountryFlagsFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ServerCountryFlags.init();
    }
}
