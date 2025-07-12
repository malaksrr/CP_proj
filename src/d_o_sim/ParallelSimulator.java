package d_o_sim;

import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.Random;

public class ParallelSimulator {
    private final SimulationParams params;
    private final long baseSeed;
    private final int numThreads;

    public ParallelSimulator(SimulationParams params, long baseSeed, int numThreads) {
        this.params = params;
        this.baseSeed = baseSeed;
        this.numThreads = numThreads;
    }

    public void simulate(int totalSimulations, LongAdder totalDeceased, LongAdder peakBeds, LongAdder capacityExceeded) 
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int simulationsPerThread = totalSimulations / numThreads;

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            int start = threadIndex * simulationsPerThread;
            int end = (threadIndex == numThreads - 1) ? totalSimulations : start + simulationsPerThread;

            executor.submit(() -> {
                Random localRandom = new Random(baseSeed + threadIndex);
                SequentialSimulator simulator = new SequentialSimulator(params, localRandom);

                for (int j = start; j < end; j++) {
                    OutbreakResult result = simulator.simulate();
                    totalDeceased.add(result.totalDeceased());
                    peakBeds.add(result.peakHospitalBedUsage());

                    if (result.capacityExceeded()) {
                        capacityExceeded.increment();
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
    }
}