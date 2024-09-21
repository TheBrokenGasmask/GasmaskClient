package dev.wiji.wynntracker.controllers;

import com.google.gson.Gson;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.ConfigData;
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
	private static final Path config_dir = Paths.get(MinecraftClient.getInstance().runDirectory.getPath() + "/config");
	private static final Path config_file = Paths.get(config_dir + "/wynntracker.json");
	private static ConfigData config_data;

	public static ConfigData getConfigData() {
		if (config_data != null) return config_data;

		try {
			if (!Files.exists(config_file)) {
				Files.createDirectories(config_dir);
				Files.createFile(config_file);
				config_data = ConfigData.getDefault();
				config_data.save();
				return config_data;
			}
		} catch (IOException e) {
			e.printStackTrace();
			config_data = ConfigData.getDefault();
			return config_data;
		}
		try {
			Gson gson = new Gson();
			FileReader reader = new FileReader(config_file.toFile());
			config_data = gson.fromJson(reader, ConfigData.class);
		} catch (IOException e) {
			e.printStackTrace();
			config_data = ConfigData.getDefault();
		}
		return config_data;
	}

	public static Screen createConfigScreen(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.literal("WynnTracker Config"));

		ConfigCategory general = builder.getOrCreateCategory(Text.literal("General Config"));

		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		general.addEntry(entryBuilder.startStrField(Text.literal("Remote Server URL"), config_data.apiUrl).setDefaultValue("http://localhost:3000").setTooltip(Text.of("Set the Base URL of the remote server")).setSaveConsumer(newValue -> {
			config_data.apiUrl = newValue;
			Authentication.sendAuthRequest();
		}).build());

		builder.setSavingRunnable(config_data::save);

		return builder.build();
	}

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return Config::createConfigScreen;
	}

	public static class ConfigData {
		public String apiUrl;

		public ConfigData(String apiUrl) {
			this.apiUrl = apiUrl;
		}

		public static ConfigData getDefault() {
			return new ConfigData("http://localhost:3000");
		}

		public void save() {
			try {
				Gson gson = new Gson();
				FileWriter writer = new FileWriter(config_file.toFile());
				gson.toJson(this, writer);
				writer.flush();
				writer.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}