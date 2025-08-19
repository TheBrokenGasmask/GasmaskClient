package dev.wiji.tbgm.objects;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public interface ClientCommand {
	void register(CommandDispatcher<FabricClientCommandSource> dispatcher);

	String getName();

	String getDescription();

	String getUsage();
}