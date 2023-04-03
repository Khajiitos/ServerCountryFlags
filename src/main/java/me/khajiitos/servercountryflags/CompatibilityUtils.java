package me.khajiitos.servercountryflags;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class CompatibilityUtils {
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
}
