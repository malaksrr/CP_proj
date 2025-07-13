package d_o_sim;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;

public class OutbreakSimulator {
    public static void main(String[] args) {
        SimulationParams params = new SimulationParams(
            100, 100000, 10, 2.5, 0.1, 0.1, 0.01, 1000
        );
        int totalSimulations = 10_000_000;
        int[] threadCounts = {1, 2, 4, 8, 16};
        long baseSeed = 42L;
        List<String> csvLines = new ArrayList<>();
        csvLines.add("Threads,Time(ms),Speedup,AvgDeceased,AvgPeakBeds");

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

            String seqLine = String.format("(Sequential),%d,%.2f,%d,%d",
                seqTime, 1.00, avgDeceased, avgPeakBeds);

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
                

                String line = String.format("%d,%d,%.2f,%d,%d",
                    threads, parTime, speedup,
                    avgDeceased, avgPeakBeds);

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