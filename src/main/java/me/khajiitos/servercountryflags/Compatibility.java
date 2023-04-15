package me.khajiitos.servercountryflags;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("All")
public class Compatibility {
    public static MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();
    public static ButtonWidget buttonWidget(int x, int y, int width, int height, Text name, ButtonWidget.PressAction pressAction) {
        // In Minecraft 1.19.4 the ButtonWidget constructor is protected
        // and we have to construct ButtonWidgets through its Builder class,
        // but in versions older than 1.19.4 the Builder class doesn't exist and the constructor is public
        try {
            Constructor<ButtonWidget> buttonWidgetConstructor = ButtonWidget.class.getDeclaredConstructor(int.class, int.class, int.class, int.class, Text.class, ButtonWidget.PressAction.class);
            buttonWidgetConstructor.setAccessible(true);
            return buttonWidgetConstructor.newInstance(x, y, width, height, name, pressAction);
        } catch (Exception ignored) {}

        try {
            String builderClassName = resolver.mapClassName("intermediary", "net.minecraft.class_4185$class_7840");
            String dimensionsMethodName = resolver.mapMethodName("intermediary", "net.minecraft.class_4185$class_7840", "method_46434", "(IIII)Lnet/minecraft/class_4185$class_7840;");
            String buildMethodName = resolver.mapMethodName("intermediary", "net.minecraft.class_4185$class_7840", "method_46431", "()Lnet/minecraft/class_4185;");
            Class builderClass = Class.forName(builderClassName);
            Constructor builderConstructor = builderClass.getConstructor(Text.class, ButtonWidget.PressAction.class);
            Method dimensionsMethod = builderClass.getMethod(dimensionsMethodName, int.class, int.class, int.class, int.class);
            Method buildMethod = builderClass.getMethod(buildMethodName);

            Object builder = builderConstructor.newInstance(name, pressAction);
            dimensionsMethod.invoke(builder, x, y, width, height);
            return (ButtonWidget) buildMethod.invoke(builder);
        } catch (Exception ignored) {}

        ServerCountryFlags.LOGGER.error("Failed to initialize a ButtonWidget");
        return null;
    }

