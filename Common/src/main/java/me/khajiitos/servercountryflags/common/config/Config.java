package me.khajiitos.servercountryflags.common.config;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.resources.language.LanguageManager;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Properties;

public class Config {
    @ConfigEntry(description = "Displays a border around flags")
    public static boolean flagBorder = true;

    @ConfigEntry(description = "Red channel value for the border color around flags")
    public static int borderR = 65;

    @ConfigEntry(description = "Green channel value for the border color around flags")
    public static int borderG = 65;

    @ConfigEntry(description = "Blue channel value for the border color around flags")
    public static int borderB = 65;

    @ConfigEntry(description = "Alpha channel value for the border color around flags")
    public static int borderA = 255;

    @ConfigEntry(description = "Forces flags to be reloaded when the server list is refreshed")
    public static boolean reloadOnRefresh = false;

    @ConfigEntry(description = "Shows the approximate distance between the server and you when you hover over a flag")
    public static boolean showDistance = true;

    @ConfigEntry(description = "Uses kilometers instead of miles")
    public static boolean useKm = true;

    @ConfigEntry(description = "Forces the API results to be in English instead of your in-game language")
    public static boolean forceEnglish = false;

    @ConfigEntry(description = "Displays the unknown flag when we don't have data about the server yet")
    public static boolean displayUnknownFlag = true;

    @ConfigEntry(description = "Shows the district of the location too, if available")
    public static boolean showDistrict = false;

    @ConfigEntry(description = "Shows the ISP of the host, if available")
    public static boolean showISP = false;

    @ConfigEntry(description = "Shows a map button in the server list which opens the server map")
    public static boolean mapButton = true;

    @ConfigEntry(description = "Decides whether the map button should be on the right side or the left side")
    public static boolean mapButtonRight = true;

    @ConfigEntry(description = "Shows your location on the server map too")
    public static boolean showHomeOnMap = true;

    @ConfigEntry(description = "Changes the flags' positions. Available options: default, left, right, behindName")
    public static String flagPosition = "behindName";

    @ConfigEntry(description = "Uses the redirected IP (if present) instead of just the resolved IP")
    public static boolean resolveRedirects = true;

    private static File configDirectory;
    private static File propertiesFile;
    private static WatchService watchService;

    public static void init() {
        String minecraftDir = Minecraft.getInstance().gameDirectory.getAbsolutePath();
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
                        if (field.getType() == String.class) {
                            field.set(null, propertiesValue);
                        } else if (field.getType() == boolean.class) {
                            field.setBoolean(null, Boolean.parseBoolean(propertiesValue));
                        } else if (field.getType() == int.class) {
                            field.setInt(null, Integer.parseInt(propertiesValue));
                        } else if (field.getType() == float.class) {
                            field.setFloat(null, Float.parseFloat(propertiesValue));
                        } else {
                            ServerCountryFlags.LOGGER.warn("Bug: unsupported config type " + field.getType().getSimpleName());
                        }
                    } catch (IllegalAccessException e) {
                        ServerCountryFlags.LOGGER.warn("Bug: can't modify a config field");
                    } catch (NumberFormatException e) {
                        ServerCountryFlags.LOGGER.warn("Field " + field.getName() + " in the properties type is not of type " + field.getType().getSimpleName());
                    }
                }
            }
        }

        if (rewriteConfig) {
            ServerCountryFlags.LOGGER.info("Properties file doesn't contain all fields available, rewriting it");
            save();
        }

        afterLoad();
    }

    private static void afterLoad() {
        if (forceEnglish) {
            ServerCountryFlags.updateAPILanguage(null);
        } else {
            LanguageManager languageManager = Minecraft.getInstance().getLanguageManager();
            // IntelliJ claims that languageManager will never be null
            // but that's actually not the case
            if (languageManager != null) {
                ServerCountryFlags.updateAPILanguage(languageManager.getSelected());
            }
        }

        // So that the map button appears/disappears without having to reopen the screen
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof JoinMultiplayerScreen) {
            screen.resize(Minecraft.getInstance(), screen.width, screen.height);
        }
    }

    public static void save() {
        Properties properties = new Properties();

        StringBuilder fieldsDescriptions = new StringBuilder();

        for (Field field : Config.class.getDeclaredFields()) {
            ConfigEntry annotation = field.getAnnotation(ConfigEntry.class);
            if (annotation != null) {
                try {
                    String fieldName = annotation.name().equals("") ? field.getName() : annotation.name();
                    if (!annotation.description().equals("")) {
                        fieldsDescriptions.append("\n").append(fieldName).append(" - ").append(annotation.description());
                    }
                    properties.setProperty(fieldName, String.valueOf(field.get(null)));
                } catch (IllegalAccessException e) {
                    ServerCountryFlags.LOGGER.warn("Bug: can't access a config field");
                }
            }
        }

        try {
            String comments = "Mod properties file";
            if (!fieldsDescriptions.isEmpty()) {
                comments += "\nField descriptions:" + fieldsDescriptions;
            }
            properties.store(new FileOutputStream(propertiesFile), comments);
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
