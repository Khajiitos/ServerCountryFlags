package me.khajiitos.betterclocks;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterClocks implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("betterclocks");
	public static boolean USE12HOUR = false;

	@Override
	public void onInitialize() {

	}

	public static MutableText getTimeText() {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null || world.getDimension().hasFixedTime())
			return Text.literal(USE12HOUR ? "00:00 ??" : "00:00").formatted(Formatting.OBFUSCATED);

		long time = (world.getTimeOfDay() + 6000) % 24000;
		int hour = (int)(time / 1000);
		int minute = (int)((time % 1000) / (1000.0 / 60.0));

		String hourStr = "";
		String minuteStr = (minute < 10) ? ("0" + minute) : "" + minute;
		String restStr = "";

		if (USE12HOUR) {
			if (hour == 0) {
				hourStr = "12";
				restStr = " AM";
			} else if (hour == 12) {
				hourStr = "12";
				restStr = " PM";
			} else if (hour > 12) {
				hourStr = "" + (hour - 12);
				restStr = " PM";
			} else {
				hourStr = "" + hour;
				restStr = " AM";
			}
		} else {
			hourStr = (hour < 10) ? ("0" + hour) : "" + hour;
		}

		return Text.literal(String.format("%s:%s%s", hourStr, minuteStr, restStr)).formatted(Formatting.BOLD);
	}
}
