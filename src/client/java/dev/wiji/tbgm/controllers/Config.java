package dev.wiji.tbgm.controllers;

import com.google.gson.Gson;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
	private static final Path configDir = Paths.get(MinecraftClient.getInstance().runDirectory.getPath() + "/config");
	private static final Path configFile = Paths.get(configDir + "/gasmask.json");
	private static ConfigData configData;

	public static ConfigData getConfigData() {
		if (configData != null) return configData;

		try {
			if (!Files.exists(configFile)) {
				Files.createDirectories(configDir);
				Files.createFile(configFile);
				configData = ConfigData.getDefault();
				configData.save();
				return configData;
			}
		} catch (IOException e) {
			e.printStackTrace();
			configData = ConfigData.getDefault();
			return configData;
		}
		try {
			Gson gson = new Gson();
			FileReader reader = new FileReader(configFile.toFile());
			configData = gson.fromJson(reader, ConfigData.class);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			configData = ConfigData.getDefault();
		}
		return configData;
	}

	public static Screen createConfigScreen(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Text.literal("Gasmask Configuration"));

		ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Custom Guild Rank Colors"), getConfigData().customGuildRankColors)
				.setDefaultValue(true)
				.setTooltip(Text.literal("Enable custom colors for guild ranks"))
				.setSaveConsumer(newValue -> getConfigData().customGuildRankColors = newValue)
				.build());

		general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Custom Guild Chat Colors"), getConfigData().customGuildChatColors)
				.setDefaultValue(true)
				.setTooltip(Text.literal("Enable custom colors for guild chat messages"))
				.setSaveConsumer(newValue -> getConfigData().customGuildChatColors = newValue)
				.build());

		general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Wynnpool Mythic Weights"), getConfigData().wynnpoolMythicWeights)
				.setDefaultValue(true)
				.setTooltip(Text.literal("Enable Wynnpool mythic item weights in tooltips"))
				.setSaveConsumer(newValue -> getConfigData().wynnpoolMythicWeights = newValue)
				.build());

		general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Nori Mythic Weights"), getConfigData().noriMythicWeights)
				.setDefaultValue(true)
				.setTooltip(Text.literal("Enable Nori mythic item weights in tooltips"))
				.setSaveConsumer(newValue -> getConfigData().noriMythicWeights = newValue)
				.build());

		builder.setSavingRunnable(() -> {
			getConfigData().save();
		});

		return builder.build();
	}

	public static class ConfigData {
		public boolean customGuildRankColors = true;
		public boolean customGuildChatColors = true;
		public boolean wynnpoolMythicWeights = true;
		public boolean noriMythicWeights = true;

		public ConfigData() {

		}

		public static ConfigData getDefault() {
			ConfigData config = new ConfigData();
			config.customGuildRankColors = true;
			config.customGuildChatColors = true;
			config.wynnpoolMythicWeights = true;
			config.noriMythicWeights = true;
			return config;
		}

		public void save() {
			try {
				Gson gson = new Gson();
				FileWriter writer = new FileWriter(configFile.toFile());
				gson.toJson(this, writer);
				writer.flush();
				writer.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}