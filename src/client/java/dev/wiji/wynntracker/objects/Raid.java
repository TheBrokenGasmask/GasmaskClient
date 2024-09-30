package dev.wiji.wynntracker.objects;

import dev.wiji.wynntracker.enums.RaidType;

import java.util.UUID;

public class Raid {
	public RaidType raidType;
	public String[] players;
	public UUID reporter;
	public int seasonRating;
	public int guildXP;

	public Raid(RaidType raidType, String[] players, UUID reporter, int seasonRating, int guildXP) {
		this.raidType = raidType;
		this.players = players;
		this.reporter = reporter;
		this.seasonRating = seasonRating;
		this.guildXP = guildXP;

		if(players.length != 4) throw new RuntimeException("Invalid number of players in raid! Expected 4, got " + players.length);
	}
}