    public static void drawBorder(MatrixStack matrices, int x, int y, int width, int height, int color) {
        // In Minecraft versions older than 1.19.4 function DrawableHelper.drawBorder doesn't exist
        DrawableHelper.fill(matrices, x, y, x + width, y + 1, color);
        DrawableHelper.fill(matrices, x, y + height - 1, x + width, y + height, color);
        DrawableHelper.fill(matrices, x, y + 1, x + 1, y + height - 1, color);
        DrawableHelper.fill(matrices, x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public static String getLanguageCode() {
        // In Minecraft 1.19.4 languageManager.getLanguage() is a String,
        // in older versions languageManager.getLanguage() is a LanguageDefinition
        // The language code is located in a private field currentLanguageCode, which we try to access
        LanguageManager languageManager = MinecraftClient.getInstance().getLanguageManager();

        if (languageManager == null) {
            return null;
        }

        try {
            String currentLanguageCodeName = resolver.mapFieldName("intermediary", "net.minecraft.class_1076", "field_5323", "Ljava/lang/String;");
            Field currentLanguageCode = languageManager.getClass().getDeclaredField(currentLanguageCodeName);
            currentLanguageCode.setAccessible(true);
            return (String) currentLanguageCode.get(languageManager);
        } catch (Exception ignored) {
            ServerCountryFlags.LOGGER.error("Failed to get the current language");
            return null;
        }
    }

    public static void setMultiplayerScreenTooltip(MultiplayerScreen screen, List<Text> tooltip) {
        // In Minecraft 1.19.4, setTooltip method uses List<OrderedText>, and in older versions it uses List<Text>
        // In Minecraft 1.19.4, there is a method setMultiPlayerScreenTooltip which uses List<Text>,
        // so we will use that for 1.19.4, and for older versions we will use setTooltip, which also uses List<Text>
        try {
            String setMultiplayerScreenTooltipName = resolver.mapMethodName("intermediary", "net.minecraft.class_500", "method_2528", "(Ljava/util/List;)V");
            Method setMultiplayerScreenTooltip = screen.getClass().getMethod(setMultiplayerScreenTooltipName, List.class);
            setMultiplayerScreenTooltip.invoke(screen, tooltip);
            return;
        } catch (Exception ignored) {}

        try {
            String setTooltipName = resolver.mapMethodName("intermediary", "net.minecraft.class_500", "method_2528", "(Ljava/util/List;)V");
            Method setTooltip = screen.getClass().getMethod(setTooltipName, List.class);
            setTooltip.invoke(screen, tooltip);
            return;
        } catch (Exception ignored) {}

        ServerCountryFlags.LOGGER.error("Failed to set a tooltip");
    }

    public static MutableText translatableText(String text) {
        return translatableText(text, new Object[0]);
    }

    public static MutableText translatableText(String text, Object... args) {
        // In 1.19 to get translatable text you use Text.translatable, and in older versions
        // you use the class TranslatableText
        try {
            String translatableMethodName = resolver.mapMethodName("intermediary", "net.minecraft.class_2561", "method_43469", "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/class_5250;");
            Method translatableMethod = Text.class.getMethod(translatableMethodName, String.class, Object[].class);
            return (MutableText) translatableMethod.invoke(null, text, args);
        } catch (Exception ignored) {}

        try {
            String translatableTextClassName = resolver.mapClassName("intermediary", "net.minecraft.class_2588");
            Class translatableTextClass = Class.forName(translatableTextClassName);
            Constructor translatableTextConstructor = translatableTextClass.getConstructor(String.class, Object[].class);
            return (MutableText) translatableTextConstructor.newInstance(text, args);
        } catch (Exception ignored) {}

        ServerCountryFlags.LOGGER.error("Failed to get translatable text");
        return null;
    }

    public static MutableText literalText(String text) {
        // In 1.19 to get literal text you use Text.literal, and in older versions
        // you use the class LiteralText
        try {
            String literalMethodName = resolver.mapMethodName("intermediary", "net.minecraft.class_2561", "method_43470", "(Ljava/lang/String;)Lnet/minecraft/class_5250;");
            Method literalMethod = Text.class.getMethod(literalMethodName, String.class);
            return (MutableText) literalMethod.invoke(null, text);
        } catch (Exception ignored) {}

        try {
            String literalTextClassName = resolver.mapClassName("intermediary", "net.minecraft.class_2585");
            Class literalTextClass = Class.forName(literalTextClassName);
            Constructor literalTextConstructor = literalTextClass.getConstructor(String.class);
            return (MutableText) literalTextConstructor.newInstance(text);
        } catch (Exception ignored) {}

        ServerCountryFlags.LOGGER.error("Failed to get literal text");
        return null;
    }

    public static Collection<Identifier> findResources(String startingPath) {
        // In Minecraft 1.19 the function findResources returns a Map<Identifier, Resource>,
        // and in older versions it returns a Collection<Identifier>
        // also, in Minecraft 1.19 the function takes a Predicate<Identifier>, and
        // in older versions it takes a Predicate<String>
        ResourceManager manager = MinecraftClient.getInstance().getResourceManager();

        try {
            Predicate<Identifier> identifierPredicate = (path) -> true;
            String newFindResourcesName = resolver.mapMethodName("intermediary", "net.minecraft.class_3300", "method_14488", "(Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/Map;");
            Method newFindResources = manager.getClass().getMethod(newFindResourcesName, String.class, Predicate.class);
            Object result = newFindResources.invoke(manager, startingPath, identifierPredicate);
            Map<Identifier, Resource> map = (Map<Identifier, Resource>) result;
            return map.keySet();
        } catch (Exception ignored) {}

        try {
            Predicate<String> stringPredicate = (path) -> true;
            String oldFindResourcesName = resolver.mapMethodName("intermediary", "net.minecraft.class_3300", "method_14488", "(Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/Collection;");
            Method oldFindResources = manager.getClass().getMethod(oldFindResourcesName, String.class, Predicate.class);
            return (Collection<Identifier>) oldFindResources.invoke(manager, startingPath, stringPredicate);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        ServerCountryFlags.LOGGER.error("Failed to find resources");
        return null;
    }

    public static Object getResource(Identifier identifier) {
        // In Minecraft 1.19 the function getResource returns an Optional<Resource>,
        // and in older versions it returns just Resource
        // Also, in newer versions Resource is a class, and in older versions it's an interface
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();

        try {
            String newGetResourceName = resolver.mapMethodName("intermediary", "net.minecraft.class_5912", "method_14486", "(Lnet/minecraft/class_2960;)Ljava/util/Optional;");
            Method newGetResource = resourceManager.getClass().getMethod(newGetResourceName, Identifier.class);
            Optional<Object> optional = (Optional<Object>) newGetResource.invoke(resourceManager, identifier);
            return optional.orElse(null);
        } catch (Exception ignored) {}

        try {
            String oldGetResourceName = resolver.mapMethodName("intermediary", "net.minecraft.class_5912", "method_14486", "(Lnet/minecraft/class_2960;)Lnet/minecraft/class_3298;");
            Method oldGetResource = resourceManager.getClass().getMethod(oldGetResourceName, Identifier.class);
            return oldGetResource.invoke(resourceManager, identifier);
        } catch (Exception ignored) {}

        ServerCountryFlags.LOGGER.error("Failed to get resource");
        return null;
    }

    public static void setScreen(Screen screen) {
        // In Minecraft 1.18 and above the function to open a screen is MinecraftClient.setScreen,
        // and in versions older than 1.18 it's named MinecraftClient.openScreen

        MinecraftClient client = MinecraftClient.getInstance();

        try {
            // The intermediary name stays the same as it's the same function, just renamed
            String setScreenName = resolver.mapMethodName("intermediary", "net.minecraft.class_310", "method_1507", "(Lnet/minecraft/class_437;)V");
            Method setScreen = client.getClass().getMethod(setScreenName, Screen.class);
            setScreen.invoke(client, screen);
            return;
        } catch (Exception ignored) {}

        ServerCountryFlags.LOGGER.error("Failed to set a screen");
    }

    public static JsonElement parseReaderToJson(Reader reader) {
        // In newer versions in gson (in 1.18+), to parse a json object from a reader
        // you use a static method JsonParser.parseReader, and in older
        // versions you use the parse(Reader) method from an instance of this class
        try {
            Method parseReader = JsonParser.class.getMethod("parseReader", Reader.class);
            return (JsonElement) parseReader.invoke(null, reader);
        } catch (Exception ignored) {}

        try {
            Constructor jsonParserConstructor = JsonParser.class.getConstructor();
            Object jsonParser = jsonParserConstructor.newInstance();
            Method parse = JsonParser.class.getMethod("parse", Reader.class);
            return (JsonElement) parse.invoke(jsonParser, reader);
        } catch (Exception ignored) {}

        ServerCountryFlags.LOGGER.error("Failed to parse JSON from a reader");
        return null;
    }

    public static boolean jsonObjectContainsAllFields(JsonObject jsonObject, List<String> fields) {
        // In newer versions of gson, jsonElements has a method keySet, using which
        // we can check if all of our fields are in the json element.
        // To support older versions, we will use a different approach to this

        for (String field : fields) {
            if (!jsonObject.has(field)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNewerThan1_19_3() {
        try {
            String getGameVersionName = resolver.mapMethodName("intermediary", "net.minecraft.class_155", "method_16673", "()Lcom/mojang/bridge/game/GameVersion;");
            Method getGameVersion = SharedConstants.class.getMethod(getGameVersionName);
            Object gameVersion = getGameVersion.invoke(null);

            for (Method method : gameVersion.getClass().getMethods()) {
                // Since we don't have any mapping for getBuildTime(),
                // we're going to find it this way
                if (method.getReturnType() == Date.class) {
                    // Method returns Date, so it must be getBuildTime
                    Date buildTime = (Date) method.invoke(gameVersion);
                    return buildTime.after(new Date(1670400844000L));
                }
            }
            throw new ClassNotFoundException();
        } catch (Throwable e) {
            ServerCountryFlags.LOGGER.warn("Failed to check if the version if newer than 1.19.3, assuming that it isn't");
            return false;
        }
    }

    public static InputStream getResourceInputStream(Object resource) {
        // Since java complains that resource is a class but expects an interface
        // or the other way around, we have to do it this way

        try {
            String getInputStreamName = resolver.mapMethodName("intermediary", "net.minecraft.class_3298", "method_14482", "()Ljava/io/InputStream;");
            Method getInputStream = resource.getClass().getMethod(getInputStreamName);
            InputStream inputStream = (InputStream) getInputStream.invoke(resource);
            return inputStream;
        } catch (Exception ignored) {
            ServerCountryFlags.LOGGER.error("Failed to get resource input stream");
            return null;
        }
    }
}
