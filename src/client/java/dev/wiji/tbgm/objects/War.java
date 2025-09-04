package dev.wiji.tbgm.objects;

import java.util.UUID;

public class War {
	public UUID reporter;
	public long timeInWar;
	public double towerEhp;
	public double towerDps;

	public String territory;
	public String ownerGuild;

	public War(UUID reporter, long timeInWar, double towerEhp, double towerDps, String territory, String ownerGuild) {
		this.reporter = reporter;
		this.timeInWar = timeInWar;
		this.towerEhp = towerEhp;
		this.towerDps = towerDps;

		this.territory = territory;
		this.ownerGuild = ownerGuild;
	}
}
