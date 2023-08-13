package me.khajiitos.servercountryflags.common.config;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.resources.language.LanguageManager;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

public class Config {
    public static class Values {
        @ConfigEntry(name = "Flag border", description = "Displays a border around flags", configCategory = "Border")
        public boolean flagBorder = true;

        @ConfigEntry(name = "Border color R", description = "Red channel value for the border color around flags", configCategory = "Border", constraints = @Constraints(maxValue = 255))
        public int borderR = 65;

        @ConfigEntry(name = "Border color G", description = "Green channel value for the border color around flags", configCategory = "Border", constraints = @Constraints(maxValue = 255))
        public int borderG = 65;

        @ConfigEntry(name = "Border color B", description = "Blue channel value for the border color around flags", configCategory = "Border", constraints = @Constraints(maxValue = 255))
        public int borderB = 65;

        @ConfigEntry(name = "Border color A", description = "Alpha channel value for the border color around flags", configCategory = "Border", constraints = @Constraints(maxValue = 255))
        public int borderA = 255;

        @ConfigEntry(name = "Reload on refresh", description = "Forces flags to be reloaded when the server list is refreshed")
        public boolean reloadOnRefresh = false;

        @ConfigEntry(name = "Show distance", description = "Shows the approximate distance between the server and you when you hover over a flag", configCategory = "Preferences")
        public boolean showDistance = true;

        @ConfigEntry(name = "Use kilometers", description = "Uses kilometers instead of miles", configCategory = "Locale")
        public boolean useKm = true;

        @ConfigEntry(name = "Force English", description = "Forces the API results to be in English instead of your in-game language", configCategory = "Locale")
        public boolean forceEnglish = false;

        @ConfigEntry(name = "Display unknown flag", description = "Displays the unknown flag when we don't have data about the server yet")
        public boolean displayUnknownFlag = true;

        @ConfigEntry(name = "Display cooldown flag", description = "Displays a timeout flag when we have an API cooldown")
        public boolean displayCooldownFlag = true;

        @ConfigEntry(name = "Show district", description = "Shows the district of the location too, if available")
        public boolean showDistrict = false;

        @ConfigEntry(name = "Show ISP", description = "Shows the ISP of the host, if available")
        public boolean showISP = false;

        @ConfigEntry(name = "Map button", description = "Shows a map button in the server list which opens the server map", configCategory = "Preferences")
        public boolean mapButton = true;

        @ConfigEntry(name = "Map button on the right side", description = "Decides whether the map button should be on the right side or the left side", configCategory = "Preferences")
        public boolean mapButtonRight = true;

        @ConfigEntry(name = "Show home on map", description = "Shows your location on the server map too", configCategory = "Preferences")
        public boolean showHomeOnMap = true;

        @ConfigEntry(name = "Resolve SRV redirects", description = "Uses the redirected IP (if present) instead of just the resolved IP")
        public boolean resolveRedirects = true;

        @ConfigEntry(name = "Flag position", description = "Changes the flags' positions. Available options: default, left, right, behindName", stringValues = {"default", "left", "right", "behindName"}, configCategory = "Preferences")
        public String flagPosition = "behindName";
    }

    private static File configDirectory;
    private static File propertiesFile;
    private static WatchService watchService;

    // DO NOT TOUCH
    public static final Config.Values DEFAULT = new Config.Values();

    // but feel free to touch this one :)
    public static final Config.Values cfg = new Config.Values();

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
        for (Field field : Config.Values.class.getDeclaredFields()) {
            ConfigEntry annotation = field.getAnnotation(ConfigEntry.class);
            if (annotation != null) {
                String propertiesValue = properties.getProperty(field.getName());
                if (propertiesValue == null) {
                    rewriteConfig = true;
                } else {
                    try {
                        if (field.getType() == String.class) {
                            field.set(cfg, propertiesValue);
                        } else if (field.getType() == boolean.class) {
                            field.setBoolean(cfg, Boolean.parseBoolean(propertiesValue));
                        } else if (field.getType() == int.class) {
                            Optional<Constraints> constraints = Arrays.stream(annotation.constraints()).findFirst();
                            int value = Integer.parseInt(propertiesValue);
                            if (constraints.isPresent()) {
                                value = Math.max(Math.min(value, constraints.get().maxValue()), constraints.get().minValue());
                            }
                            field.setInt(cfg, value);
                        } else if (field.getType() == float.class) {
                            field.setFloat(cfg, Float.parseFloat(propertiesValue));
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
        if (cfg.forceEnglish) {
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

        for (Field field : Config.Values.class.getDeclaredFields()) {
            ConfigEntry annotation = field.getAnnotation(ConfigEntry.class);
            if (annotation != null) {
                try {
                    String fieldName = field.getName();
                    if (!annotation.description().equals("")) {
                        fieldsDescriptions.append("\n").append(fieldName).append(" - ").append(annotation.description());
                    }
                    properties.setProperty(fieldName, String.valueOf(field.get(cfg)));
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
