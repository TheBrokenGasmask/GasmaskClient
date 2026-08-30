package dev.wiji.tbgm.controllers;

import dev.wiji.tbgm.enums.RaidType;
import dev.wiji.tbgm.misc.Misc;
import dev.wiji.tbgm.objects.Raid;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaidReport {
	public static final Pattern COMPLETION_TIME_PATTERN = Pattern.compile("^\\s*§7\uDB00\uDC6BTime\\s+Elapsed:\\s*\\d{2}:\\d{2}\\s*$");

	private static int lastCompletionTime = -1;
	private static long completionTimeTimestamp = 0;
	private static final long EXPIRATION_MS = 5000;

	// <players> finished <raid> and claimed <rewards>
	// <players> is a natural-language list of 1-4 names: "A", "A and B", "A, B, and C", "A, B, C, and D".
	// <rewards> is a natural-language list where any of Aspects / Emeralds / Guild Experience /
	// Seasonal Rating may be absent.
	public static final Pattern COMPLETION_PATTERN =
			Pattern.compile("(.+?) finished (.+?) and claimed (.+)");
	private static final Pattern NAME_LIST_SPLIT_PATTERN =
			Pattern.compile(",\\s*and\\s+|\\s+and\\s+|,\\s*");
	private static final Pattern GUILD_XP_PATTERN =
			Pattern.compile("([\\d,.]+[kKmM]?) Guild Experience");
	private static final Pattern SEASONAL_RATING_PATTERN =
			Pattern.compile("\\+(\\d+) Seasonal Rating");

	public static void parseChatMessage(Text message) {
		parseCompletionTime(message);

		String unformattedMessage = Misc.getUnformattedString(message.getString());
		Matcher matcher = COMPLETION_PATTERN.matcher(unformattedMessage);
		if (!matcher.matches()) return;

		RaidType raidType = RaidType.getRaidType(matcher.group(2));
		if (raidType == null) return;

		HashMap<String, List<String>> nameMap = new HashMap<>();
		GetRealName.createRealNameMap(message, nameMap);

		List<String> players = new ArrayList<>();
		for (String rawName : NAME_LIST_SPLIT_PATTERN.split(matcher.group(1))) {
			String name = rawName.trim();
			if (name.isEmpty()) continue;
			if (nameMap.containsKey(name)) name = nameMap.get(name).removeLast();
			players.add(name);
		}
		if (players.isEmpty() || players.size() > 4) return;

		String rewards = matcher.group(3);

		int guildXP = 0;
		Matcher xpMatcher = GUILD_XP_PATTERN.matcher(rewards);
		if (xpMatcher.find()) guildXP = Misc.convertToInt(xpMatcher.group(1));

		int seasonRating = 0;
		Matcher srMatcher = SEASONAL_RATING_PATTERN.matcher(rewards);
		if (srMatcher.find()) seasonRating = Integer.parseInt(srMatcher.group(1));

		UUID reporterID = MinecraftClient.getInstance().getSession().getUuidOrNull();
		if (reporterID == null) return;

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if(player == null) return;

		String playerName = player.getName().getString();
		boolean playerInRaid = players.stream().anyMatch(name -> name.equalsIgnoreCase(playerName));

		int completionTime = -1;
		if (System.currentTimeMillis() - completionTimeTimestamp < EXPIRATION_MS && playerInRaid) {
			completionTime = lastCompletionTime;
		}

		Raid raid = new Raid(raidType, players.toArray(new String[0]), reporterID, seasonRating, guildXP, completionTime);

		lastCompletionTime = -1;
		completionTimeTimestamp = 0;

		Authentication.getWebSocketManager().sendRaidReport(raid);
	}

	public static void parseCompletionTime(Text message) {
		Matcher matcher = COMPLETION_TIME_PATTERN.matcher(message.getString());

		if (!matcher.matches()) return;

		String timeString = matcher.group(0).trim().split(": ")[1];
		String[] timeParts = timeString.split(":");
		int minutes = Integer.parseInt(timeParts[0]);
		int seconds = Integer.parseInt(timeParts[1]);


		lastCompletionTime = (minutes * 60) + seconds;
		completionTimeTimestamp = System.currentTimeMillis();
	}
}