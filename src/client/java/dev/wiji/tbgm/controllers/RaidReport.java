package dev.wiji.tbgm.controllers;

import dev.wiji.tbgm.GasmaskClient;
import dev.wiji.tbgm.enums.RaidType;
import dev.wiji.tbgm.misc.Misc;
import dev.wiji.tbgm.objects.Raid;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaidReport {

	public static void parseChatMessage(Text message) {
		String unformattedMessage = Misc.getUnformattedString(message.getString());
		Matcher matcher = Pattern.compile("([A-Za-z0-9_ ]+?), ([A-Za-z0-9_ ]+?), ([A-Za-z0-9_ ]+?), and " +
				"([A-Za-z0-9_ ]+?) finished (.+?) and claimed (\\d+)x Aspects, (\\d+)x Emeralds, .(.+?m)" +
				" Guild Experience, and \\+(\\d+) Seasonal Rating", Pattern.MULTILINE).matcher(unformattedMessage);

		HashMap<String, List<String>> nameMap = new HashMap<>();
		GetRealName.createRealNameMap(message, nameMap);

		if (!matcher.matches()) {
			matcher = Pattern.compile("([A-Za-z0-9_ ]+?), ([A-Za-z0-9_ ]+?), ([A-Za-z0-9_ ]+?), and " +
					"([A-Za-z0-9_ ]+?) finished (.+?) and claimed (\\d+)x Aspects, (\\d+)x Emeralds, and .(.+?m)" +
					" Guild Experience", Pattern.MULTILINE).matcher(unformattedMessage);
		};

		if (!matcher.matches()) return;

		String user1 = matcher.group(1);
		if (nameMap.containsKey(user1)) user1 = nameMap.get(user1).removeLast();

		String user2 = matcher.group(2);
		if (nameMap.containsKey(user2)) user2 = nameMap.get(user2).removeLast();

		String user3 = matcher.group(3);
		if (nameMap.containsKey(user3)) user3 = nameMap.get(user3).removeLast();

		String user4 = matcher.group(4);
		if (nameMap.containsKey(user4)) user4 = nameMap.get(user4).removeLast();

		String raidString = matcher.group(5);
		String aspects = matcher.group(6);
		String emeralds = matcher.group(7);
		String xp = matcher.group(8);
		String sr = matcher.groupCount() >= 9 ? matcher.group(9) : "0";

		RaidType raidType = RaidType.getRaidType(raidString);
		UUID reporterID = MinecraftClient.getInstance().getGameProfile().getId();

		Raid raid = new Raid(raidType, new String[]{user1, user2, user3, user4}, reporterID, Integer.parseInt(sr), Misc.convertToInt(xp));

		Authentication.getWebSocketManager().sendRaidReport(raid);
	}

	public static void handleRaidReportResponse(boolean success, String message, String error) {
		if (success) {
			Misc.sendTbgmSuccessMessage("Successfully reported raid!");
		} else {
			String errorMsg = error != null ? error : message;
			Misc.sendTbgmErrorMessage("Failed to report raid: " + errorMsg);
		}
	}
}
