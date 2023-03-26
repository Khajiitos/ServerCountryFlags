package me.khajiitos.servercountryflags;

import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Properties;

public class Config {
    @ConfigEntry
    public static boolean flagBorder = true;

    @ConfigEntry
    public static boolean reloadOnRefresh = false;

    @ConfigEntry
    public static boolean showDistance = true;

    @ConfigEntry
    public static boolean useKm = true;

    @ConfigEntry
    public static boolean forceEnglish = false;

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
        boolean loadedProperties = false;
        try {
            properties.load(new FileInputStream(propertiesFile));
            loadedProperties = true;
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

        if (!loadedProperties)
            return;

        boolean rewriteConfig = false;
        for (Field field : Config.class.getDeclaredFields()) {
            ConfigEntry annotation = field.getAnnotation(ConfigEntry.class);
            if (annotation != null) {
                String propertiesValue = properties.getProperty(annotation.name().equals("") ? field.getName() : annotation.name());
                if (propertiesValue == null) {
                    rewriteConfig = true;
                } else {
                    try {
                        // TODO: Support more types later on
                        if (field.getType() ==  Boolean.class) {
                            field.setBoolean(null, Boolean.parseBoolean(propertiesValue));
                        }
                    } catch (IllegalAccessException e) {
                        ServerCountryFlags.LOGGER.warn("Bug: can't modify a config field");
                    }
                }
            }
        }

        if (rewriteConfig) {
            ServerCountryFlags.LOGGER.info("Properties file doesn't contain all fields available, rewriting it");
            save();
        }

        if (forceEnglish) {
            ServerCountryFlags.updateAPILanguage(null);
        } else if (MinecraftClient.getInstance().getLanguageManager() != null) {
            ServerCountryFlags.updateAPILanguage(MinecraftClient.getInstance().getLanguageManager().getLanguage());
        }
    }

    public static void save() {
        Properties properties = new Properties();

        for (Field field : Config.class.getDeclaredFields()) {
            ConfigEntry annotation = field.getAnnotation(ConfigEntry.class);
            if (annotation != null) {
                try {
                    properties.setProperty(annotation.name().equals("") ? field.getName() : annotation.name(), String.valueOf(field.get(null)));
                } catch (IllegalAccessException e) {
                    ServerCountryFlags.LOGGER.warn("Bug: can't access a config field");
                }
            }
        }

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
