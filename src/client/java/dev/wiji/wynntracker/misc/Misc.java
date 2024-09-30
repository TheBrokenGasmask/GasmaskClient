package dev.wiji.wynntracker.misc;

public class Misc {
	public static String getUnformattedString(final String string) {
		return string.replaceAll("\udaff\udffc\ue006\udaff\udfff\ue002\udaff\udffe",
						"").replaceAll("\udaff\udffc\ue001\udb00\udc06", "")
				.replaceAll("§.", "").replaceAll("&.", "").replaceAll(
						"\\[[0-9:]+]", "").replaceAll("\\s+", " ").trim();
	}

	public static int convertToInt(String input) {
		input = input.replace(",", "");

		double v = Double.parseDouble(input.substring(0, input.length() - 1));
		if (input.endsWith("k") || input.endsWith("K")) {
			return (int) (v * 1000);
		} else if (input.endsWith("m") || input.endsWith("M")) {
			return (int) (v * 1000000);
		} else {
			double value = Double.parseDouble(input);
			return (int) value;
		}
	}

}
