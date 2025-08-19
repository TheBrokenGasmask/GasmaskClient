package dev.wiji.tbgm.controllers;

import dev.wiji.tbgm.GasmaskMain;
import dev.wiji.tbgm.misc.Misc;
import dev.wiji.tbgm.objects.Aspect;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AspectReport {
	private static long lastCallTime = 0;

	public static void parseChatMessage(Text message) {
		final String regex = "([A-Za-z0-9_ ]+?) rewarded an Aspect to ([A-Za-z0-9_ ]+?)";
		String unformattedMessage = Misc.getUnformattedString(message.getString());
		Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(unformattedMessage);
		HashMap<String, List<String>> nameMap = new HashMap<>();
		GetRealName.createRealNameMap(message, nameMap);
		if (!matcher.matches()) return;
		String giver = matcher.group(1);
		if (nameMap.containsKey(giver)) {
			giver = nameMap.get(giver).removeFirst();
		}
		String receiver = matcher.group(2);
		if (nameMap.containsKey(receiver)) {
			receiver = nameMap.get(receiver).removeFirst();
		}

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;

		Aspect aspect = new Aspect(giver, receiver, player.getGameProfile().getId());
		new Thread(() -> reportAspect(aspect)).start();
	}

	public static void reportAspect(Aspect aspect) {
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastCallTime < 250) return;

		lastCallTime = currentTime;

		Map<String, String> parameters = new HashMap<>();
		parameters.put("giver", aspect.giver);
		parameters.put("receiver", aspect.receiver);
		parameters.put("reporter", aspect.reporter.toString());
		parameters.put("token", Authentication.token);

		StringBuilder urlBuilder = new StringBuilder();
		String baseUrl = GasmaskMain.getApiUrl();
		if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

		urlBuilder.append(baseUrl).append("/api/report-aspect?");
		parameters.forEach((key, value) -> urlBuilder.append(key).append("=").append(value).append("&"));

		try {
			URL url = URI.create(urlBuilder.toString()).toURL();

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			int responseCode = conn.getResponseCode();;

			if (responseCode < 200 || responseCode > 299) {
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
				String inputLine;
				StringBuilder response = new StringBuilder();

				while ((inputLine = in.readLine()) != null) response.append(inputLine);
				in.close();

				ClientPlayerEntity player = MinecraftClient.getInstance().player;
				if (player == null) return;
				player.sendMessage(Text.literal("§cFailed to report Aspect: " + response), false);
			}

			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
