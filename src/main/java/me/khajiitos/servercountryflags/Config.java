package me.khajiitos.servercountryflags;

import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class Config {
    public static boolean flagBorder = true;
    public static boolean reloadOnRefresh = false;

    private static File configDirectory;
    private static File propertiesFile;
    private static WatchService watchService;

    public static void init() {
        String minecraftDir = MinecraftClient.getInstance().runDirectory.getAbsolutePath();
        configDirectory = new File(minecraftDir + "/config/" + ServerCountryFlags.MOD_ID);
        propertiesFile = new File(configDirectory.getAbsolutePath() + "/" + ServerCountryFlags.MOD_ID + ".properties");

        load();
        registerWatchService();
    }

    public static void load() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFile));
        } catch (FileNotFoundException e) {
            ServerCountryFlags.LOGGER.info("Our properties file doesn't exist, creating it");
            try {
                boolean ignored = configDirectory.mkdirs();
                if (!propertiesFile.createNewFile()) {
                    ServerCountryFlags.LOGGER.warn("Our properties file actually exists... What?");
                }
                save();
            } catch (IOException ex) {
                ServerCountryFlags.LOGGER.error("Couldn't create the properties file");
                ServerCountryFlags.LOGGER.error(ex.getMessage());
            }
        } catch (IOException e) {
            ServerCountryFlags.LOGGER.error("Couldn't read the properties file");
            ServerCountryFlags.LOGGER.error(e.getMessage());
        }

        flagBorder = Boolean.parseBoolean(String.valueOf(properties.getOrDefault("flagBorder", String.valueOf(flagBorder))));
        reloadOnRefresh = Boolean.parseBoolean(String.valueOf(properties.getOrDefault("reloadOnRefresh", String.valueOf(reloadOnRefresh))));
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("flagBorder", String.valueOf(flagBorder));
        properties.setProperty("reloadOnRefresh", String.valueOf(reloadOnRefresh));
        try {
            properties.store(new FileOutputStream(propertiesFile), "Mod properties file");
        } catch (FileNotFoundException e) {
            ServerCountryFlags.LOGGER.error("Couldn't save the properties file because it doesn't exist");
        } catch (IOException e) {
            ServerCountryFlags.LOGGER.error("Couldn't save the properties file");
            ServerCountryFlags.LOGGER.error(e.getMessage());
        }
    }

    private static void registerWatchService() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Paths.get(propertiesFile.getParent()).register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            Thread watcherThread = new Thread(() -> {
                try {
                    while (true) {
                        WatchKey key = watchService.take();

                        // Sometimes multiple events may be taken from updating the file once, sleeping prevents it
                        Thread.sleep(250);

                        if (!key.pollEvents().isEmpty()) {
                            ServerCountryFlags.LOGGER.info("Properties file modified, reloading it");
                            load();
                        }
                        key.reset();
                    }
                } catch (Exception e) {
                    ServerCountryFlags.LOGGER.warn("WatchService closed");
                }
            });
            watcherThread.setName("File watcher");
            watcherThread.start();
        } catch (IOException e) {
            ServerCountryFlags.LOGGER.error("Couldn't initialize WatchService");
        }
    }
}
