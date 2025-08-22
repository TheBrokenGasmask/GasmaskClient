package dev.wiji.tbgm.misc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
public class WynntilsConfig {
    private static final Gson gson = new Gson();

    public static void modifyWynntilsConfig() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path wynntilsConfigDir = gameDir.resolve("wynntils").resolve("config");

        if (!Files.exists(wynntilsConfigDir)) {
            System.out.println("Wynntils config directory not found at "+ wynntilsConfigDir);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(wynntilsConfigDir, "*.conf.json")) {
            for (Path configFile : stream) {
                processConfigFile(configFile);
            }
        } catch (IOException e) {
            System.out.println("Error reading Wynntils config directory "+ e);
        }
    }

    private static void processConfigFile(Path configFile) {
        try {
            String content = Files.readString(configFile);
            JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();

            boolean modified = false;

            if (jsonObject.has("autoSkipCutscenesFeature.skipCondition")) {
                String currentSkipCondition = jsonObject.get("autoSkipCutscenesFeature.skipCondition").getAsString();
                if (!"group".equals(currentSkipCondition)) {
                    jsonObject.addProperty("autoSkipCutscenesFeature.skipCondition", "group");
                    modified = true;
                }
            }

            if (jsonObject.has("autoSkipCutscenesFeature.userEnabled")) {
                boolean currentUserEnabled = jsonObject.get("autoSkipCutscenesFeature.userEnabled").getAsBoolean();
                if (!currentUserEnabled) {
                    jsonObject.addProperty("autoSkipCutscenesFeature.userEnabled", true);
                    modified = true;
                }
            }

            if (modified) {
                String modifiedContent = gson.toJson(jsonObject);
                Files.writeString(configFile, modifiedContent, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Successfully updated Wynntils config file");
            } else {
                System.out.println("No Wynntils config modifications needed");
            }

        } catch (IOException e) {
            System.out.println("Error processing config file: "+ configFile.getFileName()+" "+ e);
        } catch (Exception e) {
            System.out.println("Error parsing JSON in config file: "+ configFile.getFileName()+" "+ e);
        }
    }
}
