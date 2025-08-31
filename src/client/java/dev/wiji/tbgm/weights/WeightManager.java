package dev.wiji.tbgm.weights;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WeightManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeightManager.class);
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static WeightManager INSTANCE;

    private final Map<String, Map<String, Map<String, Double>>> weights = new HashMap<>();
    private boolean weightsLoaded = false;
    private boolean addNoriWeights;
    private boolean addWynnpoolWeights;

    public WeightManager(boolean addNoriWeights, boolean addWynnpoolWeights) {
        this.addNoriWeights = addNoriWeights;
        this.addWynnpoolWeights = addWynnpoolWeights;
    }

    public static void initialize(boolean addNoriWeights, boolean addWynnpoolWeights) {
        if (INSTANCE == null) {
            INSTANCE = new WeightManager(addNoriWeights, addWynnpoolWeights);
            INSTANCE.addWeights();
        }
    }

    public static WeightManager getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<Void> addWeights() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (addNoriWeights) {
                    loadNoriWeights();
                }

                if (addWynnpoolWeights) {
                    loadWynnpoolWeights();
                }

                weightsLoaded = true;
                LOGGER.info("Successfully loaded mythic item weights");
            } catch (Exception e) {
                LOGGER.error("Failed to load weights", e);
                showError("Failed to load weights: " + e.getMessage());
            }
        });
    }

    private void loadNoriWeights() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://nori.fish/api/item/mythic"))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Nori API returned status code: " + response.statusCode());
        }

        JsonObject jsonResponse = GSON.fromJson(response.body(), JsonObject.class);
        JsonObject weightsParsed = jsonResponse.getAsJsonObject("weights");

        if (weightsParsed == null || weightsParsed.size() == 0) {
            throw new RuntimeException("No mythic weights found from Nori API");
        }

        for (String mythicKey : weightsParsed.keySet()) {
            weights.computeIfAbsent(mythicKey, k -> new HashMap<>());

            JsonObject mythicWeights = weightsParsed.getAsJsonObject(mythicKey);
            for (String scaleKey : mythicWeights.keySet()) {
                String modifiedScaleKey = scaleKey + ";Nori"; // In case Nori and Wynnpool have a scale with the same name

                Map<String, Double> scaleWeights = new HashMap<>();
                JsonObject scaleData = mythicWeights.getAsJsonObject(scaleKey);

                for (String identificationKey : scaleData.keySet()) {
                    scaleWeights.put(identificationKey, scaleData.get(identificationKey).getAsDouble());
                }

                weights.get(mythicKey).put(modifiedScaleKey, scaleWeights);
            }
        }

        LOGGER.info("Successfully loaded Nori weights for {} mythic items", weightsParsed.size());
    }

    private void loadWynnpoolWeights() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynnpool.com/item/weight/all"))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Wynnpool API returned status code: " + response.statusCode());
        }

        JsonArray weightsParsed = GSON.fromJson(response.body(), JsonArray.class);

        if (weightsParsed == null || weightsParsed.size() == 0) {
            throw new RuntimeException("No mythic weights found from Wynnpool API");
        }

        for (JsonElement scaleElement : weightsParsed) {
            JsonObject scale = scaleElement.getAsJsonObject();
            String mythicKey = scale.get("item_name").getAsString();

            weights.computeIfAbsent(mythicKey, k -> new HashMap<>());

            List<IdentificationWeight> identifications = new ArrayList<>();
            JsonObject identificationsObj = scale.getAsJsonObject("identifications");

            for (String identificationKey : identificationsObj.keySet()) {
                double weight = identificationsObj.get(identificationKey).getAsDouble() * 100;
                identifications.add(new IdentificationWeight(identificationKey, weight));
            }

            identifications.sort((a, b) -> Double.compare(b.weight, a.weight));

            String scaleKey = scale.get("weight_name").getAsString() + ";Wynnpool"; // In case Nori and Wynnpool have a scale with the same name

            Map<String, Double> scaleWeights = new LinkedHashMap<>(); // LinkedHashMap to preserve order
            for (IdentificationWeight identification : identifications) {
                scaleWeights.put(identification.name, identification.weight);
            }

            weights.get(mythicKey).put(scaleKey, scaleWeights);
        }

        LOGGER.info("Successfully loaded Wynnpool weights for {} scales", weightsParsed.size());
    }

    private void showError(String message) {
        LOGGER.error(message);
    }

    public Map<String, Map<String, Map<String, Double>>> getWeights() {
        return Collections.unmodifiableMap(weights);
    }

    public boolean isWeightsLoaded() {
        return weightsLoaded;
    }

    public Map<String, Map<String, Double>> getWeightsForItem(String itemName) {
        return weights.getOrDefault(itemName, Collections.emptyMap());
    }

    private static class IdentificationWeight {
        final String name;
        final double weight;

        IdentificationWeight(String name, double weight) {
            this.name = name;
            this.weight = weight;
        }
    }
}