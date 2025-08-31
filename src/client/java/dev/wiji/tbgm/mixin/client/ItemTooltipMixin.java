package dev.wiji.tbgm.mixin.client;

import com.wynntils.handlers.tooltip.impl.identifiable.IdentifiableTooltipBuilder;
import com.wynntils.models.items.properties.IdentifiableItemProperty;
import dev.wiji.tbgm.weights.WeightedRollProcessor;
import dev.wiji.tbgm.weights.WeightedRollSummary;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = IdentifiableTooltipBuilder.class, remap = false)
public class ItemTooltipMixin {

    @Inject(method = "fromParsedItemStack", at = @At("HEAD"), remap = false)
    private static void onFromParsedItemStack(ItemStack itemStack, IdentifiableItemProperty itemInfo, CallbackInfoReturnable<IdentifiableTooltipBuilder> cir) {
        if (itemInfo != null && isNonCraftedIdentifiable(itemInfo)) {
            try {
                WeightedRollProcessor.processWeightedRolls(itemStack, itemInfo);
            } catch (Exception e) {
                System.out.println("Error processing weights: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "fromParsedItemStack", at = @At("RETURN"), remap = false)
    private static void modifyTooltipAfterCreation(ItemStack itemStack, IdentifiableItemProperty itemInfo, CallbackInfoReturnable<IdentifiableTooltipBuilder> cir) {
        if (itemInfo != null && isNonCraftedIdentifiable(itemInfo)) {
            try {
                IdentifiableTooltipBuilder builder = cir.getReturnValue();
                List<Text> weightLines = WeightedRollSummary.buildHeaderLines(itemStack);

                if (weightLines != null && !weightLines.isEmpty()) {
                    try {
                        java.lang.reflect.Field headerField = builder.getClass().getSuperclass().getDeclaredField("header");
                        headerField.setAccessible(true);

                        @SuppressWarnings("unchecked")
                        List<Text> currentHeader = (List<Text>) headerField.get(builder);
                        List<Text> modifiedHeader = new ArrayList<>(currentHeader);

                        int insertPosition = findInsertPosition(modifiedHeader);

                        if (insertPosition > 0 && insertPosition < modifiedHeader.size()) {
                            String prevLineText = modifiedHeader.get(insertPosition - 1).getString();
                            if (!prevLineText.trim().isEmpty()) {
                                modifiedHeader.add(insertPosition, Text.literal(""));
                                insertPosition++;
                            }
                        }

                        for (int i = 0; i < weightLines.size(); i++) {
                            modifiedHeader.add(insertPosition + i, weightLines.get(i));
                        }

                        int afterWeightPos = insertPosition + weightLines.size();
                        if (afterWeightPos < modifiedHeader.size()) {
                            String nextLineText = modifiedHeader.get(afterWeightPos).getString();
                            if (!nextLineText.trim().isEmpty()) {
                                modifiedHeader.add(afterWeightPos, Text.literal(""));
                            }
                        }

                        headerField.set(builder, modifiedHeader);

                    } catch (Exception e) {
                        System.out.println("Error modifying tooltip header: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error in tooltip modification: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static int findInsertPosition(List<Text> tooltipLines) {
        for (int i = 0; i < tooltipLines.size(); i++) {
            String lineText = tooltipLines.get(i).getString();
            if (lineText.toLowerCase().contains("attack speed")) {
                return i + 1;
            }
        }

        for (int i = 1; i < tooltipLines.size(); i++) {
            String lineText = tooltipLines.get(i).getString().trim();

            if (lineText.isEmpty()) {
                continue;
            }

            if (lineText.contains("Damage") ||
                    lineText.contains("Defense") ||
                    lineText.contains("Health") ||
                    lineText.contains("Mana") ||
                    lineText.contains("Req") ||
                    lineText.contains("Class") ||
                    lineText.contains("Lv. Min") ||
                    lineText.contains("✔") ||
                    lineText.contains("✖")) {

                System.out.println("No Attack Speed found, inserting at position " + i + " before: " + lineText);
                return i;
            }
        }

        System.out.println("No Attack Speed found, inserting at fallback position 1");
        return 1;
    }

    private static boolean isNonCraftedIdentifiable(IdentifiableItemProperty<?, ?> itemInfo) {
        String className = itemInfo.getClass().getSimpleName();
        return (className.contains("GearItem") ||
                className.contains("CharmItem") ||
                className.contains("TomeItem")) &&
                !className.contains("Crafted");
    }
}