package me.khajiitos.servercountryflags.common.config;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ClothConfigScreenMaker {

    public static Screen create(Minecraft minecraft, Screen parent) {
        return create(parent);
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(new TextComponent("Server Country Flags"))
                .setSavingRunnable(Config::save);

        // Saving the config will cause it to be written to and then instantly loaded from file again.
        // Probably not a big deal though.

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        for (Field field : Config.Values.class.getDeclaredFields()) {
            addEntryForField(field, builder, entryBuilder);
        }

        return builder.build();
    }

    public static void addEntryForField(Field field, ConfigBuilder configBuilder, ConfigEntryBuilder entryBuilder) {
        ConfigEntry annotation = field.getAnnotation(ConfigEntry.class);

        if (annotation == null) {
            return;
        }

        ConfigCategory category = configBuilder.getOrCreateCategory(new TextComponent(annotation.configCategory()));

        try {
            if (field.getType() == int.class) {
                Optional<Constraints> constraints = Arrays.stream(annotation.constraints()).findFirst();
                category.addEntry(entryBuilder.startIntField(new TextComponent(annotation.name()), field.getInt(Config.cfg))
                        .setTooltip(new TextComponent(annotation.description()))
                        .setDefaultValue(field.getInt(Config.DEFAULT))
                        .setSaveConsumer(newValue -> setCfgInt(field, newValue))
                        .setMin(constraints.isPresent() ? constraints.get().minValue() : null)
                        .setMax(constraints.isPresent() ? constraints.get().maxValue() : null)
                        .build());
            } else if (field.getType() == boolean.class) {
                category.addEntry(entryBuilder.startBooleanToggle(new TextComponent(annotation.name()), field.getBoolean(Config.cfg))
                        .setTooltip(new TextComponent(annotation.description()))
                        .setDefaultValue(field.getBoolean(Config.DEFAULT))
                        .setSaveConsumer(newValue -> setCfgBoolean(field, newValue))
                        .build());
            } else if (field.getType() == String.class) {
                if (annotation.stringValues() != null) {
                    category.addEntry(entryBuilder.startStringDropdownMenu(new TextComponent(annotation.name()), (String)field.get(Config.cfg))
                            .setSelections(List.of(annotation.stringValues()))
                            .setTooltip(new TextComponent(annotation.description()))
                            .setDefaultValue((String)field.get(Config.DEFAULT))
                            .setSaveConsumer(newValue -> setCfgString(field, newValue))
                            .build());
                } else {
                    category.addEntry(entryBuilder.startStrField(new TextComponent(annotation.name()), (String)field.get(Config.cfg))
                            .setTooltip(new TextComponent(annotation.description()))
                            .setDefaultValue((String)field.get(Config.DEFAULT))
                            .setSaveConsumer(newValue -> setCfgString(field, newValue))
                            .build());
                }
            }
        } catch (IllegalAccessException e) {
            ServerCountryFlags.LOGGER.error("Failed to access a field", e);
        }
    }

    private static void setCfgInt(Field field, int value) {
        try {
            field.setInt(Config.cfg, value);
        } catch (IllegalAccessException e) {
            ServerCountryFlags.LOGGER.error("Failed to set value to a field", e);
        }
    }

    private static void setCfgBoolean(Field field, boolean value) {
        try {
            field.setBoolean(Config.cfg, value);
        } catch (IllegalAccessException e) {
            ServerCountryFlags.LOGGER.error("Failed to set value to a field", e);
        }
    }

    private static void setCfgString(Field field, String string) {
        try {
            field.set(Config.cfg, string);
        } catch (IllegalAccessException e) {
            ServerCountryFlags.LOGGER.error("Failed to set value to a field", e);
        }
    }
}
