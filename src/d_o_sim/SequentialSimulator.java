package d_o_sim;
import java.util.Random;

public class SequentialSimulator {
    private final SimulationParams params;
    private final Random random;

    public SequentialSimulator(SimulationParams params, Random random) {
        this.params = params;
        this.random = new Random(random.nextLong()); // Ensure thread-local randomness
    }

    public OutbreakResult simulate() {
        int susceptible = params.populationSize() - params.initialInfected();
        int infected = params.initialInfected();
        int deceased = 0;
        int peakBeds = 0;
        boolean capacityExceeded = false; //Whether the healthcare system was overwhelmed
        int daysRun = 0; //How many days the outbreak lasted

        for (int day = 0; day < params.durationDays() && infected > 0; day++) {
            daysRun++;
            double infectionProb = params.r0() / (double) params.populationSize();
            int newInfections = Math.min(
                (int) (infected * infectionProb * susceptible * random.nextDouble()),
                susceptible
            );
            susceptible -= newInfections;
            infected += newInfections;

            int recovered = (int) (infected * params.recoveryRate() * random.nextDouble());
            int deaths = (int) (infected * params.fatalityRate() * random.nextDouble());

            infected -= (recovered + deaths);
            deceased += deaths;

            int bedsNeeded = (int) (infected * params.hospitalizationRate() * (0.5 + random.nextDouble()));
            peakBeds = Math.max(peakBeds, bedsNeeded);

            if (bedsNeeded > params.totalBeds()) {
                capacityExceeded = true;
            }
        }

        return new OutbreakResult(peakBeds, capacityExceeded, deceased, daysRun);
    }
}