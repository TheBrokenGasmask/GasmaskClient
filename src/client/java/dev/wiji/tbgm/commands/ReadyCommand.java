package dev.wiji.tbgm.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.wiji.tbgm.raid.RaidTracker;
import dev.wiji.tbgm.objects.AbstractClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import dev.wiji.tbgm.misc.Misc;
import net.minecraft.client.MinecraftClient;

public class ReadyCommand extends AbstractClientCommand {

    private enum GuiState {
        WAITING_FOR_PARTY_FINDER,
        CLICKING_PARTY_QUEUE,
        LOOKING_FOR_TARGET_RAID,
        COMPLETED,
        FAILED
    }

    private GuiState currentState = GuiState.WAITING_FOR_PARTY_FINDER;
    private String targetRaidName = null;
    private long lastClickTime = 0;
    private int partyQueueClickAttempts = 0;
    private boolean targetRaidFound = false;
    private static final long CLICK_COOLDOWN = 50;
    private static final int MAX_PARTY_QUEUE_ATTEMPTS = 10;
    private static final long STATE_TIMEOUT = 4000;

    public ReadyCommand() {
        super(
                "ready",
                "Instantly ready up for the last completed raid",
                "/ready"
        );
    }

    @Override
    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal(name)
                .executes(this::executeRaidStatus));
    }

    private int executeRaidStatus(CommandContext<FabricClientCommandSource> source) {
        try {
            RaidTracker tracker = RaidTracker.getInstance();

            String lastRaid = tracker.getLastRaid();
            if (lastRaid != null) {
                targetRaidName = lastRaid;

                currentState = GuiState.WAITING_FOR_PARTY_FINDER;
                lastClickTime = 0;
                partyQueueClickAttempts = 0;
                targetRaidFound = false;

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null && client.getNetworkHandler() != null) {
                    client.player.networkHandler.sendChatCommand("partyfinder");
                    startStateMachine();
                }
            } else {
                Misc.sendTbgmSuccessMessage("No raid data available");
            }

        } catch (Exception e) {
            sendErrorMessage(source.getSource(), "Failed to get raid status: " + e.getMessage());
        }

        return 1;
    }

    private void startStateMachine() {
        new Thread(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            long stateStartTime = System.currentTimeMillis();

            while (currentState != GuiState.COMPLETED && currentState != GuiState.FAILED) {
                try {
                    if (System.currentTimeMillis() - stateStartTime > STATE_TIMEOUT) {
                        currentState = GuiState.FAILED;
                        break;
                    }

                    if (client.currentScreen instanceof GenericContainerScreen containerScreen) {
                        boolean stateChanged = processCurrentState(containerScreen);
                        if (stateChanged) {
                            stateStartTime = System.currentTimeMillis();
                        }
                    } else {
                        if (targetRaidFound) {
                            currentState = GuiState.COMPLETED;
                            break;
                        }
                    }

                    Thread.sleep(50);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    break;
                }
            }

            client.execute(() -> {
                if (currentState != GuiState.COMPLETED) {
                    if (targetRaidFound) {
                        Misc.sendTbgmErrorMessage("Cannot ready up: missing requirements");
                    } else {
                        Misc.sendTbgmErrorMessage("Failed to ready up for raid");
                    }
                }
            });

        }).start();
    }

    private boolean processCurrentState(GenericContainerScreen containerScreen) {
        MinecraftClient client = MinecraftClient.getInstance();
        switch (currentState) {
            case WAITING_FOR_PARTY_FINDER:
                if (isPartyFinderGui(containerScreen)) {
                    currentState = GuiState.CLICKING_PARTY_QUEUE;
                    return true;
                }
                break;

            case CLICKING_PARTY_QUEUE:
                if (isPartyFinderGui(containerScreen) && canAttemptClick()) {
                    if (attemptClickPartyQueue(containerScreen)) {
                        currentState = GuiState.LOOKING_FOR_TARGET_RAID;
                        partyQueueClickAttempts = 0;
                        return true;
                    } else {
                        partyQueueClickAttempts++;
                        if (partyQueueClickAttempts >= MAX_PARTY_QUEUE_ATTEMPTS) {
                            currentState = GuiState.FAILED;
                            return true;
                        }
                    }
                } else if (!isPartyFinderGui(containerScreen)) {
                    currentState = GuiState.LOOKING_FOR_TARGET_RAID;
                    return true;
                }
                break;

            case LOOKING_FOR_TARGET_RAID:
                if (hasTargetRaid(containerScreen)) {
                    if (canAttemptClick() && attemptClickTargetRaid(containerScreen)) {
                        targetRaidFound = true;
                        return false;
                    }
                }

                if (targetRaidFound && hasTargetCharacters(containerScreen)) {
                    String buttonResult = checkAndClickReadyButton(containerScreen);
                    if (buttonResult.equals("READY_CLICKED")) {
                        client.execute(() -> {
                            if (client.currentScreen != null) {
                                client.currentScreen.close();
                            }
                        });
                        currentState = GuiState.COMPLETED;
                        return true;
                    } else if (buttonResult.equals("MISSING_REQUIREMENTS")) {
                        client.execute(() -> {
                            if (client.currentScreen != null) {
                                client.currentScreen.close();
                            }
                        });
                        currentState = GuiState.FAILED;
                        return true;
                    } else if (buttonResult.equals("CLICKING")) {
                        return false;
                    }
                }
                break;
        }
        return false;
    }

    private boolean canAttemptClick() {
        long now = System.currentTimeMillis();
        return (now - lastClickTime) >= CLICK_COOLDOWN;
    }

    private boolean attemptClickPartyQueue(GenericContainerScreen containerScreen) {
        return attemptClick(containerScreen, "Party Queue");
    }

    private boolean attemptClickTargetRaid(GenericContainerScreen containerScreen) {
        return attemptClick(containerScreen, targetRaidName);
    }

    private String checkAndClickReadyButton(GenericContainerScreen containerScreen) {
        if (!canAttemptClick()) {
            return "COOLDOWN";
        }

        for (Slot slot : containerScreen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                String cleanName = stripFormatting(stack.getName().getString());

                if (cleanName.contains("Waiting for Others") || cleanName.contains("Looking for Players")) {
                    return "READY_CLICKED";
                }
            }
        }

        for (Slot slot : containerScreen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                String cleanName = stripFormatting(stack.getName().getString());

                if (cleanName.contains("Missing Requirements")) {
                    return "MISSING_REQUIREMENTS";
                }
            }
        }

        lastClickTime = System.currentTimeMillis();
        MinecraftClient client = MinecraftClient.getInstance();

        for (Slot slot : containerScreen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                String cleanName = stripFormatting(stack.getName().getString());

                if (cleanName.trim().isEmpty()) {
                    continue;
                }

                if (cleanName.contains("Ready Up!")) {
                    client.execute(() -> {
                        ItemStack currentStack = slot.getStack();
                        if (!currentStack.isEmpty()) {
                            client.interactionManager.clickSlot(
                                    containerScreen.getScreenHandler().syncId,
                                    slot.id,
                                    0, // Left click
                                    net.minecraft.screen.slot.SlotActionType.PICKUP,
                                    client.player
                            );
                        }
                    });
                    return "CLICKING";
                }
            }
        }

        return "NOT_FOUND";
    }

    private boolean attemptClick(GenericContainerScreen containerScreen, String targetName) {
        if (!canAttemptClick()) {
            return false;
        }

        lastClickTime = System.currentTimeMillis();
        MinecraftClient client = MinecraftClient.getInstance();

        for (Slot slot : containerScreen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                String cleanName = stripFormatting(stack.getName().getString());

                if (cleanName.trim().isEmpty()) {
                    continue;
                }

                if (cleanName.toLowerCase().contains(targetName.toLowerCase()) ||
                        targetName.toLowerCase().contains(cleanName.toLowerCase())) {

                    client.execute(() -> {
                        ItemStack currentStack = slot.getStack();
                        if (!currentStack.isEmpty()) {
                            client.interactionManager.clickSlot(
                                    containerScreen.getScreenHandler().syncId,
                                    slot.id,
                                    0, // Left click
                                    net.minecraft.screen.slot.SlotActionType.PICKUP,
                                    client.player
                            );
                        }
                    });
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isPartyFinderGui(GenericContainerScreen containerScreen) {
        try {
            String title = stripFormatting(containerScreen.getTitle().getString());
            if (title.contains("Party Finder")) {
                return true;
            }

            for (Slot slot : containerScreen.getScreenHandler().slots) {
                ItemStack stack = slot.getStack();
                if (!stack.isEmpty()) {
                    String itemName = stripFormatting(stack.getName().getString());
                    if (itemName.contains("Party Queue") || itemName.contains("Create Party")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private boolean hasTargetRaid(GenericContainerScreen containerScreen) {
        if (targetRaidName == null) return false;

        for (Slot slot : containerScreen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                String itemName = stripFormatting(stack.getName().getString());
                if (itemName.toLowerCase().contains(targetRaidName.toLowerCase()) ||
                        targetRaidName.toLowerCase().contains(itemName.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasTargetCharacters(GenericContainerScreen containerScreen) {
        try {
            String title = stripFormatting(containerScreen.getTitle().getString());

            if (title.contains("\uE00C") || title.contains("\uE00B")) {
                return true;
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    private String stripFormatting(String text) {
        return text.replaceAll("(?i)[Ã‚Â§&][0-9a-fk-or]", "").trim();
    }
}