package dev.wiji.tbgm.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.wiji.tbgm.GasmaskClient;
import dev.wiji.tbgm.GasmaskMain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Scanner;

public class Updater {
    public static void checkForUpdates() {
        new Thread(() -> {
            try {
                URL url = URI.create(GasmaskMain.getRepoUrl()).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    GasmaskClient.LOGGER.error("Failed to check for updates. Response code: " + responseCode);
                    return;
                }

                Scanner scanner = new Scanner(url.openStream());
                StringBuilder inline = new StringBuilder();
                while (scanner.hasNext()) {
                    inline.append(scanner.nextLine());
                }
                scanner.close();

                Gson gson = new Gson();
                JsonObject release = gson.fromJson(inline.toString(), JsonObject.class);
                String latestVersion = release.get("tag_name").getAsString();
                String currentVersion = GasmaskClient.MOD_VERSION;

                if (latestVersion.equals(currentVersion)) {
                    GasmaskClient.LOGGER.info("Mod is up to date.");
                    return;
                }

                GasmaskClient.LOGGER.info("New version available: " + latestVersion);

                String downloadUrl = null;
                String sha256 = null;

                for (int i = 0; i < release.get("assets").getAsJsonArray().size(); i++) {
                    JsonObject asset = release.get("assets").getAsJsonArray().get(i).getAsJsonObject();
                    if (asset.get("name").getAsString().endsWith(".jar")) {
                        downloadUrl = asset.get("browser_download_url").getAsString();
                        sha256 = asset.get("digest").getAsString().replace("sha256:", "");
                        break;
                    }
                }

                if (downloadUrl == null) {
                    GasmaskClient.LOGGER.error("Could not find download URL for the latest release.");
                    return;
                }

                URL downloadURL = URI.create(downloadUrl).toURL();
                HttpURLConnection downloadConnection = (HttpURLConnection) downloadURL.openConnection();
                downloadConnection.setRequestMethod("GET");
                downloadConnection.connect();

                Path tempFile = Files.createTempFile(GasmaskMain.getModId(), ".jar");
                try (InputStream in = downloadConnection.getInputStream();
                     FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
                String checksum = getFileChecksum(sha256Digest, tempFile.toFile());

                if (!checksum.equals(sha256)) {
                    GasmaskClient.LOGGER.error("SHA256 checksum does not match. Aborting update.");
                    return;
                }

                Path modsDir = new File(".").toPath().resolve("mods");
                Path targetFile = modsDir.resolve(GasmaskMain.getModId() + "-" + latestVersion + ".jar");

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        GasmaskClient.LOGGER.info("Mod updated successfully. Please restart the game.");
                    } catch (Exception e) {
                        GasmaskClient.LOGGER.error("Failed to move updated mod file.", e);
                    }
                }));

            } catch (Exception e) {
                GasmaskClient.LOGGER.error("Failed to check for updates.", e);
            }
        }).start();
    }

    private static String getFileChecksum(MessageDigest digest, File file) throws Exception {
        try (InputStream fis = Files.newInputStream(file.toPath())) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}