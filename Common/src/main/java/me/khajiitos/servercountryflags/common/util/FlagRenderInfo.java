package me.khajiitos.servercountryflags.common.util;

import net.minecraft.network.chat.Component;

public record FlagRenderInfo(String countryCode, double flagAspectRatio, Component tooltip) {}
