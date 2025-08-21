package dev.wiji.tbgm.misc;

import dev.wiji.tbgm.enums.Rank;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class Misc {
	public static String getUnformattedString(final String string) {
		return string.replaceAll("\udaff\udffc\ue006\udaff\udfff\ue002\udaff\udffe",
				"").replaceAll("\udaff\udffc\ue001\udb00\udc06", "")
				.replaceAll("§.", "").replaceAll("&.", "").replaceAll(
					"\\[[0-9:]+]", "").replaceAll("\\s+", " ").trim();
	}

	public static int convertToInt(String input) {
		input = input.replace(",", "");

		double v = Double.parseDouble(input.substring(0, input.length() - 1));
		if (input.endsWith("k") || input.endsWith("K")) {
			return (int) (v * 1000);
		} else if (input.endsWith("m") || input.endsWith("M")) {
			return (int) (v * 1000000);
		} else {
			double value = Double.parseDouble(input);
			return (int) value;
		}
	}

	public static void sendTbgmErrorMessage(String message) {
		sendTbgmMessage(message, 0xd64d4b);
	}

	public static void sendTbgmSuccessMessage(String message) {
		sendTbgmMessage(message, 0x5bc97e);
	}

	public static void sendTbgmMessage(String message) {
		sendTbgmMessage(message, 0xcfc7b0);
	}

	public static void sendTbgmMessage(String message, int color) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getNetworkHandler() == null || client.player == null) {
			return;
		}
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

		Rank tbgmRank = Rank.TBGM;

		MutableText guildAlertComponent = Text.literal(tbgmRank.getBackgroundText())
				.setStyle(Style.EMPTY
							.withColor(0x242424)
							.withFont(Identifier.of("minecraft", "banner/pill")));

		MutableText guildAlertForegroundComponent = Text.literal(tbgmRank.getForegroundText())
				.setStyle(Style.EMPTY
							.withColor(tbgmRank.getRankColor())
							.withFont(Identifier.of("minecraft", "banner/pill"))
							.withShadowColor(16777215));

		MutableText bodyComponent = Text.literal(" " + message)
				.setStyle(Style.EMPTY
							.withColor(color)
							.withFont(Identifier.of("minecraft", "default")));

		MutableText finalMessage = guildAlertComponent
				.append(guildAlertForegroundComponent)
				.append(bodyComponent);

		GameMessageS2CPacket packet = new GameMessageS2CPacket(finalMessage, false);
		client.execute(() -> networkHandler.onGameMessage(packet));
	}
}

