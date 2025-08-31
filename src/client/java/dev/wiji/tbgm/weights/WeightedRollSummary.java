package dev.wiji.tbgm.weights;

import com.wynntils.utils.wynn.ColorScaleUtils;
import com.wynntils.features.tooltips.ItemStatInfoFeature;
import dev.wiji.tbgm.controllers.Config;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

public class WeightedRollSummary {
    private static final Map<ItemStack, List<ScaleLine>> LINES_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final String NORI_INDICATOR = "\uEff1";
    private static final String WYNNPOOL_INDICATOR = "\uEff2";
    private static boolean NORI_FIRST_STAT = true;
    private static boolean WYNNPOOL_FIRST_STAT = true;
    public record ScaleLine(String label, double percent, String source) {}

    private static boolean isNoriEnabled(){
        return Config.getConfigData().noriMythicWeights;
    }

    private static boolean isWynnpoolEnabled(){
        return Config.getConfigData().wynnpoolMythicWeights;
    }

    public static void storeScaleLines(ItemStack stack, List<ScaleLine> lines) {
        if (stack == null || lines == null || lines.isEmpty()) return;
        LINES_CACHE.put(stack, new ArrayList<>(lines));
    }

    private static boolean shouldIncludeSource(String source) {
        if ("Nori".equals(source)) {
            return isNoriEnabled();
        } else if ("Wynnpool".equals(source)) {
            return isWynnpoolEnabled();
        }
        return true;
    }

    public static List<Text> buildHeaderLines(ItemStack stack) {
        List<ScaleLine> lines = LINES_CACHE.get(stack);
        if (lines == null || lines.isEmpty()) return null;

        List<ScaleLine> enabledLines = new ArrayList<>();
        for (ScaleLine line : lines) {
            if (shouldIncludeSource(line.source())) {
                enabledLines.add(line);
            }
        }

        List<ScaleLine> sortedLines = new ArrayList<>(enabledLines);
        sortedLines.sort((a, b) -> {
            String sourceA = a.source();
            String sourceB = b.source();

            if ("Wynnpool".equals(sourceA) && !"Wynnpool".equals(sourceB)) {
                return -1;
            }
            if (!"Wynnpool".equals(sourceA) && "Wynnpool".equals(sourceB)) {
                return 1;
            }
            if ("Nori".equals(sourceA) && !"Nori".equals(sourceB)) {
                return -1;
            }
            if (!"Nori".equals(sourceA) && "Nori".equals(sourceB)) {
                return 1;
            }

            if (sourceA.equals(sourceB)) {
                String labelA = a.label().toLowerCase();
                String labelB = b.label().toLowerCase();

                if (labelA.equals("main") && !labelB.equals("main")) {
                    return -1;
                }
                if (!labelA.equals("main") && labelB.equals("main")) {
                    return 1;
                }
                return a.label().compareTo(b.label());
            }
            return a.label().compareTo(b.label());
        });

        NORI_FIRST_STAT = true;
        WYNNPOOL_FIRST_STAT = true;

        List<Text> out = new ArrayList<>(sortedLines.size() + 1);

        for (ScaleLine line : sortedLines) {
            Text pct = wynntilsColoredPct(line.percent(), 2);

            Text lineText = Text.empty()
                    .append(getSourceIndicator(line.source()))
                    .append(Text.literal(" "+line.label()).setStyle(Style.EMPTY
                            .withColor(Formatting.GRAY)
                            .withFont(Identifier.of("minecraft", "default"))))
                    .append(pct);

            out.add(lineText);
        }

        return out;
    }

    private static Text getSourceIndicator(String source) {
        if ("Nori".equals(source)) {
            String prefix = NORI_INDICATOR;

            if(NORI_FIRST_STAT) {
                NORI_FIRST_STAT = false;
                return Text.literal(prefix)
                        .setStyle(Style.EMPTY
                                .withColor(Formatting.WHITE)
                                .withFont(Identifier.of("tbgm", "weight")));
            } else {
                prefix = "\uEff3";
                return Text.literal(prefix)
                        .setStyle(Style.EMPTY
                                .withColor(0x1CB7FF)
                                .withFont(Identifier.of("tbgm", "weight")));
            }

        } else if ("Wynnpool".equals(source)) {
            String prefix = WYNNPOOL_INDICATOR;

            if(WYNNPOOL_FIRST_STAT) {
                WYNNPOOL_FIRST_STAT = false;
                return Text.literal(prefix)
                        .setStyle(Style.EMPTY
                                .withColor(Formatting.WHITE)
                                .withFont(Identifier.of("tbgm", "weight")));
            } else {
                prefix = "\uEff3";
                return Text.literal(prefix)
                        .setStyle(Style.EMPTY
                                .withColor(0xFF9900)
                                .withFont(Identifier.of("tbgm", "weight")));
            }
        }

        return Text.literal("");
    }

    private static Text wynntilsColoredPct(double percent, int decimals) {
        NavigableMap<Float, TextColor> map = new ItemStatInfoFeature().getColorMap();

        MutableText comp = ColorScaleUtils.getPercentageTextComponent(map, (float) percent, true, decimals);
        Style style = comp.getStyle();
        TextColor color = style.getColor();
        int rgb = (color == null) ? 0xFFFFFF : color.getRgb();
        String pctStr = String.format(Locale.ROOT, "%." + decimals + "f%%", percent);
        return Text.literal(" [" + pctStr + "]")
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromRgb(rgb))
                        .withItalic(false));
    }

    public static class StatContribution {
        public final String apiName;
        public final String displayName;
        public final double rollPercentage;
        public final double weight;
        public final double contribution;

        public StatContribution(String apiName, String displayName, double rollPercentage,
                                double weight, double contribution) {
            this.apiName = apiName;
            this.displayName = displayName;
            this.rollPercentage = rollPercentage;
            this.weight = weight;
            this.contribution = contribution;
        }
    }
}