package me.khajiitos.servercountryflags.common.util;

import net.minecraft.network.chat.Component;

import java.util.List;

public record FlagRenderInfo(String countryCode, double flagAspectRatio, List<Component> tooltip) {}
