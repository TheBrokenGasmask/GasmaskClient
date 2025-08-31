package dev.wiji.tbgm.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.wiji.tbgm.objects.AbstractClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import dev.wiji.tbgm.misc.Misc;
import net.minecraft.client.MinecraftClient;

public class UnreadyCommand extends AbstractClientCommand {

    private enum GuiState {
        WAITING_FOR_PARTY_FINDER,
        LOOKING_FOR_TARGET_RAID,
        COMPLETED,
        FAILED
    }

    private GuiState currentState = GuiState.LOOKING_FOR_TARGET_RAID;
    private long lastClickTime = 0;
    private boolean targetRaidFound = false;
    private static final long CLICK_COOLDOWN = 50;
    private static final long STATE_TIMEOUT = 4000;

    public UnreadyCommand() {
        super(
                "unready",
                "Unready from your current raid party",
                "/unready"
        );
    }

    @Override
    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal(name)
                .executes(this::executeRaidStatus));
    }

    private int executeRaidStatus(CommandContext<FabricClientCommandSource> source) {
        try {
            currentState = GuiState.WAITING_FOR_PARTY_FINDER;
            lastClickTime = 0;
            targetRaidFound = false;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.getNetworkHandler() != null) {
                client.player.networkHandler.sendChatCommand("party lobby");
                startStateMachine();
            }

        } catch (Exception e) {
            sendErrorMessage(source.getSource(), "Failed to unready raid: " + e.getMessage());
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
                    Misc.sendTbgmErrorMessage("Failed to unready from raid");
                }
            });

        }).start();
    }

    private boolean processCurrentState(GenericContainerScreen containerScreen) {
        MinecraftClient client = MinecraftClient.getInstance();

        switch (currentState) {
            case WAITING_FOR_PARTY_FINDER:
                currentState = GuiState.LOOKING_FOR_TARGET_RAID;
                return true;

            case LOOKING_FOR_TARGET_RAID:
                if (hasTargetCharacters(containerScreen)) {
                    String buttonResult = checkAndClickUnreadyButton(containerScreen);

                    if (buttonResult.equals("UNREADY_CLICKED")) {
                        client.execute(() -> {
                            if (client.currentScreen != null) {
                                client.currentScreen.close();
                            }
                        });
                        currentState = GuiState.COMPLETED;
                        targetRaidFound = true;
                        return true;
                    } else if (buttonResult.equals("ALREADY_UNREADY")) {
                        client.execute(() -> {
                            if (client.currentScreen != null) {
                                client.currentScreen.close();
                            }
                        });
                        currentState = GuiState.COMPLETED;
                        targetRaidFound = true;
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

    private String checkAndClickUnreadyButton(GenericContainerScreen containerScreen) {
        if (!canAttemptClick()) {
            return "COOLDOWN";
        }

        for (Slot slot : containerScreen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                String cleanName = stripFormatting(stack.getName().getString());

                if (cleanName.contains("Ready Up!")) {
                    return "UNREADY_CLICKED";
                }
            }
        }

        lastClickTime = System.currentTimeMillis();
        MinecraftClient client = MinecraftClient.getInstance();

        for (Slot slot : containerScreen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                String cleanName = stripFormatting(stack.getName().getString());

                if (cleanName.contains("Waiting for Others") || cleanName.contains("Looking for Players")) {
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

        return "ALREADY_UNREADY";
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
        return text.replaceAll("(?i)[Ãƒâ€šÃ‚Â§&][0-9a-fk-or]", "").trim();
    }
}