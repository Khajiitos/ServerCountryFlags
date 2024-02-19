package me.khajiitos.servercountryflags.common.config;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.util.Color;
import me.khajiitos.servercountryflags.common.util.FlagPosition;
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
        @ConfigEntry(configCategory = "servercountryflags.config.category.border")
        public boolean flagBorder = true;

        @ConfigEntry(configCategory = "servercountryflags.config.category.border")
        public Color borderColor = new Color(65, 65, 65, 255);

        @ConfigEntry(configCategory = "servercountryflags.config.category.border")
        public boolean reloadOnRefresh = false;

        @ConfigEntry(configCategory = "servercountryflags.config.category.preferences")
        public boolean showDistance = true;

        @ConfigEntry(configCategory = "servercountryflags.config.category.locale")
        public boolean useKm = true;

        @ConfigEntry(configCategory = "servercountryflags.config.category.locale")
        public boolean forceEnglish = false;

        @ConfigEntry(configCategory = "servercountryflags.config.category.preferences")
        public boolean displayUnknownFlag = true;

        @ConfigEntry(configCategory = "servercountryflags.config.category.preferences")
        public boolean displayCooldownFlag = true;

        @ConfigEntry(configCategory = "servercountryflags.config.category.preferences")
        public boolean showDistrict = false;

        @ConfigEntry(configCategory = "servercountryflags.config.category.preferences")
        public boolean showISP = false;

        @ConfigEntry(configCategory = "servercountryflags.config.category.preferences")
        public boolean mapButton = true;

        @ConfigEntry(configCategory = "servercountryflags.config.category.preferences")
        public boolean mapButtonRight = true;

        @ConfigEntry(configCategory = "servercountryflags.config.category.preferences")
        public boolean showHomeOnMap = true;

        @ConfigEntry(configCategory = "servercountryflags.config.category.preferences")
        public boolean resolveRedirects = true;

        @ConfigEntry(configCategory = "servercountryflags.config.category.preferences", stringValues = {"left", "right", "behindName", "bottomRight"})
        public FlagPosition flagPosition = FlagPosition.BEHIND_NAME;

        @ConfigEntry(configCategory = "servercountryflags.config.category.preferences", requiredMod = "serverbrowser")
        public boolean serverBrowserIntegration = true;
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
            properties.load(new FileReader(propertiesFile));
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
                        } else if (field.getType().isEnum()) {
                            for (Object enumConstant : field.getType().getEnumConstants()) {
                                if (enumConstant.toString().equals(propertiesValue)) {
                                    field.set(cfg, enumConstant);
                                    break;
                                }
                            }
                        } else if (field.getType() == Color.class) {
                            field.set(cfg, Color.fromString(propertiesValue));
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

    // We just want to suppress the "condition always true" warning
    @SuppressWarnings("all")
    private static void afterLoad() {
        if (cfg.forceEnglish) {
            ServerCountryFlags.updateAPILanguage(null);
        } else {
            LanguageManager languageManager = Minecraft.getInstance().getLanguageManager();
            // IntelliJ claims that languageManager will never be null
            // but that's actually not the case
            if (languageManager != null) {
                ServerCountryFlags.updateAPILanguage(languageManager.getSelected().getCode());
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

        for (Field field : Config.Values.class.getDeclaredFields()) {
            ConfigEntry annotation = field.getAnnotation(ConfigEntry.class);
            if (annotation != null) {
                try {
                    properties.setProperty(field.getName(), String.valueOf(field.get(cfg)));
                } catch (IllegalAccessException e) {
                    ServerCountryFlags.LOGGER.warn("Bug: can't access a config field");
                }
            }
        }

        try {
            properties.store(new BufferedWriter(new FileWriter(propertiesFile)), "Server Country Flags properties file");
        } catch (FileNotFoundException e) {
            ServerCountryFlags.LOGGER.error("Couldn't save the properties file because it doesn't exist");
        } catch (IOException e) {
            ServerCountryFlags.LOGGER.error("Couldn't save the properties file");
            ServerCountryFlags.LOGGER.error(e.getMessage());
        }
    }

    @SuppressWarnings("BusyWait")
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
