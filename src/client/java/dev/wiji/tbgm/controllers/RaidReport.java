package dev.wiji.tbgm.controllers;

import dev.wiji.tbgm.GasmaskMain;
import dev.wiji.tbgm.enums.RaidType;
import dev.wiji.tbgm.misc.Misc;
import dev.wiji.tbgm.objects.Raid;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaidReport {
	private static long lastCallTime = 0;

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

		new Thread(() -> reportRaid(raid)).start();
	}

	public static void reportRaid(Raid raid) {
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastCallTime < 100) return;

		lastCallTime = currentTime;

		Map<String, String> parameters = new HashMap<>();
		parameters.put("raid", String.valueOf(raid.raidType.id));
		parameters.put("reporter", raid.reporter.toString());
		parameters.put("token", Authentication.token);
		parameters.put("seasonRating", String.valueOf(raid.seasonRating));
		parameters.put("guildXP", String.valueOf(raid.guildXP));

		for(int i = 0; i < raid.players.length; i++) parameters.put("player" + (i + 1), raid.players[i]);

		StringBuilder urlBuilder = new StringBuilder();
		String baseUrl = GasmaskMain.getApiUrl();
		if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

		urlBuilder.append(baseUrl).append("/api/report-raid?");
		parameters.forEach((key, value) -> urlBuilder.append(key).append("=").append(value).append("&"));

		try {
			URL url = URI.create(urlBuilder.toString()).toURL();
			System.out.println("Sending raid report to: " + urlBuilder);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			int responseCode = conn.getResponseCode();
			String responseMessage = conn.getResponseMessage();

			System.out.println("Sending raid response code: " + responseCode);
			System.out.println("Sending raid response message: " + responseMessage);

			if (responseCode < 200 || responseCode > 299) {
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
				String inputLine;
				StringBuilder response = new StringBuilder();

				while ((inputLine = in.readLine()) != null) response.append(inputLine);
				in.close();

				ClientPlayerEntity player = MinecraftClient.getInstance().player;
				if (player == null) return;
				player.sendMessage(Text.literal("§cFailed to report raid: " + response), false);
			} else {
				ClientPlayerEntity player = MinecraftClient.getInstance().player;
				if (player == null) return;
				player.sendMessage(Text.literal("§aSuccessfully reported raid!"), false);
			}

			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
