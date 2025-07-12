package d_o_sim;
public record SimulationParams(
    int durationDays,  //How many days to simulate
    int populationSize, //Total number of people in the population
    int initialInfected, //Number of infected individuals at the start
    double r0, //Basic reproduction number (average number of people one infected person infects)
    double hospitalizationRate, //Proportion of infected who end up hospitalized
    double recoveryRate, //Chance that an infected person recovers per day
    double fatalityRate, //	Chance that an infected person dies per day
    int totalBeds //Maximum number of hospital beds available
) {}