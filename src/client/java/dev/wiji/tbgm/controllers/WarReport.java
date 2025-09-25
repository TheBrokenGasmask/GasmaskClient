package dev.wiji.tbgm.controllers;

import com.wynntils.models.war.event.GuildWarEvent;
import com.wynntils.models.war.type.WarTowerState;
import dev.wiji.tbgm.objects.War;
import net.minecraft.client.MinecraftClient;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.UUID;

public class WarReport {

	@SubscribeEvent
	public void onWarEnd(GuildWarEvent.Ended event) {
		WarTowerState initialState = event.getWarBattleInfo().getInitialState();
		WarTowerState currentState = event.getWarBattleInfo().getCurrentState();

		// War lost
		if (currentState.health() > 0) return;

		long timeInWar = event.getWarBattleInfo().getTotalLengthSeconds();
		double towerEhp = initialState.effectiveHealth();
		double towerDps = event.getWarBattleInfo().getTowerDps().low();
		String territory = event.getWarBattleInfo().getTerritory();
		String ownerGuild = event.getWarBattleInfo().getOwnerGuild();

		UUID reporterID = MinecraftClient.getInstance().getGameProfile().getId();
		War war = new War(reporterID, timeInWar, towerEhp, towerDps, territory, ownerGuild);

		Authentication.getWebSocketManager().sendWarReport(war);
	}
}
