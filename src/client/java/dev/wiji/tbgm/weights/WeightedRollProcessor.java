package dev.wiji.tbgm.weights;

import com.wynntils.models.items.properties.IdentifiableItemProperty;
import com.wynntils.models.stats.StatCalculator;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WeightedRollProcessor {

    public static void processWeightedRolls(ItemStack itemStack, IdentifiableItemProperty<?, ?> itemInfo) {
        WeightManager weightManager = WeightManager.getInstance();
        List<WeightedRollSummary.ScaleLine> scaleLines = new ArrayList<>();

        if (weightManager == null || !weightManager.isWeightsLoaded()) {
            return;
        }

        String itemName = itemInfo.getName();
        Map<String, Map<String, Double>> itemWeights = weightManager.getWeightsForItem(itemName);

        if (itemWeights.isEmpty()) {
            return;
        }

        List<StatActualValue> identifications = itemInfo.getIdentifications();
        List<StatPossibleValues> possibleValues = itemInfo.getPossibleValues();

        if (identifications.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Map<String, Double>> scaleEntry : itemWeights.entrySet()) {
            String fullScaleName = scaleEntry.getKey();
            Map<String, Double> scaleWeights = scaleEntry.getValue();

            String[] parts = fullScaleName.split(";");
            String scaleName = parts[0];
            String source = parts.length > 1 ? parts[1] : "Unknown";

            double totalWeightedScore = 0.0;
            double totalPossibleScore = 0.0;
            boolean hasApplicableStats = false;
            List<WeightedRollSummary.StatContribution> contributions = new ArrayList<>();

            for (StatActualValue stat : identifications) {
                StatType statType = stat.statType();
                String apiName = statType.getApiName();

                Double weight = scaleWeights.get(apiName);
                if (weight == null) {
                    continue;
                }

                StatPossibleValues possibleValue = possibleValues.stream()
                        .filter(pv -> pv.statType().equals(statType))
                        .findFirst()
                        .orElse(null);

                if (possibleValue == null || possibleValue.range().isFixed()) {
                    continue;
                }

                float rollPercentage = StatCalculator.getPercentage(stat, possibleValue);

                if (weight < 0) {
                    rollPercentage = 100 - rollPercentage;
                }

                double weightedScore = (rollPercentage / 100.0) * Math.abs(weight);
                totalWeightedScore += weightedScore;
                totalPossibleScore += Math.abs(weight);
                hasApplicableStats = true;

                String displayName = statType.getDisplayName();
                if (apiName.toLowerCase().endsWith("raw") || apiName.toLowerCase().startsWith("raw")) {
                    displayName += " Raw";
                }

                contributions.add(new WeightedRollSummary.StatContribution(
                        apiName, displayName, rollPercentage, Math.abs(weight), weightedScore));
            }

            if (hasApplicableStats) {
                double weightedPercentage = (totalWeightedScore / totalPossibleScore) * 100.0;
                scaleLines.add(new WeightedRollSummary.ScaleLine(scaleName, weightedPercentage, source));
            }
        }

        if (!scaleLines.isEmpty()) {
            WeightedRollSummary.storeScaleLines(itemStack, scaleLines);
        }
    }
}