package dev.wiji.tbgm.objects;

import dev.wiji.tbgm.misc.Misc;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public abstract class AbstractClientCommand implements ClientCommand {
    protected final String name;
    protected final String description;
    protected final String usage;
    protected static final String PREFIX = "tbgm";

    public AbstractClientCommand(String name, String description, String usage) {
        this.name = name;
        this.description = description;
        this.usage = usage;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getUsage() {
        return usage;
    }

    protected void sendSuccessMessage(FabricClientCommandSource source, String message) {
        Misc.sendTbgmSuccessMessage(message);
    }

    protected void sendErrorMessage(FabricClientCommandSource source, String message) {
        Misc.sendTbgmErrorMessage(message);
    }

    protected void sendInfoMessage(FabricClientCommandSource source, String message) {
        Misc.sendTbgmMessage(message);
    }
}