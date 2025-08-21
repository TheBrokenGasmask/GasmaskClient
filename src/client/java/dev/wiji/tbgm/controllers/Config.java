package dev.wiji.tbgm.controllers;

import com.google.gson.Gson;
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
		} catch (IOException e) {
			e.printStackTrace();
			configData = ConfigData.getDefault();
		}
		return configData;
	}

	public static Screen createConfigScreen(Screen parent) {
		// Simple fallback screen when cloth-config is not available
		return parent;
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