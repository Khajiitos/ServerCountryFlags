package me.khajiitos.servercountryflags.util;

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
import net.minecraft.util.Formatting;
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

    private static Constructor buttonWidgetConstructor;
    private static Constructor buttonWidgetBuilderConstructor;
    private static Constructor translatableTextConstructor;
    private static Constructor literalTextConstructor;
    private static Constructor jsonParserConstructor;

    private static Method buttonWidgetBuilderDimensionsMethod;
    private static Method buttonWidgetBuilderBuildMethod;
    private static Method setMultiplayerScreenTooltipMethod;
    private static Method setTooltipMethod;
    private static Method translatableMethod;
    private static Method literalMethod;
    private static Method formattedMethod;
    private static Method newFindResourcesMethod;
    private static Method oldFindResourcesMethod;
    private static Method newGetResourceMethod;
    private static Method oldGetResourceMethod;
    private static Method setScreenMethod;
    private static Method parseReaderMethod;
    private static Method parseMethod;
    private static Method getBuildTimeMethod;
    private static Method getInputStreamMethod;
    private static Method getGameVersionMethod;

    private static Field currentLanguageCodeField;

    static {
        try {
            buttonWidgetConstructor = ButtonWidget.class.getDeclaredConstructor(int.class, int.class, int.class, int.class, Text.class, ButtonWidget.PressAction.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String builderClassName = resolver.mapClassName("intermediary", "net.minecraft.class_4185$class_7840");
            String dimensionsMethodName = resolver.mapMethodName("intermediary", "net.minecraft.class_4185$class_7840", "method_46434", "(IIII)Lnet/minecraft/class_4185$class_7840;");
            String buildMethodName = resolver.mapMethodName("intermediary", "net.minecraft.class_4185$class_7840", "method_46431", "()Lnet/minecraft/class_4185;");
            Class builderClass = Class.forName(builderClassName);
            buttonWidgetBuilderConstructor = builderClass.getConstructor(Text.class, ButtonWidget.PressAction.class);
            buttonWidgetBuilderDimensionsMethod = builderClass.getMethod(dimensionsMethodName, int.class, int.class, int.class, int.class);
            buttonWidgetBuilderBuildMethod = builderClass.getMethod(buildMethodName);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String currentLanguageCodeName = resolver.mapFieldName("intermediary", "net.minecraft.class_1076", "field_5323", "Ljava/lang/String;");
            currentLanguageCodeField = LanguageManager.class.getDeclaredField(currentLanguageCodeName);
            currentLanguageCodeField.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String currentLanguageCodeName = resolver.mapFieldName("intermediary", "net.minecraft.class_1076", "field_5323", "Ljava/lang/String;");
            currentLanguageCodeField = LanguageManager.class.getDeclaredField(currentLanguageCodeName);
            currentLanguageCodeField.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String setMultiplayerScreenTooltipName = resolver.mapMethodName("intermediary", "net.minecraft.class_500", "method_2528", "(Ljava/util/List;)V");
            setMultiplayerScreenTooltipMethod = MultiplayerScreen.class.getMethod(setMultiplayerScreenTooltipName, List.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String setTooltipName = resolver.mapMethodName("intermediary", "net.minecraft.class_500", "method_2528", "(Ljava/util/List;)V");
            setTooltipMethod = Screen.class.getMethod(setTooltipName, List.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String translatableMethodName = resolver.mapMethodName("intermediary", "net.minecraft.class_2561", "method_43469", "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/class_5250;");
            translatableMethod = Text.class.getMethod(translatableMethodName, String.class, Object[].class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String translatableTextClassName = resolver.mapClassName("intermediary", "net.minecraft.class_2588");
            Class translatableTextClass = Class.forName(translatableTextClassName);
            translatableTextConstructor = translatableTextClass.getConstructor(String.class, Object[].class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String literalMethodName = resolver.mapMethodName("intermediary", "net.minecraft.class_2561", "method_43470", "(Ljava/lang/String;)Lnet/minecraft/class_5250;");
            literalMethod = Text.class.getMethod(literalMethodName, String.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String literalTextClassName = resolver.mapClassName("intermediary", "net.minecraft.class_2585");
            Class literalTextClass = Class.forName(literalTextClassName);
            literalTextConstructor = literalTextClass.getConstructor(String.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String formattedMethodName = resolver.mapMethodName("intermediary", "net.minecraft.class_5250", "method_27692", "(Lnet/minecraft/class_124;)Lnet/minecraft/class_5250;");
            formattedMethod = MutableText.class.getMethod(formattedMethodName, Formatting.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String newFindResourcesName = resolver.mapMethodName("intermediary", "net.minecraft.class_3300", "method_14488", "(Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/Map;");
            newFindResourcesMethod = ResourceManager.class.getMethod(newFindResourcesName, String.class, Predicate.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String oldFindResourcesName = resolver.mapMethodName("intermediary", "net.minecraft.class_3300", "method_14488", "(Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/Collection;");
            oldFindResourcesMethod = ResourceManager.class.getMethod(oldFindResourcesName, String.class, Predicate.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String newGetResourceName = resolver.mapMethodName("intermediary", "net.minecraft.class_5912", "method_14486", "(Lnet/minecraft/class_2960;)Ljava/util/Optional;");
            newGetResourceMethod = ResourceManager.class.getMethod(newGetResourceName, Identifier.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String oldGetResourceName = resolver.mapMethodName("intermediary", "net.minecraft.class_5912", "method_14486", "(Lnet/minecraft/class_2960;)Lnet/minecraft/class_3298;");
            oldGetResourceMethod = ResourceManager.class.getMethod(oldGetResourceName, Identifier.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String setScreenName = resolver.mapMethodName("intermediary", "net.minecraft.class_310", "method_1507", "(Lnet/minecraft/class_437;)V");
            setScreenMethod = MinecraftClient.class.getMethod(setScreenName, Screen.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            parseReaderMethod = JsonParser.class.getMethod("parseReader", Reader.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            jsonParserConstructor = JsonParser.class.getConstructor();
            parseMethod = JsonParser.class.getMethod("parse", Reader.class);
        } catch (ReflectiveOperationException ignored) {}

        try {
            String getGameVersionName = resolver.mapMethodName("intermediary", "net.minecraft.class_155", "method_16673", "()Lcom/mojang/bridge/game/GameVersion;");
            getGameVersionMethod = SharedConstants.class.getMethod(getGameVersionName);

            for (Method method : getGameVersionMethod.invoke(null).getClass().getMethods()) {
                // Since we don't have any mapping for getBuildTime(),
                // we're going to find it this way
                if (method.getReturnType() == Date.class) {
                    // Method returns Date, so it must be getBuildTime
                    getBuildTimeMethod = method;
                }
            }
        } catch (Exception ignored) {}

        try {
            String getInputStreamName = resolver.mapMethodName("intermediary", "net.minecraft.class_3298", "method_14482", "()Ljava/io/InputStream;");
            getInputStreamMethod = Resource.class.getMethod(getInputStreamName);
        } catch (ReflectiveOperationException ignored) {}
    }

    public static ButtonWidget buttonWidget(int x, int y, int width, int height, Text name, ButtonWidget.PressAction pressAction) {
        // In Minecraft 1.19.4 the ButtonWidget constructor is protected
        // and we have to construct ButtonWidgets through its Builder class,
        // but in versions older than 1.19.4 the Builder class doesn't exist and the constructor is public
        if (buttonWidgetConstructor != null) {
            try {
                return (ButtonWidget) buttonWidgetConstructor.newInstance(x, y, width, height, name, pressAction);
            } catch (ReflectiveOperationException e) {}
        } else {
            try {
                Object builder = buttonWidgetBuilderConstructor.newInstance(name, pressAction);
                buttonWidgetBuilderDimensionsMethod.invoke(builder, x, y, width, height);
                return (ButtonWidget) buttonWidgetBuilderBuildMethod.invoke(builder);
            } catch (ReflectiveOperationException e) {}
        }

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
            return (String) currentLanguageCodeField.get(languageManager);
        } catch (ReflectiveOperationException e) {}

        return null;
    }

    public static void setMultiplayerScreenTooltip(MultiplayerScreen screen, List<Text> tooltip) {
        // In Minecraft 1.19.4, setTooltip method uses List<OrderedText>, and in older versions it uses List<Text>
        // In Minecraft 1.19.4, there is a method setMultiPlayerScreenTooltip which uses List<Text>,
        // so we will use that for 1.19.4, and for older versions we will use setTooltip, which also uses List<Text>
        if (setMultiplayerScreenTooltipMethod != null) {
            try {
                setMultiplayerScreenTooltipMethod.invoke(screen, tooltip);
            } catch (ReflectiveOperationException ignored) {}
        } else {
            try {
                setTooltipMethod.invoke(screen, tooltip);
            } catch (ReflectiveOperationException ignored) {}
        }
    }

    public static MutableText translatableText(String text) {
        return translatableText(text, new Object[0]);
    }

    public static MutableText translatableText(String text, Object... args) {
        // In 1.19 to get translatable text you use Text.translatable, and in older versions
        // you use the class TranslatableText
        if (translatableMethod != null) {
            try {
                return (MutableText) translatableMethod.invoke(null, text, args);
            } catch (ReflectiveOperationException ignored) {}
        } else {
            try {
                return (MutableText) translatableTextConstructor.newInstance(text, args);
            } catch (ReflectiveOperationException ignored) {}
        }
        return null;
    }

    public static MutableText literalText(String text) {
        // In 1.19 to get literal text you use Text.literal, and in older versions
        // you use the class LiteralText
        if (literalMethod != null) {
            try {
                return (MutableText) literalMethod.invoke(null, text);
            } catch (ReflectiveOperationException ignored) {}
        } else {
            try {
                return (MutableText) literalTextConstructor.newInstance(text);
            } catch (ReflectiveOperationException ignored) {}
        }
        return null;
    }

    public static MutableText formatted(MutableText text, Formatting formatting) {
        // We're invoking formatted through reflection because
        // in some versions MutableText is a class and in some it's an interface
        try {
            return (MutableText) formattedMethod.invoke(text, formatting);
        } catch (ReflectiveOperationException ignored) {}

        return null;
    }

    public static Collection<Identifier> findResources(String startingPath) {
        // In Minecraft 1.19 the function findResources returns a Map<Identifier, Resource>,
        // and in older versions it returns a Collection<Identifier>
        // also, in Minecraft 1.19 the function takes a Predicate<Identifier>, and
        // in older versions it takes a Predicate<String>
        ResourceManager manager = MinecraftClient.getInstance().getResourceManager();

        if (newFindResourcesMethod != null) {
            try {
                Predicate<Identifier> identifierPredicate = (path) -> true;
                Map<Identifier, Resource> result = (Map<Identifier, Resource>) newFindResourcesMethod.invoke(manager, startingPath, identifierPredicate);
                return result.keySet();
            } catch (ReflectiveOperationException ignored) {}
        }

        if (oldFindResourcesMethod != null) {
            try {
                Predicate<String> stringPredicate = (path) -> true;
                return (Collection<Identifier>) oldFindResourcesMethod.invoke(manager, startingPath, stringPredicate);
            } catch (ReflectiveOperationException ignored) {}
        }
        return null;
    }

    public static Object getResource(Identifier identifier) {
        // In Minecraft 1.19 the function getResource returns an Optional<Resource>,
        // and in older versions it returns just Resource
        // Also, in newer versions Resource is a class, and in older versions it's an interface
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();

        if (newGetResourceMethod != null) {
            try {
                Object result = newGetResourceMethod.invoke(resourceManager, identifier);
                if (result instanceof Optional<?> optional) {
                    return optional.orElse(null);
                }
            } catch (ReflectiveOperationException ignored) {}
        }

        if (oldGetResourceMethod != null) {
            try {
                return oldGetResourceMethod.invoke(resourceManager, identifier);
            } catch (ReflectiveOperationException ignored) {}
        }
        return null;
    }

    public static void setScreen(Screen screen) {
        // In Minecraft 1.18 and above the function to open a screen is MinecraftClient.setScreen,
        // and in versions older than 1.18 it's named MinecraftClient.openScreen
        MinecraftClient client = MinecraftClient.getInstance();

        try {
            setScreenMethod.invoke(client, screen);
            return;
        } catch (ReflectiveOperationException ignored) {}
    }

    public static JsonElement parseReaderToJson(Reader reader) {
        // In newer versions in gson (in 1.18+), to parse a json object from a reader
        // you use a static method JsonParser.parseReader, and in older
        // versions you use the parse(Reader) method from an instance of this class
        if (parseReaderMethod != null) {
            try {
                return (JsonElement) parseReaderMethod.invoke(null, reader);
            } catch (ReflectiveOperationException ignored) {}
        } else {
            try {
                Object jsonParser = jsonParserConstructor.newInstance();
                return (JsonElement) parseMethod.invoke(jsonParser, reader);
            } catch (ReflectiveOperationException ignored) {}
        }
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
        if (getGameVersionMethod != null) {
            try {
                Object gameVersion = getGameVersionMethod.invoke(null);
                Date buildDate = (Date) getBuildTimeMethod.invoke(gameVersion);
                return buildDate.after(new Date(1670400844000L));
            } catch (ReflectiveOperationException e) {}
        }
        return false;
    }

    public static InputStream getResourceInputStream(Object resource) {
        // Since java complains that resource is a class but expects an interface
        // or the other way around, we have to do it this way
        try {
            InputStream inputStream = (InputStream) getInputStreamMethod.invoke(resource);
            return inputStream;
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }
}
