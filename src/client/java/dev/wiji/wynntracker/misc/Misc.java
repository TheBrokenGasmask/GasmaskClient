package dev.wiji.wynntracker.misc;

public class Misc {
	public static String getUnformattedString(final String string) {
		return string.replaceAll("\udaff\udffc\ue006\udaff\udfff\ue002\udaff\udffe",
						"").replaceAll("\udaff\udffc\ue001\udb00\udc06", "")
				.replaceAll("§.", "").replaceAll("&.", "").replaceAll(
						"\\[[0-9:]+]", "").replaceAll("\\s+", " ").trim();
	}
}
