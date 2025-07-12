package d_o_sim;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicLong;

public class OutbreakSimulator {
    public static void main(String[] args) {
        SimulationParams params = new SimulationParams(
            100, 100000, 10, 2.5, 0.1, 0.1, 0.01, 1000
        );
        int totalSimulations = 10_000_000;
        int[] threadCounts = {1, 2, 4, 8, 16};
        long baseSeed = 42L;
        List<String> csvLines = new ArrayList<>();
        csvLines.add("Threads,Simulations,Time(ms),Speedup,AvgDeceased,AvgPeakBeds,CapacityExceededCount");

        // Run sequential baseline
        long seqTime = 0;
        List<OutbreakResult> seqResults = new ArrayList<>();
        try {
            SequentialSimulator seqSim = new SequentialSimulator(params, new Random(baseSeed));
            long startTime = System.nanoTime();

            for (int i = 0; i < totalSimulations; i++) {
                seqResults.add(seqSim.simulate());
            }

            seqTime = (System.nanoTime() - startTime) / 1_000_000;
            System.out.println("Sequential Time: " + seqTime + " ms");

            // Compute averages
            long totalDeceased = seqResults.stream().mapToLong(OutbreakResult::totalDeceased).sum();
            int avgDeceased = (int)(totalDeceased / totalSimulations);
            int avgPeakBeds = (int)seqResults.stream()
                .mapToInt(OutbreakResult::peakHospitalBedUsage)
                .average()
                .orElse(0);
            long capacityExceededCount = seqResults.stream()
                .filter(OutbreakResult::capacityExceeded)
                .count();

            String seqLine = String.format("1 (Sequential),%d,%d,%.2f,%d,%d,%d",
                totalSimulations, seqTime, 1.00, avgDeceased, avgPeakBeds, capacityExceededCount);

            csvLines.add(seqLine);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Run parallel simulations
        for (int threads : threadCounts) {
            try {
                ParallelSimulator parSim = new ParallelSimulator(params, baseSeed, threads);
                long startTime = System.nanoTime();

                LongAdder totalDeceased = new LongAdder();
                LongAdder totalPeakBeds = new LongAdder();
                LongAdder capacityExceededCount = new LongAdder();
                
                parSim.simulate(totalSimulations, totalDeceased, totalPeakBeds, capacityExceededCount);

                long parTime = (System.nanoTime() - startTime) / 1_000_000;
                double speedup = (double) seqTime / parTime;

                int avgDeceased = (int)(totalDeceased.sum() / totalSimulations);
                int avgPeakBeds = (int)(totalPeakBeds.sum() / totalSimulations);
                long exceededCount = capacityExceededCount.sum();

                String line = String.format("%d,%d,%d,%.2f,%d,%d,%d",
                    threads, totalSimulations, parTime, speedup,
                    avgDeceased, avgPeakBeds, exceededCount);

                csvLines.add(line);

                System.out.printf("Threads: %d | Time: %d ms | Speedup: %.2fx\n",
                    threads, parTime, speedup);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Save to CSV
        try {
            Path path = Paths.get("simulation_results.csv");
            Files.write(path, csvLines);
            System.out.println("Results saved to simulation_results.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}