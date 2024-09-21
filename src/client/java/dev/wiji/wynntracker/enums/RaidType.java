package dev.wiji.wynntracker.enums;

public enum RaidType {
	NEST_OF_THE_GROOTSLANGS(0, "Nest of the Grootslangs"),
	NEXUS_OF_LIGHT(1, "Orphion's Nexus of Light"),
	THE_CANYON_COLOSSUS(2, "The Canyon Colossus"),
	THE_NAMELESS_ANOMALY(3, "The Nameless Anomaly"),
	;

	public final int id;
	public final String name;

	RaidType(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public static RaidType getRaidType(String name) {
		for (RaidType raidType : values()) {
			if (raidType.name.equals(name)) return raidType;
		}
		return null;
	}
}
