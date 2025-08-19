package dev.wiji.wynntracker.controllers;

import com.google.gson.Gson;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
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

public class Config implements ModMenuApi {
	private static final Path configDir = Paths.get(MinecraftClient.getInstance().runDirectory.getPath() + "/config");
	private static final Path configFile = Paths.get(configDir + "/wynntracker.json");
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
		} catch (IOException e) {
			e.printStackTrace();
			configData = ConfigData.getDefault();
		}
		return configData;
	}

	public static Screen createConfigScreen(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.literal("WynnTracker Config"));

		ConfigCategory general = builder.getOrCreateCategory(Text.literal("General Config"));
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		builder.setSavingRunnable(configData::save);

		return builder.build();
	}

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return Config::createConfigScreen;
	}

	public static class ConfigData {
		public ConfigData() {

		}

		public static ConfigData getDefault() {
			return new ConfigData();
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