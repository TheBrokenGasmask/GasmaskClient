package dev.wiji.wynntracker.objects;

import java.util.UUID;

public class Aspect {
	public String giver;
	public String receiver;
	public UUID reporter;

	public Aspect(String giver, String receiver, UUID reporter) {
		this.giver = giver;
		this.receiver = receiver;
		this.reporter = reporter;
	}
}
