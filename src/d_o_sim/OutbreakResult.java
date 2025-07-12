package d_o_sim;

public record OutbreakResult(
		int peakHospitalBedUsage,
		boolean capacityExceeded,
		int totalDeceased,
		int daysSimulated) {}